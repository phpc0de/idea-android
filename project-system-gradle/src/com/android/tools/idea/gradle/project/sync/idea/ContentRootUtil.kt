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
package com.android.tools.idea.gradle.project.sync.idea

import com.android.tools.idea.gradle.model.IdeAndroidArtifact
import com.android.tools.idea.gradle.model.IdeBaseArtifact
import com.android.tools.idea.gradle.model.IdeSourceProvider
import com.android.tools.idea.gradle.model.IdeVariant
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.gradle.project.sync.idea.data.service.AndroidProjectKeys
import com.android.tools.idea.gradle.util.GradleUtil
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.RESOURCE
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.RESOURCE_GENERATED
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.SOURCE
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.SOURCE_GENERATED
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.TEST
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.TEST_GENERATED
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.TEST_RESOURCE
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType.TEST_RESOURCE_GENERATED
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.annotations.SystemDependent
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * Sets up all of the content entries for a given [DataNode] containing the [ModuleData]. We use the given
 * [variant] to find the information required to set up the [ContentRootData] if the root folder of the module
 * exists as a content root we will attempt to re-use it. Anything outside of this folder will be added in their
 * own [ContentRootData].
 *
 * If no [variant] is provided that this method will operate solely on the information contained within the [DataNode] tree.
 * This method should have no effects outside of manipulating the [DataNode] tree from the [ModuleData] node downwards.
 */
fun DataNode<ModuleData>.setupAndroidContentEntries(variant: IdeVariant?) {
  // 1 - Extract all of the information (models) we need from the nodes
  val androidModel = ExternalSystemApiUtil.find(this, AndroidProjectKeys.ANDROID_MODEL)?.data ?: return
  val selectedVariant = variant ?: androidModel.selectedVariant

  // 2 - Compute all of the content roots that this module requires from the models we obtained above.
  val contentRoots = collectContentRootData(selectedVariant, androidModel)

  // 3 - Add the ContentRootData nodes to the module.
  contentRoots.forEach { contentRootData ->
    this.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
  }
}

/**
 * This is a helper for [setupAndroidContentEntries] this method collects all of the content roots for a given variant.
 *
 * This method will attempt to reuse the root project path from [androidModel] and will create new content entries for
 * anything outside of this path.
 *
 * Any existing [ContentRootData] will not be included in the returned collection, these may be modified.
 * The returned collection contains only new [ContentRootData] that were created inside this method.
 */
private fun collectContentRootData(
  variant: IdeVariant,
  androidModel: AndroidModuleModel
): Collection<ContentRootData> {
  val buildDir: File = androidModel.androidProject.buildFolder
  val moduleRootPath = ExternalSystemApiUtil.toCanonicalPath(androidModel.rootDirPath.absolutePath)

  val newContentRoots = mutableListOf<ContentRootData>()
  val mainContentRootData = ContentRootData(GradleConstants.SYSTEM_ID, moduleRootPath).also {
    newContentRoots.add(it)
  }

  val handleBuildDir = FileUtil.isAncestor(moduleRootPath, buildDir.path, false)
  // Function passed in to the methods below to register each source path with a ContentRootData object.
  fun addSourceFolder(path: @SystemDependent String, sourceType: ExternalSystemSourceType?) {
    if (sourceType != null && handleBuildDir && FileUtil.isAncestor(buildDir.path, path, false)) {
      mainContentRootData.storePath(sourceType, path)
    }
    else {
      val contentRootData = ContentRootData(GradleConstants.SYSTEM_ID, path)
      if (sourceType != null) {
        contentRootData.storePath(sourceType, path)
      }
      newContentRoots.add(contentRootData)
    }
  }

  // Processes all generated sources that are contained directly in the artifacts and are not part of the source providers.
  variant.processGeneratedSources(androidModel, ::addSourceFolder)

  // Process all of the non-test source providers that are currently active for the selected variant.
  androidModel.activeSourceProviders.forEach { sourceProvider ->
    sourceProvider.processAll(false, ::addSourceFolder)
  }
  // Process all of the unit test and Android test source providers for the selected variant.
  (androidModel.unitTestSourceProviders + androidModel.androidTestSourceProviders).forEach { sourceProvider ->
    sourceProvider.processAll(true, ::addSourceFolder)
  }

  return newContentRoots
}

/**
 * Processes all the [SourceProvider]s and sources contained within this [IdeBaseArtifact], these are
 * processed by using the provided [processor], each [SourceProvider] is then passed to [processAll] along
 * with the processor.
 */
