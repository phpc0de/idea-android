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
package com.android.tools.idea.nav.safeargs.psi.xml

import com.intellij.navigation.NavigationItem
import com.intellij.psi.impl.source.xml.XmlTagImpl
import com.intellij.psi.xml.XmlTag
import javax.swing.Icon

sealed class SafeArgsNavItem(private val xmlTag: XmlTagImpl) : NavigationItem by xmlTag

class SafeArgsXmlTag(
  private val xmlTag: XmlTagImpl,
  private val icon: Icon,
  private val name: String
) : XmlTag by xmlTag, SafeArgsNavItem(xmlTag) {
  fun getOriginal(): XmlTagImpl {
    return xmlTag
  }

  override fun getName(): String {
    return name
  }

  override fun getIcon(flags: Int): Icon {
    return icon
  }

  override fun isPhysical(): Boolean {
    return false
  }
}