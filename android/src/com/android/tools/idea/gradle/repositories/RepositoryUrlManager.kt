/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.repositories

import com.android.ide.common.repository.*
import com.android.ide.common.repository.GradleCoordinate.ArtifactType
import com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_LOWER
import com.android.io.CancellableFileIo
import com.android.repository.io.FileOp
import com.android.repository.io.FileOpUtils
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.tools.idea.gradle.util.EmbeddedDistributionPaths
import com.android.tools.idea.gradle.util.GradleLocalCache
import com.android.tools.idea.gradle.util.ImportUtil
import com.android.tools.idea.lint.common.LintIdeSupport
import com.android.tools.idea.projectsystem.GoogleMavenArtifactId
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.sdk.progress.StudioLoggerProgressIndicator
import com.android.tools.lint.checks.GradleDetector.Companion.getLatestVersionFromRemoteRepo
import com.android.tools.lint.client.api.LintClient
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.common.collect.Multimap
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

/**
 * Helper class to aid in generating Maven URLs for various internal repository files (Support Library, AppCompat, etc).
 */
class RepositoryUrlManager @NonInjectable @VisibleForTesting constructor(
  private val googleMavenRepository: GoogleMavenRepository,
  private val cachedGoogleMavenRepository: GoogleMavenRepository,
  private val forceRepositoryChecksInTests: Boolean,
  private val useEmbeddedStudioRepo: Boolean = true) {
  private val pendingNetworkRequests: MutableSet<String> = ConcurrentHashMap.newKeySet()

  internal constructor() : this(IdeGoogleMavenRepository,
                                OfflineIdeGoogleMavenRepository, false)

  fun getArtifactStringCoordinate(artifactId: GoogleMavenArtifactId, preview: Boolean): String? =
    getArtifactStringCoordinate(artifactId, null, preview)

  fun getArtifactStringCoordinate(artifactId: GoogleMavenArtifactId, filter: Predicate<GradleVersion>?, preview: Boolean): String? {
    val revision = getLibraryRevision(
      artifactId.mavenGroupId, artifactId.mavenArtifactId, filter, preview, FileOpUtils.create()
    ) ?: return null
    return artifactId.getCoordinate(revision).toString()
  }

  /**
   * A helper function which wraps [findVersion]?.toString().
   */
  fun getLibraryRevision(
    groupId: String, artifactId: String, filter: Predicate<GradleVersion>?, includePreviews: Boolean, fileOp: FileOp
  ): String? = findVersion(groupId, artifactId, filter, includePreviews, fileOp)?.toString()

  /**
   * Returns the string for the specific version number of the most recent version of the given library
   * (matching the given prefix filter, if any)
   *
   * @param groupId         the group id
   * @param artifactId      the artifact id
   * @param filter          the optional filter constraining acceptable versions
   * @param includePreviews whether to include preview versions of libraries
   */
  fun findVersion(
    groupId: String, artifactId: String, filter: Predicate<GradleVersion>?, includePreviews: Boolean, fileOp: FileOp
  ): GradleVersion? {
    val version: GradleVersion?
    // First check the Google maven repository, which has most versions.
    if (ApplicationManager.getApplication().isDispatchThread) {
      version = cachedGoogleMavenRepository.findVersion(groupId, artifactId, filter, includePreviews)
      refreshCacheInBackground(groupId, artifactId)
    }
    else {
      version = googleMavenRepository.findVersion(groupId, artifactId, filter, includePreviews)
    }
    if (version != null) {
      return version
    }

    if (useEmbeddedStudioRepo) {
      // Try the repo embedded in AS.
      return EmbeddedDistributionPaths.getInstance().findAndroidStudioLocalMavenRepoPaths()
        .filter { it?.isDirectory == true }
        .firstNotNullResult {
          MavenRepositories.getHighestInstalledVersion(groupId, artifactId, fileOp.toPath(it), filter, includePreviews)
        }?.version
    }
    return null
  }

  fun findCompileDependencies(groupId: String,
                              artifactId: String,
                              version: GradleVersion): List<GradleCoordinate> {
    // First check the Google maven repository, which has most versions.
    val result: List<GradleCoordinate>
    if (ApplicationManager.getApplication().isDispatchThread) {
      result = cachedGoogleMavenRepository.findCompileDependencies(groupId, artifactId, version)
      refreshCacheInBackground(groupId, artifactId)
    }
    else {
      result = googleMavenRepository.findCompileDependencies(groupId, artifactId, version)
    }
    return result
  }

  /**
   * Gets the file on the local filesystem that corresponds to the given maven coordinate.
   *
   * @param gradleCoordinate the coordinate to retrieve an archive file for
   * @param sdkLocation      SDK to use
   * @param fileOp           [FileOp] used for file operations
   * @return a file pointing at the archive for the given coordinate or null if no SDK is configured
   */
  fun getArchiveForCoordinate(gradleCoordinate: GradleCoordinate, sdkLocation: File, fileOp: FileOp): File? {
    val groupId = gradleCoordinate.groupId
    val artifactId = gradleCoordinate.artifactId
    val sdkPath = fileOp.toPath(sdkLocation)
    val repository = SdkMavenRepository.find(sdkPath, groupId, artifactId) ?: return null
    val repositoryLocation = repository.getRepositoryLocation(sdkPath, true) ?: return null
    val artifactDirectory: Path? = MavenRepositories.getArtifactDirectory(repositoryLocation, gradleCoordinate)
    if (!CancellableFileIo.isDirectory(artifactDirectory!!)) {
      return null
    }
    for (artifactType in ImmutableList.of(ArtifactType.JAR, ArtifactType.AAR)) {
      val archive = artifactDirectory.resolve("$artifactId-${gradleCoordinate.revision}.$artifactType")
      if (CancellableFileIo.isRegularFile(archive)) {
        return fileOp.toFile(archive)
      }
    }
    return null
  }

  /**
   * Checks the given Gradle coordinate, and if it contains a dynamic dependency, returns
   * a new Gradle coordinate with the dynamic dependency replaced with a specific version.
   * This tries looking at local caches, to pick the best version that Gradle would use without hitting the network,
   * but (if a [Project] is provided) it can also fall back to querying the network for the latest version.
   * Works not just for a completely generic version (e.g. "+"), but for more specific version filters like 23.+ and 23.1.+ as well.
   *
   * Note that in some cases the method may return null -- such as the case for unknown artifacts not found on disk or on the network,
   * or for valid artifacts but where there is no local cache and the network query is not successful.
   *
   * @param coordinate the coordinate whose version we want to resolve
   * @param project    the current project, if known. This is required if you want to perform a network lookup of
   * the current best version if we can't find a locally cached version of the library
   * @return the resolved coordinate, or null if not successful
   */
  fun resolveDynamicCoordinate(
    coordinate: GradleCoordinate, project: Project?, sdkHandler: AndroidSdkHandler?
  ): GradleCoordinate? {
    val version = resolveDynamicCoordinateVersion( // sdkHandler is nullable for Mockito support, should have default value instead
      coordinate, project, sdkHandler ?: AndroidSdks.getInstance().tryToChooseSdkHandler()) ?: return null
    val revisions = GradleCoordinate.parseRevisionNumber(version)
    if (revisions.isNotEmpty()) {
      return GradleCoordinate(coordinate.groupId, coordinate.artifactId, revisions, coordinate.artifactType)
    }
    return null
  }

  fun resolveDynamicCoordinateVersion(coordinate: GradleCoordinate, project: Project?, sdkHandler: AndroidSdkHandler?): String? {
    @Suppress("NAME_SHADOWING")
    val sdkHandler = sdkHandler ?: AndroidSdks.getInstance().tryToChooseSdkHandler()

    val revision = coordinate.revision
    if (!revision.endsWith(REVISION_ANY)) {
      // Already resolved.
      return revision
    }
    val versionPrefix = revision.substring(0, revision.length - 1)
    val filter = Predicate { version: GradleVersion -> version.toString().startsWith(versionPrefix) }
    val groupId = coordinate.groupId
    val artifactId = coordinate.artifactId
    val bestAvailableGoogleMavenRepo: GoogleMavenRepository

    // First check the Google maven repository, which has most versions.
    if (ApplicationManager.getApplication().isDispatchThread) {
      bestAvailableGoogleMavenRepo = cachedGoogleMavenRepository
      refreshCacheInBackground(groupId, artifactId)
    }
    else {
      bestAvailableGoogleMavenRepo = googleMavenRepository
    }

    val stable = bestAvailableGoogleMavenRepo.findVersion(groupId, artifactId, filter, false)
    if (stable != null) {
      return stable.toString()
    }
    val version = bestAvailableGoogleMavenRepo.findVersion(groupId, artifactId, filter, true)
    if (version != null) {
      // Only had preview version; use that (for example, artifacts that haven't been released as stable yet).
      return version.toString()
    }
    val sdkLocation = sdkHandler.location
    if (sdkLocation != null) {
      // If this coordinate points to an artifact in one of our repositories, mark it with a comment if they don't
      // have that repository available.
      var libraryCoordinate = getLibraryRevision(groupId, artifactId, filter, false, sdkHandler.fileOp)
      if (libraryCoordinate != null) {
        return libraryCoordinate
      }

      // If that didn't yield any matches, try again, this time allowing preview platforms.
      // This is necessary if the artifact prefix includes enough of a version where there are only preview matches.
      libraryCoordinate = getLibraryRevision(groupId, artifactId, filter, true, sdkHandler.fileOp)
      if (libraryCoordinate != null) {
        return libraryCoordinate
      }
    }

    // Regular Gradle dependency? Look in Gradle cache.
    val versionFound = GradleLocalCache.getInstance().findLatestArtifactVersion(coordinate, project, revision)
    if (versionFound != null) {
      return versionFound.toString()
    }

    // Maybe it's available for download as an SDK component.
    val progress = StudioLoggerProgressIndicator(javaClass)
    val sdkPackage = SdkMavenRepository.findLatestRemoteVersion(coordinate, sdkHandler, filter, progress)
    if (sdkPackage != null) {
      val found = SdkMavenRepository.getCoordinateFromSdkPath(sdkPackage.path)
      if (found != null) {
        return found.revision
      }
    }

    // Perform network lookup to resolve current best version, if possible.
    project ?: return null
    val client: LintClient = LintIdeSupport.get().createClient(project)
    val latest = getLatestVersionFromRemoteRepo(client, coordinate, filter, coordinate.isPreview)?.toString() ?: return null
    if (latest.startsWith(versionPrefix)) {
      return latest
    }
    return null
  }

  /**
   * Resolves multiple dynamic dependencies on artifacts distributed in the SDK.
   *
   * Doesn't check any remote repositories, just the already downloaded SDK "extras" repositories.
   */
  fun resolveDynamicSdkDependencies(
    dependencies: Multimap<String, GradleCoordinate>, supportLibVersionFilter: String?, fileOp: FileOp
  ): List<GradleCoordinate> {
    val result = mutableListOf<GradleCoordinate>()
    val supportFilter = findExistingExplicitVersion(dependencies.values())
                        ?: supportLibVersionFilter
    for (key in dependencies.keySet()) {
      var highest: GradleCoordinate = dependencies.get(key).maxWithOrNull(COMPARE_PLUS_LOWER)!!

      // For test consistency, don't depend on installed SDK state while testing
      if (!forceRepositoryChecksInTests && ApplicationManager.getApplication().isUnitTestMode) {
        result.add(highest)
        continue
      }
      // If this coordinate points to an artifact in one of our repositories, check to see if there is a static version
      // that we can add instead of a plus revision.

      var revision: String? = highest.revision
      if (revision!!.endsWith(REVISION_ANY)) {
        revision = if (revision.length > 1)
          revision.dropLast(1)
        else
          supportFilter.takeIf {
            ImportUtil.SUPPORT_GROUP_ID == highest.groupId || ImportUtil.CORE_KTX_GROUP_ID == highest.groupId
          }
        val filter = if (revision != null)
          Predicate { version: GradleVersion -> version.toString().startsWith(revision) }
        else
          null
        var version: String? = null
        // 1 - Latest specific (ie support lib version filter level) stable version
        if (filter != null) {
          version = getLibraryRevision(highest.groupId, highest.artifactId, filter, false, fileOp)
        }
        // 2 - Latest specific (ie support lib version filter level) preview version
        if (version == null && filter != null) {
          version = getLibraryRevision(highest.groupId, highest.artifactId, filter, true, fileOp)
        }
        // 3 - Latest stable version
        if (version == null) {
          version = getLibraryRevision(highest.groupId, highest.artifactId, null, false, fileOp)
        }
        // 4 - Latest preview version
        if (version == null) {
          version = getLibraryRevision(highest.groupId, highest.artifactId, null, true, fileOp)
        }
        // 5 - No version found
        if (version != null) {
          val libraryCoordinate = highest.id + ":" + version
          val available = GradleCoordinate.parseCoordinateString(libraryCoordinate)
          if (available != null && COMPARE_PLUS_LOWER.compare(available, highest) >= 0) {
            highest = available
          }
        }
      }
      result.add(highest)
    }
    return result
  }

  private fun refreshCacheInBackground(groupId: String, artifactId: String) {
    val searchKey = ("$groupId:$artifactId")
    if (pendingNetworkRequests.add(searchKey)) {
      ApplicationManager.getApplication().executeOnPooledThread {
        try {
          // We don't care about the result, just the side effect of updating the cache.
          // This will only make a network request if there is no cache or it has expired.
          googleMavenRepository.findVersion(groupId, artifactId, { true }, true)
        }
        finally {
          pendingNetworkRequests.remove(searchKey)
        }
      }
    }
  }

  companion object {
    @JvmStatic
    fun get(): RepositoryUrlManager = ApplicationManager.getApplication().getService(
      RepositoryUrlManager::class.java)
  }
}

/**
 * Constant full revision for "anything available"
 */
const val REVISION_ANY = "+"

private fun findExistingExplicitVersion(dependencies: Collection<GradleCoordinate>): String? {
  val highest = dependencies
                  .filter { coordinate: GradleCoordinate -> ImportUtil.SUPPORT_GROUP_ID == coordinate.groupId }
                  .maxWithOrNull(COMPARE_PLUS_LOWER) ?: return null
  val version = highest.revision
  return if (version.endsWith(REVISION_ANY))
    if (version.length > 1) version.dropLast(1) else null
  else version
}
