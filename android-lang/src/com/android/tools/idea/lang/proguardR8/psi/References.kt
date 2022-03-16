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
package com.android.tools.idea.lang.proguardR8.psi

import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.parentOfType

/**
 *  There is one reference type used for both field and method name, even though we have separate PSI nodes for fields and methods,
 *  because getVariants() will may return methods for PSI that is (user haven't typed parentheses yet) a field.
 */
class ProguardR8ClassMemberNameReference(
  classMemberName: ProguardR8ClassMemberName
) : PsiPolyVariantReferenceBase<ProguardR8ClassMemberName>(classMemberName) {
  private val containingMember = classMemberName.parentOfType<ProguardR8ClassMember>()!!
  private val type = containingMember.type
  private val parameters = containingMember.parameters
  private val accessModifiers = containingMember.modifierList.filter { it.isAccessModifier && !it.isNegated }.map(::toPsiModifier)
  private val negatedAccessModifiers = containingMember.modifierList.filter { it.isAccessModifier && it.isNegated }.map(::toPsiModifier)
  private val modifiers = containingMember.modifierList.filter { !it.isAccessModifier && !it.isNegated }.map(::toPsiModifier)
  private val negatedModifiers = containingMember.modifierList.filter { !it.isAccessModifier && it.isNegated }.map(::toPsiModifier)

  private fun List<String>.overlaps(psiModifierList: PsiModifierList): Boolean {
    return any { psiModifierList.hasModifierProperty(it) }
  }

  /**
   * Checks if [ProguardR8ClassMemberName] of this reference matches access modifiers of the given [PsiModifierListOwner]
   *
   * If there are some access flags, to match there must be an overlap with accessModifiers and no overlap with negatedAccessModifiers
   */
  private fun matchesAccessLevel(psiElement: PsiModifierListOwner): Boolean {
    val psiModifierList = psiElement.modifierList ?: return false
    if (accessModifiers.isNotEmpty() && !accessModifiers.overlaps(psiModifierList)) return false
    if (negatedAccessModifiers.isNotEmpty() && negatedAccessModifiers.overlaps(psiModifierList)) return false
    return true
  }

  private fun matchesModifiers(psiElement: PsiModifierListOwner): Boolean {
    val psiModifierList = psiElement.modifierList ?: return false
    if (modifiers.any { !psiModifierList.hasModifierProperty(it) }) return false
    if (negatedModifiers.any { psiModifierList.hasModifierProperty(it) }) return false
    return true
  }

  private fun getFields(): Collection<PsiField> {
    return containingMember.resolveParentClasses().asSequence()
      .flatMap { it.fields.asSequence() }
      .filter { type == null || type.matchesPsiType(it.type) }
      .filter(::matchesAccessLevel)
      .filter(::matchesModifiers)
      .toList()
  }

  private fun getMethods(): Collection<PsiMethod> {
    return containingMember.resolveParentClasses().asSequence()
      .flatMap { it.methods.asSequence() }
      .filter { it.returnType != null } // if returnType is null it's constructor
      .filter { type == null || type.matchesPsiType(it.returnType!!) } // match return type
      .filter { parameters == null || parameters.matchesPsiParameterList(it.parameterList) } // match parameters
      .filter(::matchesAccessLevel)
      .filter(::matchesModifiers)
      .toList()
  }

  private fun getConstructors(): Collection<PsiMethod> {
    // Constructors don't have return type, but always have parameters.
    if (type != null || parameters == null) return emptyList()

    return containingMember.resolveParentClasses().asSequence()
      .flatMap { it.constructors.asSequence() }
      .filter { parameters.matchesPsiParameterList(it.parameterList) } // match parameters
      .filter(::matchesAccessLevel)
      .filter(::matchesModifiers)
      .toList()
  }

  /**
   * Returns empty array if there is no class member matching the type and parameters corresponding to this [ProguardR8ClassMemberName]
   * otherwise returns array with found class members.
   * It can be single element array or not (case with overloads/different access modifiers/not specified return type/ etc.)
   */
  override fun multiResolve(incompleteCode: Boolean): Array<PsiElementResolveResult> {
    val constructors = if (containingMember.isConstructor()) getConstructors() else emptyList()
    val members: Collection<PsiNamedElement> = if (parameters == null) getFields() else getMethods()
    return (members.filter { it.name == element.text } + constructors).map(::PsiElementResolveResult).toTypedArray()
  }

  override fun getVariants(): Array<LookupElementBuilder> {
    val fields = (if (parameters == null) getFields() else emptyList()).map(JavaLookupElementBuilder::forField)
    val methods = getMethods().map {
      JavaLookupElementBuilder
        .forMethod(it, PsiSubstitutor.EMPTY)
        .withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS)
    }
    return (fields + methods).toTypedArray()
  }
}
