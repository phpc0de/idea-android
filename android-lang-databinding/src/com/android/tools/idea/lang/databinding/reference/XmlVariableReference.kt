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
package com.android.tools.idea.lang.databinding.reference

import com.android.tools.idea.databinding.DataBindingMode
import com.android.tools.idea.databinding.index.BindingXmlData
import com.android.tools.idea.databinding.index.VariableData
import com.android.tools.idea.databinding.util.DataBindingUtil
import com.android.tools.idea.lang.databinding.JAVA_LANG
import com.android.tools.idea.lang.databinding.model.PsiModelClass
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

/**
 * Reference that points to a <variable> tag in a layout XML file.
 */
internal class XmlVariableReference(element: PsiElement,
                                    resolveTo: XmlTag,
                                    private val variable: VariableData,
                                    private val bindingData: BindingXmlData,
                                    private val module: Module)
  : DbExprReference(element, resolveTo) {
  override val resolvedType: PsiModelClass?
    get() {
      return DataBindingUtil.getQualifiedType(element.project, variable.type, bindingData, false)
        ?.let { name -> resolveType(name) }
        ?.let { psiType -> PsiModelClass(psiType, DataBindingMode.fromPsiElement(element)) }
    }

  /**
   * Returns the resolved [PsiClassType] for fully qualified name with type parameters.
   */
  private fun resolveType(name: String): PsiClassType? {
    val index = name.indexOf('<')
    val simpleName = if (index == -1) name else name.substring(0, index).trim()
    val qualifiedName = if (simpleName.contains('.')) simpleName else JAVA_LANG + simpleName

    // Create the string for parameters in the format of "psiType1, psiType2, ... lastPsiType,"
    val parametersString = if (index == -1) "" else name.substring(index + 1, name.lastIndexOf('>')).trim() + ","
    val psiClass =
      JavaPsiFacade.getInstance(element.project).findClass(qualifiedName, module.getModuleSystem().getResolveScope(ScopeType.MAIN))
      ?: return null

    // Parse and resolve type parameters recursively
    // For example: name = "MyClass<Class1<InsideClass1, InsideClass2>, Class2<InsideClass3<InsideClass4>, InsideClass4>"
    //
    // layerCount          0000001111111111111111111111111110000000001111111111111222222222222211111111111111100
    // parametersString    Class1<InsideClass1, InsideClass2>, Class2<InsideClass3<InsideClass4>, InsideClass4>,
    // delimiters                                            *                                                 *
    //
    // "Class1<InsideClass1, InsideClass2>" and "Class2<InsideClass3<InsideClass4>, InsideClass4>" will be resolved recursively and
    // added to parameters
    val parameters = ArrayList<PsiClassType?>()
    val stringBuilder = StringBuilder()
    var layerCount = 0
    parametersString.forEach { c ->
      if (c == ',' && layerCount == 0) {
        parameters.add(resolveType(stringBuilder.trim().toString()))
        stringBuilder.clear()
      }
      else {
        stringBuilder.append(c)
        when (c) {
          '<' -> layerCount += 1
          '>' -> layerCount -= 1
        }
      }
    }

    return JavaPsiFacade.getElementFactory(psiClass.project).createType(psiClass, *(parameters.toTypedArray()))
  }

  override val memberAccess = PsiModelClass.MemberAccess.ALL_MEMBERS
}
