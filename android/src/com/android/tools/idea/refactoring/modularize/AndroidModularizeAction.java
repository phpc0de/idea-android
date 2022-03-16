/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.refactoring.modularize;

import com.android.tools.idea.projectsystem.ProjectSystemUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BaseJavaRefactoringAction;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidModularizeAction extends BaseJavaRefactoringAction {

  @Override
  protected boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  protected boolean isAvailableForFile(PsiFile file) {
    return file != null && file.getFileType() == JavaFileType.INSTANCE && AndroidUtils.hasAndroidFacets(file.getProject());
  }

  @Override
  protected boolean isEnabledOnDataContext(@NotNull DataContext dataContext) {
    // Hide action if last Gradle sync was unsuccessful.
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project != null && !ProjectSystemUtil.getSyncManager(project).getLastSyncResult().isSuccessful()) {
      return false;
    }

    for (PsiElement element : getPsiElementArray(dataContext)) {
      if (!isAvailableForFile(element.getContainingFile())) {
        return false;
      }
    }

    PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    return file == null || isAvailableForFile(file);
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element,
                                                        @NotNull Editor editor,
                                                        @NotNull PsiFile file,
                                                        @NotNull DataContext context) {
    Project project = file.getProject();
    return ProjectSystemUtil.getSyncManager(project).getLastSyncResult().isSuccessful();
  }

  @Override
  protected boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
    return false;
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
    return new AndroidModularizeHandler();
  }
}
