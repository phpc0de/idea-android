/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.experimental.codeanalysis.datastructs.value;

import com.android.tools.idea.experimental.codeanalysis.datastructs.PsiCFGClass;
import com.android.tools.idea.experimental.codeanalysis.datastructs.PsiCFGMethod;
import com.intellij.psi.PsiType;

public interface NewExpr extends AnyNewExpr {
  public PsiType getBaseType();

  public PsiCFGClass getBaseClass();

  public void setBaseType(PsiType type);

  public void setBaseClass(PsiCFGClass cfgClass);

  public Value[] getArgs();

  public int getArgCount();

  public Value getArg(int index);

  public boolean containsConstructorInvocation();

  public PsiCFGMethod getConstructorInvocation();

}
