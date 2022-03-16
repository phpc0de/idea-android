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

// ATTENTION: This file has been automatically generated from proguardR8.bnf. Do not edit it manually.

package com.android.tools.idea.lang.proguardR8.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiClass;

public interface ProguardR8ClassSpecificationHeader extends PsiElement {

  @NotNull
  List<ProguardR8AnnotationName> getAnnotationNameList();

  @NotNull
  List<ProguardR8ClassModifier> getClassModifierList();

  @NotNull
  List<ProguardR8ClassName> getClassNameList();

  @NotNull
  ProguardR8ClassType getClassType();

  @NotNull
  List<ProguardR8SuperClassName> getSuperClassNameList();

  @NotNull
  List<PsiClass> resolvePsiClasses();

  @NotNull
  List<PsiClass> resolveSuperPsiClasses();

}
