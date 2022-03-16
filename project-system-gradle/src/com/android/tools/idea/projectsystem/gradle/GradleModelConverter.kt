/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.android.ide.common.util.PathString
import com.android.ide.common.util.toPathString
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.projectmodel.RecursiveResourceFolder
import com.android.projectmodel.ResourceFolder
import com.android.tools.idea.gradle.model.IdeAndroidLibrary
import com.android.tools.idea.gradle.model.IdeLibrary

/**
 * Converts a builder-model [IdeAndroidLibrary] into a [ExternalAndroidLibrary]. Returns null
 * if the input is invalid.
 */
fun convertLibraryToExternalLibrary(source: IdeAndroidLibrary): ExternalAndroidLibrary = AndroidLibraryWrapper(source)

private abstract class LibraryWrapper<T: IdeLibrary>(protected val lib: T) : ExternalAndroidLibrary {
  @Suppress("FileComparisons")
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return lib.artifact == (other as? LibraryWrapper<*>)?.lib?.artifact
  }

  override fun hashCode(): Int {
    return lib.artifact.hashCode()
  }
}

private class AndroidLibraryWrapper(source: IdeAndroidLibrary) : LibraryWrapper<IdeAndroidLibrary>(source) {
  override val address: String get() = lib.artifactAddress
  override val location: PathString? get() = lib.artifact.toPathString()
  override val manifestFile: PathString? get() = PathString(lib.manifest)
  override val packageName: String? get() = null
  override val resFolder: ResourceFolder? get() = RecursiveResourceFolder(PathString(lib.resFolder))
  override val symbolFile: PathString? get() = PathString(lib.symbolFile)
  override val resApkFile: PathString? get() = lib.resStaticLibrary?.let(::PathString)

  // NOTE: The intended implementation is resApkFile != null || resFolder != null, but resFolder is currently always not null.
  override val hasResources: Boolean get() = true
}
