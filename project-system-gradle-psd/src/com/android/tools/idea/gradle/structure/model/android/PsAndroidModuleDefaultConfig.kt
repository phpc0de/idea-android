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
package com.android.tools.idea.gradle.structure.model.android

import com.android.tools.idea.gradle.structure.model.PsChildModel
import com.android.tools.idea.gradle.structure.model.PsModel
import com.android.tools.idea.gradle.structure.model.meta.getValue

class PsAndroidModuleDefaultConfig(val module: PsAndroidModule) : PsChildModel(){
  override val descriptor by PsAndroidModuleDefaultConfigDescriptors
  override val parent: PsModel = module
  override val name: String = "default"
  override val isDeclared: Boolean = module.isDeclared
  var applicationId by PsAndroidModuleDefaultConfigDescriptors.applicationId
  var applicationIdSuffix by PsAndroidModuleDefaultConfigDescriptors.applicationIdSuffix
  var maxSdkVersion by PsAndroidModuleDefaultConfigDescriptors.maxSdkVersion
  var minSdkVersion by PsAndroidModuleDefaultConfigDescriptors.minSdkVersion
  var multiDexEnabled by PsAndroidModuleDefaultConfigDescriptors.multiDexEnabled
  var targetSdkVersion by PsAndroidModuleDefaultConfigDescriptors.targetSdkVersion
  var testApplicationId by PsAndroidModuleDefaultConfigDescriptors.testApplicationId
  var testFunctionalTest by PsAndroidModuleDefaultConfigDescriptors.testFunctionalTest
  var testHandleProfiling by PsAndroidModuleDefaultConfigDescriptors.testHandleProfiling
  var testInstrumentationRunner by PsAndroidModuleDefaultConfigDescriptors.testInstrumentationRunner
  var versionCode by PsAndroidModuleDefaultConfigDescriptors.versionCode
  var versionName by PsAndroidModuleDefaultConfigDescriptors.versionName
  var versionNameSuffix by PsAndroidModuleDefaultConfigDescriptors.versionNameSuffix
  var proGuardFiles by PsAndroidModuleDefaultConfigDescriptors.proGuardFiles
  var resConfigs by PsAndroidModuleDefaultConfigDescriptors.resConfigs
  var manifestPlaceholders by PsAndroidModuleDefaultConfigDescriptors.manifestPlaceholders
  var testInstrumentationRunnerArguments by PsAndroidModuleDefaultConfigDescriptors.testInstrumentationRunnerArguments
}