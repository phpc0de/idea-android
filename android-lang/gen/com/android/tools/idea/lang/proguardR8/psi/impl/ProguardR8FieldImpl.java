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

package com.android.tools.idea.lang.proguardR8.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.android.tools.idea.lang.proguardR8.psi.*;

public class ProguardR8FieldImpl extends ASTWrapperPsiElement implements ProguardR8Field {

  public ProguardR8FieldImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ProguardR8Visitor visitor) {
    visitor.visitField(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ProguardR8Visitor) accept((ProguardR8Visitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public ProguardR8AnnotationName getAnnotationName() {
    return findChildByClass(ProguardR8AnnotationName.class);
  }

  @Override
  @NotNull
  public ProguardR8ClassMemberName getClassMemberName() {
    return findNotNullChildByClass(ProguardR8ClassMemberName.class);
  }

  @Override
  @NotNull
  public List<ProguardR8Modifier> getModifierList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ProguardR8Modifier.class);
  }

  @Override
  @Nullable
  public ProguardR8Type getType() {
    return findChildByClass(ProguardR8Type.class);
  }

  @Override
  @Nullable
  public ProguardR8Parameters getParameters() {
    return ProguardR8PsiImplUtil.getParameters(this);
  }

}
