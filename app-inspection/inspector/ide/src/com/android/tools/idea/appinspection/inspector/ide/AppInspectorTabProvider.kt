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
package com.android.tools.idea.appinspection.inspector.ide

import com.android.tools.idea.appinspection.inspector.api.AppInspectionIdeServices
import com.android.tools.idea.appinspection.inspector.api.AppInspectorMessenger
import com.android.tools.idea.appinspection.inspector.api.process.ProcessDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import javax.swing.Icon

interface AppInspectorTabProvider: Comparable<AppInspectorTabProvider> {
  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName<AppInspectorTabProvider>(
      "com.android.tools.idea.appinspection.inspector.ide.appInspectorTabProvider"
    )
  }

  val inspectorId: String
  val displayName: String
  val icon: Icon? get() = null
  val inspectorLaunchParams: AppInspectorLaunchParams
  fun isApplicable(): Boolean = true

  /**
   * Whether this tab's UI can handle working with disposed inspectors or not.
   *
   * By default, after an inspector is disposed (i.e. the process its inspecting has stopped), its
   * associated tab is closed, as it takes intentional effort to handle this case. After all,
   * trying to interact with a disposed inspector will cause exceptions to get thrown.
   *
   * Children that override this method to return true are explicitly opting into a more complex UI
   * lifecycle (with two states, mutable and immutable, depending on the state of its associated
   * inspector).
   */
  fun supportsOffline(): Boolean = false

  /**
   * Extension point for creating UI that communicates with some target inspector and is shown in
   * the app inspection tool window.
   *
   * @param ideServices Various functions which clients may use to request IDE-specific behaviors
   * @param processDescriptor Information about the process and device that the associated inspector
   *   that will drive this UI is attached to
   * @param messenger A class for communicating to the associated inspector
   */
  fun createTab(
    project: Project,
    ideServices: AppInspectionIdeServices,
    processDescriptor: ProcessDescriptor,
    messenger: AppInspectorMessenger,
    parentDisposable: Disposable
  ): AppInspectorTab

  override fun compareTo(other: AppInspectorTabProvider): Int = this.displayName.compareTo(other.displayName)
}