private fun IdeVariant.processGeneratedSources(
  androidModel: AndroidModuleModel,
  processor: (String, ExternalSystemSourceType) -> Unit
) {

  fun IdeBaseArtifact.applicableGeneratedSourceFolders(): Collection<File> = GradleUtil.getGeneratedSourceFoldersToUse(this, androidModel)
  fun Collection<File>.processAs(type: ExternalSystemSourceType) = forEach { processor(it.absolutePath, type) }

  // Note: This only works with Gradle plugin versions 1.2 or higher. However we should be fine not supporting
  // this far back.
  mainArtifact.applicableGeneratedSourceFolders().processAs(SOURCE_GENERATED)
  mainArtifact.generatedResourceFolders.processAs(RESOURCE_GENERATED)
  unitTestArtifact?.applicableGeneratedSourceFolders()?.processAs(TEST_GENERATED)
  androidTestArtifact?.applicableGeneratedSourceFolders()?.processAs(TEST_GENERATED)
  androidTestArtifact?.generatedResourceFolders?.processAs(TEST_RESOURCE_GENERATED)
}

/**
 * Processes all sources contained within this [SourceProvider] using the given [processor]. This
 * [processor] is called with the absolute path to the file and the type of the source.
 */
private fun IdeSourceProvider.processAll(
  forTest: Boolean = false,
  processor: (String, ExternalSystemSourceType?) -> Unit
) {
  (resourcesDirectories + resDirectories + assetsDirectories + mlModelsDirectories).forEach {
    processor(it.absolutePath, if (forTest) TEST_RESOURCE else RESOURCE)
  }
  processor(manifestFile.parentFile.absolutePath, null)

  val allSources = aidlDirectories + javaDirectories + kotlinDirectories + renderscriptDirectories + shadersDirectories

  allSources.forEach {
    processor(it.absolutePath, if (forTest) TEST else SOURCE)
  }
}

//****************************************************************************************************************************
/* Below are methods related to the processing of content roots for Android modules when module per source set is being used */
//****************************************************************************************************************************

private typealias ArtifactSelector = (IdeVariant) -> IdeBaseArtifact?
private typealias SourceProviderSelector = (AndroidModuleModel) -> List<IdeSourceProvider>

fun DataNode<ModuleData>.setupAndroidContentEntriesPerSourceSet(
  androidModel: AndroidModuleModel
) {
  val variant = androidModel.selectedVariant
  // Go over all modules GradleSourceSetData nodes and set up the required content roots from the artifacts.
  fun DataNode<GradleSourceSetData>.populateContentEntries(
    artifactSelector: ArtifactSelector,
    sourceProviderSelector: SourceProviderSelector
  ) {
    val contentRoots = collectContentRootDataForArtifact(artifactSelector, sourceProviderSelector, androidModel, variant)

    // Add the ContentRootData nodes to the module.
    contentRoots.forEach { contentRootData ->
      createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
    }
  }

  findSourceSetDataForArtifact(variant.mainArtifact)
    .populateContentEntries(IdeVariant::mainArtifact, AndroidModuleModel::getActiveSourceProviders)
  variant.unitTestArtifact?.also {
    findSourceSetDataForArtifact(it)
      .populateContentEntries(IdeVariant::unitTestArtifact, AndroidModuleModel::getUnitTestSourceProviders)
  }
  variant.androidTestArtifact?.also {
    findSourceSetDataForArtifact(it)
      .populateContentEntries(IdeVariant::androidTestArtifact, AndroidModuleModel::getAndroidTestSourceProviders)
  }
}

private fun collectContentRootDataForArtifact(
  artifactSelector: ArtifactSelector,
  sourceProviderSelector: SourceProviderSelector,
  androidModel: AndroidModuleModel,
  selectedVariant: IdeVariant
) : Collection<ContentRootData> {
  val artifact = artifactSelector(selectedVariant) ?: throw ExternalSystemException("Couldn't find artifact for descriptor")

  val newContentRoots = mutableListOf<ContentRootData>()

  // Function passed in to the methods below to register each source path with a ContentRootData object.
  fun addSourceFolder(path: @SystemDependent String, sourceType: ExternalSystemSourceType?) {
    val contentRootData = ContentRootData(GradleConstants.SYSTEM_ID, path)
    if (sourceType != null) {
      contentRootData.storePath(sourceType, path)
    }
    newContentRoots.add(contentRootData)
  }

  sourceProviderSelector(androidModel).forEach { sourceProvider ->
    sourceProvider.processAll(artifact.isTestArtifact, ::addSourceFolder)
  }

  fun IdeBaseArtifact.applicableGeneratedSourceFolders(): Collection<File> = GradleUtil.getGeneratedSourceFoldersToUse(this, androidModel)
  fun Collection<File>.processAs(type: ExternalSystemSourceType) = forEach { addSourceFolder(it.absolutePath, type) }

  artifact.applicableGeneratedSourceFolders().processAs(if (artifact.isTestArtifact) TEST_GENERATED else SOURCE_GENERATED)
  if (artifact is IdeAndroidArtifact) {
    artifact.generatedResourceFolders.processAs(if (artifact.isTestArtifact) TEST_RESOURCE_GENERATED else RESOURCE_GENERATED)
  }

  return newContentRoots
}

