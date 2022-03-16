// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android.sdk;

import com.android.tools.idea.model.AndroidModel;
import com.android.tools.idea.res.IdeResourcesUtil;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidSdkNotConfiguredNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  private static final Key<EditorNotificationPanel> KEY = Key.create("android.sdk.not.configured.notification");

  private final Project myProject;

  public AndroidSdkNotConfiguredNotificationProvider(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (file.getFileType() != XmlFileType.INSTANCE) {
      return null;
    }
    final Module module = ModuleUtilCore.findModuleForFile(file, myProject);
    final AndroidFacet facet = module != null ? AndroidFacet.getInstance(module) : null;

    if (facet == null) {
      return null;
    }
    if (!AndroidModel.isRequired(facet)
        && (IdeResourcesUtil.isResourceFile(file, facet) || file.equals(AndroidRootUtil.getPrimaryManifestFile(facet)))) {
      final AndroidPlatform platform = AndroidPlatform.getInstance(module);

      if (platform == null) {
        return new MySdkNotConfiguredNotificationPanel(fileEditor, module);
      }
    }
    return null;
  }

  private class MySdkNotConfiguredNotificationPanel extends EditorNotificationPanel {

    MySdkNotConfiguredNotificationPanel(@NotNull FileEditor fileEditor, @NotNull final Module module) {
      super(fileEditor);

      setText("Android SDK is not configured for module '" + module.getName() + "' or corrupted");

      createActionLabel("Open Project Structure", new Runnable() {
        @Override
        public void run() {
          ModulesConfigurator.showDialog(module.getProject(), module.getName(), ClasspathEditor.getName());
          EditorNotifications.getInstance(myProject).updateAllNotifications();
        }
      });
    }
  }
}

