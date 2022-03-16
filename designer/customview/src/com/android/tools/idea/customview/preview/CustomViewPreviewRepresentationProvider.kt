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
package com.android.tools.idea.customview.preview

import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.common.surface.DesignSurface
import com.android.tools.idea.common.type.DesignerTypeRegistrar
import com.android.tools.idea.uibuilder.editor.multirepresentation.PreviewRepresentationProvider
import com.android.tools.idea.uibuilder.editor.multirepresentation.sourcecode.hasSourceFileExtension
import com.android.tools.idea.uibuilder.type.LayoutEditorFileType
import com.google.wireless.android.sdk.stats.LayoutEditorState
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

/**
 * A [PreviewRepresentationProvider] coupled with [CustomViewPreviewRepresentation].
 */
class CustomViewPreviewRepresentationProvider : PreviewRepresentationProvider {
  private object CustomViewEditorFileType : LayoutEditorFileType() {
    override fun getLayoutEditorStateType() = LayoutEditorState.Type.CUSTOM_VIEWS

    override fun isResourceTypeOf(file: PsiFile) = file.virtualFile is CustomViewLightVirtualFile

    override fun getToolbarActionGroups(surface: DesignSurface) = CustomViewPreviewToolbar(surface)

    override fun getSelectionContextToolbar(surface: DesignSurface, selection: List<NlComponent>): DefaultActionGroup =
      DefaultActionGroup()

    override fun isEditable() = true
  }

  init {
    DesignerTypeRegistrar.register(CustomViewEditorFileType)
  }
  /**
   * Checks if the input [psiFile] contains custom views and therefore can be provided with the [PreviewRepresentation] of them.
   */
  override fun accept(project: Project, psiFile: PsiFile): Boolean {
    val virtualFile = psiFile.virtualFile
    if (!virtualFile.hasSourceFileExtension()) {
      return false
    }

    return PsiManager.getInstance(project).findFile(virtualFile)!!.containsViewSuccessor()
  }

  /**
   * Creates a [CustomViewPreviewRepresentation] for the input [psiFile].
   */
  override fun createRepresentation(psiFile: PsiFile) : CustomViewPreviewRepresentation {
    return CustomViewPreviewRepresentation(psiFile)
  }

  override val displayName = "Custom views"

}
