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
package com.android.tools.idea.projectsystem.gradle

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.util.registry.Registry

class AndroidGradleDisableAutoImportInitializer : Runnable {
  override fun run() {
    Registry.get("external.system.auto.import.disabled").setValue(true)
  }
}

class RefreshOnlyAutoImportProjectTracker : ExternalSystemProjectTracker {
  override fun activate(id: ExternalSystemProjectId) = Unit
  override fun markDirty(id: ExternalSystemProjectId) = Unit
  override fun markDirtyAllProjects() = Unit
  override fun register(projectAware: ExternalSystemProjectAware) = Unit
  override fun register(projectAware: ExternalSystemProjectAware, parentDisposable: Disposable) = Unit
  override fun remove(id: ExternalSystemProjectId) = Unit
  override fun scheduleProjectRefresh() = Unit
  override fun scheduleChangeProcessing() = Unit
}