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
@file:JvmName("DataBindingLangUtil")

package com.android.tools.idea.lang.databinding

import com.android.tools.idea.databinding.index.BindingXmlIndex
import com.android.tools.idea.databinding.util.DataBindingUtil
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.facet.AndroidFacet

const val JAVA_LANG = "java.lang."

/**
 * Given a [PsiElement], return an associated entry in the [BindingXmlIndex] for it.
 * This will return null if the IDEA module of the [PsiElement] cannot be found,
 * doesn't have an Android facet attached to it, or databinding is not enabled on it.
 */
fun getBindingIndexEntry(element: PsiElement): BindingXmlIndex.Entry? {
  val module = ModuleUtilCore.findModuleForPsiElement(element)
  if (module != null) {
    val facet = AndroidFacet.getInstance(module)
    if (facet != null && DataBindingUtil.isDataBindingEnabled(facet)) {
      val topLevelFile = InjectedLanguageManager.getInstance(module.project).getTopLevelFile(element)
      if (topLevelFile != null) {
        val name = StringUtil.trimExtensions(topLevelFile.name)
        return BindingXmlIndex.getEntriesForLayout(module, name).firstOrNull()
      }
    }
  }
  return null
}

/**
 * Return a search scope that includes this module's R resources, or null if that scope is not found given
 * the calling [PsiElement]
 *
 * This method should only be called within a data binding expression,
 * as its containing layout file is guaranteed to be two folders below
 * its parent 'res/' folder.
 */
val PsiElement.resolveScopeWithResources: GlobalSearchScope?
  get() = containingFile.context?.containingFile?.containingDirectory?.resolveScope