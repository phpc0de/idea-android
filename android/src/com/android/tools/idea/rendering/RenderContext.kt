/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.rendering

import com.android.resources.ResourceFolderType
import com.android.tools.idea.configurations.Configuration
import com.android.tools.idea.model.AndroidModuleInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.android.sdk.AndroidPlatform

class RenderContext(
  val module: Module,
  val configuration: Configuration,
  moduleInfo: AndroidModuleInfo,
  val platform: AndroidPlatform?
) {
  val project: Project
    get() = module.project
  val minSdkVersion = moduleInfo.minSdkVersion
  val targetSdkVersion = moduleInfo.targetSdkVersion
  var folderType: ResourceFolderType? = null
}
