/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.lang.androidSql.room

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.impl.ResolveScopeManager
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Extends a use scope for PsiField/KtProperty that defines a Room column.
 *
 * Returns a use scope of containing file of the given PsiField/KtProperty.
 *
 * We need to extend useScope [com.intellij.psi.search.PsiSearchHelper.getUseScope] for a private PsiField/KtProperty because they
 * can be used in a Room query in any file of the module (default useScope doesn't include all files in the module for
 * the private/protected fields)
 */
class RoomUseScopeEnlarger : UseScopeEnlarger() {
  override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
    if (RoomDependencyChecker.getInstance(element.project).isRoomPresent() &&
        (element is PsiField || element is KtProperty) &&
        element.definesRoomSchema &&
        element.containingFile != null) {
      return ResolveScopeManager.getElementUseScope(element.containingFile)
    }
    return null
  }
}