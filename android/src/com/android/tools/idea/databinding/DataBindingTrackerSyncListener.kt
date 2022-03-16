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
package com.android.tools.idea.databinding

import com.android.tools.idea.databinding.analytics.api.DataBindingTracker
import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.intellij.openapi.project.Project

class DataBindingTrackerSyncListener(project: Project) : GradleSyncListener {
  private val dataBindingTracker = DataBindingTracker.getInstance(project)

  override fun syncSucceeded(project: Project) {
    dataBindingTracker.trackPolledMetaData()
  }

  override fun syncFailed(project: Project, errorMessage: String) {
    dataBindingTracker.trackPolledMetaData()
  }

  override fun syncSkipped(project: Project) {
    dataBindingTracker.trackPolledMetaData()
  }
}

