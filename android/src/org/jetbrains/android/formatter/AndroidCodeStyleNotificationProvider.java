// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android.formatter;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.ModuleResourceManagers;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidCodeStyleNotificationProvider extends EditorNotifications.Provider<AndroidCodeStyleNotificationProvider.MyPanel> {
  private static final Key<MyPanel> KEY = Key.create("android.xml.code.style.notification");

  @NonNls private static final String ANDROID_XML_CODE_STYLE_NOTIFICATION_GROUP = "Android XML code style notification";

  private final Project project;

  public AndroidCodeStyleNotificationProvider(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public Key<MyPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public MyPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
    if (file.getFileType() != XmlFileType.INSTANCE ||
        !(fileEditor instanceof TextEditor)) {
      return null;
    }
    final Module module = ModuleUtilCore.findModuleForFile(file, project);

    if (module == null) {
      return null;
    }
    final AndroidFacet facet = AndroidFacet.getInstance(module);

    if (facet == null) {
      return null;
    }
    final VirtualFile parent = file.getParent();
    final VirtualFile resDir = parent != null ? parent.getParent() : null;

    if (resDir == null || !ModuleResourceManagers.getInstance(facet).getLocalResourceManager().isResourceDir(resDir)) {
      return null;
    }
    final CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);
    final AndroidXmlCodeStyleSettings androidSettings = AndroidXmlCodeStyleSettings.getInstance(settings);

    if (androidSettings.USE_CUSTOM_SETTINGS) {
      return null;
    }
    if (NotificationsConfigurationImpl.getSettings(ANDROID_XML_CODE_STYLE_NOTIFICATION_GROUP).
      getDisplayType() == NotificationDisplayType.NONE) {
      return null;
    }
    NotificationsConfiguration.getNotificationsConfiguration().register(
      ANDROID_XML_CODE_STYLE_NOTIFICATION_GROUP, NotificationDisplayType.BALLOON, false);
    return new MyPanel(fileEditor);
  }

  public class MyPanel extends EditorNotificationPanel {

    MyPanel(@NotNull FileEditor fileEditor) {
      super(fileEditor);

      setText("You can format your XML resources in the 'standard' Android way. " +
              "Choose 'Set from... | Android' in the XML code style settings.");

      createActionLabel("Open code style settings", new Runnable() {
        @Override
        public void run() {
          ShowSettingsUtilImpl.showSettingsDialog(
            project, "preferences.sourceCode.XML", "");
          EditorNotifications.getInstance(project).updateAllNotifications();
        }
      });

      createActionLabel("Disable notification", new Runnable() {
        @Override
        public void run() {
          NotificationsConfiguration.getNotificationsConfiguration()
            .changeSettings(ANDROID_XML_CODE_STYLE_NOTIFICATION_GROUP, NotificationDisplayType.NONE, false, false);
          EditorNotifications.getInstance(project).updateAllNotifications();
        }
      });
    }
  }
}
