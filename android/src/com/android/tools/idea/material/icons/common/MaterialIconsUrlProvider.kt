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
package com.android.tools.idea.material.icons.common

import com.android.ide.common.vectordrawable.VdIcon
import com.android.tools.idea.material.icons.utils.MaterialIconsUtils
import com.android.tools.idea.material.icons.utils.MaterialIconsUtils.MATERIAL_ICONS_PATH
import com.android.tools.idea.material.icons.utils.MaterialIconsUtils.toDirFormat
import com.android.utils.SdkUtils.fileToUrl
import java.io.File
import java.net.URL

/**
 * Interface used to get [URL] objects of [MaterialVdIcons].
 */
interface MaterialIconsUrlProvider {

  /**
   * Returns the [URL] of the files under a particular material icon style.
   */
  fun getStyleUrl(style: String): URL?

  /**
   * Returns the [URL] of the actual Material Icon file for the given style, icon name and its file name.
   */
  fun getIconUrl(style: String, iconName: String, iconFileName: String): URL?
}

/**
 * The default [MaterialIconsUrlProvider] for [VdIcon] files bundled with Android Studio.
 */
internal class BundledIconsUrlProvider : MaterialIconsUrlProvider {
  override fun getStyleUrl(style: String): URL? {
    return javaClass.classLoader.getResource(getStyleDirectoryPath(style))
  }

  override fun getIconUrl(style: String, iconName: String, iconFileName: String): URL? {
    return javaClass.classLoader.getResource(getIconDirectoryPath(style, iconName) + iconFileName)
  }

  private fun getIconDirectoryPath(style: String, name: String): String {
    return getStyleDirectoryPath(style) + name + "/"
  }

  private fun getStyleDirectoryPath(style: String): String {
    return MATERIAL_ICONS_PATH + style.toDirFormat() + "/"
  }
}

/**
 * [MaterialIconsUrlProvider] for [VdIcon] files located in the .../Android/Sdk directory.
 *
 * @see MaterialIconsUtils.getIconsSdkTargetPath
 */
internal class SdkMaterialIconsUrlProvider: MaterialIconsUrlProvider {
  private val iconsSdkPath = MaterialIconsUtils.getIconsSdkTargetPath()

  override fun getStyleUrl(style: String): URL? {
    return getStyleDirectoryFile(style)?.let(::fileToUrl)
  }

  override fun getIconUrl(style: String, iconName: String, iconFileName: String): URL? {
    return getIconDirectoryFile(style, iconName)?.resolve(iconFileName)?.let(::fileToUrl)
  }

  private fun getIconDirectoryFile(style: String, name: String): File? {
    return getStyleDirectoryFile(style)?.resolve(name)
  }

  private fun getStyleDirectoryFile(style: String): File? {
    return iconsSdkPath?.resolve(style.toDirFormat())
  }
}