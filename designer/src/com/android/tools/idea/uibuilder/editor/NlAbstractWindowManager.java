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
package com.android.tools.idea.uibuilder.editor;

import com.android.tools.idea.common.editor.DesignerEditorPanel;
import com.android.tools.idea.common.surface.DesignSurface;
import com.intellij.designer.DesignerEditorPanelFacade;
import com.intellij.designer.LightToolWindowManager;
import com.intellij.designer.ToggleEditorModeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NlAbstractWindowManager extends LightToolWindowManager {

  private ToolWindowType myPreviousWindowType;
  private ToolWindowAnchor myPreviousWindowAnchor;
  /** The design surface the tool window is attached to, if any */
  private DesignSurface myDesignSurface;

  public NlAbstractWindowManager(@NotNull Project project) {
    super(project);
  }

  protected void initToolWindow(final @NotNull String id, @NotNull Icon icon) {
    myToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(id, false, getAnchor(), myProject, true);
    myToolWindow.setIcon(icon);
    myToolWindow.setAvailable(false);
    myToolWindow.setAutoHide(false);
    myPreviousWindowType = myToolWindow.getType();
    myPreviousWindowAnchor = getEditorMode();
    myProject.getMessageBus().connect(this).subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      @Override
      public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        final ToolWindow window = toolWindowManager.getToolWindow(id);
        ToolWindowType newWindowType = window.getType();
        ToolWindowAnchor newWindowAnchor = getEditorMode();

        if (newWindowType != myPreviousWindowType || newWindowAnchor != myPreviousWindowAnchor) {
          // TODO: Report the window docking state
          if (myDesignSurface != null) {
            myDesignSurface.getAnalyticsManager().trackUnknownEvent();
          }

          myPreviousWindowType = newWindowType;
          myPreviousWindowAnchor = newWindowAnchor;
        }
      }
    });
    initGearActions();
  }

  @Override
  protected void updateToolWindow(@Nullable DesignerEditorPanelFacade designer) {
    myDesignSurface = getDesignSurface(designer);
  }

  @Nullable
  @Override
  protected DesignerEditorPanelFacade getDesigner(@Nullable FileEditor editor) {
    return null;
  }

  @Override
  protected ToggleEditorModeAction createToggleAction(@NotNull ToolWindowAnchor anchor) {
    return new ToggleEditorModeAction(this, myProject, anchor) {
      @Override
      protected LightToolWindowManager getOppositeManager() {
        return null;
      }
    };
  }

  @NotNull
  protected static DesignSurface getDesignSurface(@Nullable DesignerEditorPanelFacade designer) {
    if (designer instanceof DesignerEditorPanel) {
      DesignerEditorPanel editor = (DesignerEditorPanel)designer;
      return editor.getSurface();
    }

    String facadeName = designer == null ? "null" : designer.getClass().getName();
    throw new IllegalStateException(String.format("Unexpected designer facade found: %s", facadeName));
  }

  protected void createWindowContent(@NotNull JComponent contentPane, @NotNull JComponent focusedComponent, @Nullable AnAction[] actions) {
    ContentManager contentManager = myToolWindow.getContentManager();
    Content content = contentManager.getFactory().createContent(contentPane, null, false);
    content.setCloseable(false);
    content.setPreferredFocusableComponent(focusedComponent);
    if (actions != null) {
      ToolWindowEx toolWindow = (ToolWindowEx)myToolWindow;
      toolWindow.setTitleActions(actions);
    }
    contentManager.addContent(content);
    contentManager.setSelectedContent(content, true);
  }
}
