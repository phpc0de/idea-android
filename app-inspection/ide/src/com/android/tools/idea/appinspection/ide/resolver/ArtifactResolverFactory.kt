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
package com.android.tools.idea.appinspection.ide.resolver

import com.android.tools.idea.appinspection.ide.resolver.blaze.BlazeArtifactResolver
import com.android.tools.idea.appinspection.ide.resolver.http.HttpArtifactResolver
import com.android.tools.idea.appinspection.ide.resolver.moduleSystem.ModuleSystemArtifactResolver
import com.android.tools.idea.appinspection.inspector.ide.resolver.ArtifactResolver
import com.android.tools.idea.appinspection.inspector.ide.resolver.ArtifactResolverFactory
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.android.tools.idea.io.FileService
import com.android.tools.idea.project.AndroidProjectInfo
import com.intellij.openapi.project.Project

class ArtifactResolverFactory(
  private val fileService: FileService
) : ArtifactResolverFactory {
  override fun getArtifactResolver(project: Project): ArtifactResolver {
    return if (GradleProjectInfo.getInstance(project).isBuildWithGradle
               || AndroidProjectInfo.getInstance(project).isApkProject) {
      if (StudioFlags.APP_INSPECTION_USE_SNAPSHOT_JAR.get()) {
        ModuleSystemArtifactResolver(project)
      }
      else {
        HttpArtifactResolver(fileService)
      }
    }
    else {
      BlazeArtifactResolver(fileService, project)
    }
  }
}