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
package com.android.tools.adtui.workbench;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.actions.ToggleDistractionFreeModeAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All {@link WorkBench}es of a specified name will use the same {@link DetachedToolWindow}
 * for a given tool window name.
 * This class is responsible for switching the content to the content of the currently
 * active {@link WorkBench}.
 */
public class DetachedToolWindowManager implements Disposable {
  private final Project myProject;
  private final MyFileEditorManagerListener myEditorManagerListener;
  private final Map<FileEditor, WorkBench<?>> myWorkBenchMap;
  private final Map<String, DetachedToolWindow<?>> myToolWindowMap;

  private DetachedToolWindowFactory myDetachedToolWindowFactory;
  private FileEditor myLastSelectedEditor;

  public static DetachedToolWindowManager getInstance(@NotNull Project project) {
    return project.getService(DetachedToolWindowManager.class);
  }

  @VisibleForTesting
  DetachedToolWindowManager(@NotNull Project project) {
    myProject = project;
    myEditorManagerListener = new MyFileEditorManagerListener();
    myWorkBenchMap = new IdentityHashMap<>(13);
    myToolWindowMap = new HashMap<>(8);
    myDetachedToolWindowFactory = DetachedToolWindow::new;
    myProject.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myEditorManagerListener);
  }

  @VisibleForTesting
  void setDetachedToolWindowFactory(@NotNull DetachedToolWindowFactory factory) {
    myDetachedToolWindowFactory = factory;
  }

  @VisibleForTesting
  FileEditorManagerListener getFileEditorManagerListener() {
    return myEditorManagerListener;
  }

  public void register(@Nullable FileEditor fileEditor, @NotNull WorkBench<?> workBench) {
    if (fileEditor != null) {
      myWorkBenchMap.put(fileEditor, workBench);
      if (fileEditor == myLastSelectedEditor) {
        updateToolWindowsForWorkBench(workBench);
      }
    }
  }

  public void unregister(@Nullable FileEditor fileEditor) {
    if (fileEditor != null) {
      myWorkBenchMap.remove(fileEditor);
    }
  }

  @Override
  public void dispose() {
    for (DetachedToolWindow detachedToolWindow : myToolWindowMap.values()) {
      detachedToolWindow.updateSettingsInAttachedToolWindow();
    }
  }

  @Nullable
  private WorkBench getActiveWorkBench() {
    if (myProject.isDisposed()){
      return null;
    }
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner instanceof WorkBench) {
      return (WorkBench)focusOwner;
    }
    WorkBench current = (WorkBench)SwingUtilities.getAncestorOfClass(WorkBench.class, focusOwner);
    if (current != null) {
      return current;
    }
    FileEditor[] selectedEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
    if (selectedEditors.length == 0) {
      return null;
    }
    if (selectedEditors.length == 1) {
      return myWorkBenchMap.get(selectedEditors[0]);
    }
    if (focusOwner != null) {
      for (FileEditor editor : selectedEditors) {
        if (SwingUtilities.isDescendingFrom(focusOwner, editor.getComponent())) {
          return myWorkBenchMap.get(editor);
        }
      }
    }
    return myWorkBenchMap.get(selectedEditors[0]);
  }

  public void updateToolWindowsForWorkBench(@Nullable WorkBench workBench) {
    if (myProject.isDisposed()) {
      return;
    }
    Set<String> ids = new HashSet<>(myToolWindowMap.keySet());
    if (workBench != null) {
      String workBenchPrefix = workBench.getName() + ".";
      //noinspection unchecked
      List<AttachedToolWindow> detachedToolWindows = workBench.getDetachedToolWindows();
      for (AttachedToolWindow tool : detachedToolWindows) {
        ToolWindowDefinition definition = tool.getDefinition();
        String id = workBenchPrefix + definition.getName();
        DetachedToolWindow detachedToolWindow = myToolWindowMap.get(id);
        if (detachedToolWindow == null) {
          String workBenchName = getWorkBenchTitleName(workBench);
          detachedToolWindow = myDetachedToolWindowFactory.create(myProject, workBenchName, definition);
          Disposer.register(myProject, detachedToolWindow);
          myToolWindowMap.put(id, detachedToolWindow);
        }
        if (!ToggleDistractionFreeModeAction.isDistractionFreeModeEnabled()) {
          //noinspection unchecked
          detachedToolWindow.show(tool);
        }
        ids.remove(id);
      }
    }
    ids.forEach(id -> myToolWindowMap.get(id).hide());
  }

  public void restoreDefaultLayout() {
    ApplicationManager.getApplication().invokeLater(() -> updateToolWindowsForWorkBench(getActiveWorkBench()));
  }

  @NotNull
  private static String getWorkBenchTitleName(@NotNull WorkBench workBench) {
    switch (workBench.getName()) {
      case "NELE_EDITOR": return "Designer";
      case "NAV_EDITOR": return "Navigation";
      default: return workBench.getName();
    }
  }

  private class MyFileEditorManagerListener implements FileEditorManagerListener {

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      ApplicationManager.getApplication().invokeLater(() -> updateToolWindowsForWorkBench(getActiveWorkBench()));
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      ApplicationManager.getApplication().invokeLater(() -> updateToolWindowsForWorkBench(getActiveWorkBench()));
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
      myLastSelectedEditor = event.getNewEditor();
      updateToolWindowsForWorkBench(myWorkBenchMap.get(myLastSelectedEditor));
    }
  }

  interface DetachedToolWindowFactory {
    DetachedToolWindow create(@NotNull Project project, @NotNull String workBenchName, @NotNull ToolWindowDefinition definition);
  }
}
