/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.model

import com.android.SdkConstants.GRADLE_PATH_SEPARATOR
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.DependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.FileDependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.FileTreeDependencyModel
import com.android.tools.idea.gradle.dsl.api.dependencies.ModuleDependencyModel
import com.google.common.collect.ArrayListMultimap
import com.intellij.openapi.application.ApplicationManager

class PsParsedDependencies(parsedModel: GradleBuildModel?) {
  // Key: module's Gradle path
  private val parsedModuleDependencies = ArrayListMultimap.create<String, ModuleDependencyModel>()
  private val parsedFileDependencies = ArrayListMultimap.create<String, FileDependencyModel>()
  private val parsedFileTreeDependencies = ArrayListMultimap.create<String, FileTreeDependencyModel>()

  // Key: artifact group ID + ":" + artifact name (e.g. "com.google.guava:guava")
  private val parsedArtifactDependencies = ArrayListMultimap.create<String, ArtifactDependencyModel>()

  init {
    reset(parsedModel)
  }

  fun reset(parsedModel: GradleBuildModel?) {
    parsedArtifactDependencies.clear()
    parsedModuleDependencies.clear()
    parsedFileTreeDependencies.clear()
    parsedFileDependencies.clear()
    if (parsedModel != null) {
      ApplicationManager.getApplication().runReadAction {
        for (parsedDependency in parsedModel.dependencies().all()) {
          when (parsedDependency) {
            is ArtifactDependencyModel -> parsedArtifactDependencies.put(createIdFrom(parsedDependency), parsedDependency)
            is FileDependencyModel -> parsedFileDependencies.put(parsedDependency.file().forceString(), parsedDependency)
            is FileTreeDependencyModel -> parsedFileTreeDependencies.put(parsedDependency.dir().forceString(), parsedDependency)
            is ModuleDependencyModel -> parsedModuleDependencies.put(parsedDependency.path().forceString(), parsedDependency)
          }
        }
      }
    }
  }

  /**
   * Finds library dependencies with matching [group] and [name].
   */
  fun findLibraryDependencies(
    group: String?,
    name: String
  ): List<ArtifactDependencyModel> {
    val id = createIdFrom(group, name)
    val potentialMatches = parsedArtifactDependencies[id] ?: return listOf()
    // Do not return live copy.
    return potentialMatches.toList()
  }

  fun findFileDependencies(
    filePath: String
  ): List<FileDependencyModel> {
    val potentialMatches = parsedFileDependencies[filePath] ?: return listOf()
    // Do not return live copy.
    return potentialMatches.toList()
  }

  fun findFileTreeDependencies(
    filePath: String
  ): List<FileTreeDependencyModel> {
    val potentialMatches = parsedFileTreeDependencies[filePath] ?: return listOf()
    // Do not return live copy.
    return potentialMatches.toList()
  }

  fun findModuleDependency(gradlePath: String, predicate: (ModuleDependencyModel) -> Boolean = { true }): ModuleDependencyModel? =
    parsedModuleDependencies[gradlePath]?.find(predicate)

  fun forEach(block: (DependencyModel) -> Unit) {
    parsedModuleDependencies.values().forEach(block)
    parsedArtifactDependencies.values().forEach(block)
  }

  fun forEachModuleDependency(block: (ModuleDependencyModel) -> Unit) {
    parsedModuleDependencies.values().forEach(block)
  }

  fun forEachLibraryDependency(block: (ArtifactDependencyModel) -> Unit) {
    parsedArtifactDependencies.values().forEach(block)
  }

  fun forEachFileDependency(block: (FileDependencyModel) -> Unit) {
    parsedFileDependencies.values().forEach(block)
  }

  fun forEachFileTreeDependency(block: (FileTreeDependencyModel) -> Unit) {
    parsedFileTreeDependencies.values().forEach(block)
  }

  private fun createIdFrom(group: String?, name: String) =
    joinAsGradlePath(listOf(group, name))

  private fun createIdFrom(dependency: ArtifactDependencyModel) =
    joinAsGradlePath(listOf(dependency.group().toString(), dependency.name().toString()))

  private fun joinAsGradlePath(segments: List<String?>) =
    segments.filterNotNull().joinToString(GRADLE_PATH_SEPARATOR)
}
