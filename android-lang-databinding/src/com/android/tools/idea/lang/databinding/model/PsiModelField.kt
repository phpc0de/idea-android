/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.lang.databinding.model

import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier

/**
 * PSI wrapper around psi fields that additionally expose information particularly useful in data binding expressions.
 *
 * Note: This class is adapted from [android.databinding.tool.reflection.ModelField] from db-compiler.
 */
class PsiModelField(override val containingClass: PsiModelClass, val psiField: PsiField) : PsiModelMember{

  val name: String
    get() = psiField.name

  val isPublic = psiField.hasModifierProperty(PsiModifier.PUBLIC)

  override val isStatic = psiField.hasModifierProperty(PsiModifier.STATIC)

  val fieldType = PsiModelClass(containingClass.substitutor.substitute(psiField.type), containingClass.mode)
}
