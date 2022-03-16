/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.welcome.wizard.deprecated;

import static com.android.tools.idea.gradle.project.AndroidGradleProjectSettingsControlBuilder.ANDROID_STUDIO_DEFAULT_JDK_NAME;
import static com.android.tools.idea.gradle.ui.SdkUiStrings.JDK_LOCATION_TOOLTIP;
import static com.android.tools.idea.gradle.ui.SdkUiStrings.JDK_LOCATION_WARNING_URL;
import static com.android.tools.idea.gradle.ui.SdkUiStrings.generateChooseValidJdkDirectoryError;
import static com.android.tools.idea.gradle.ui.SdkUiUtils.getLocationFromComboBoxWithBrowseButton;
import static com.android.tools.idea.sdk.IdeSdks.getJdkFromJavaHome;
import static com.android.tools.idea.wizard.WizardConstants.KEY_JDK_LOCATION;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.ui.LabelAndFileForLocation;
import com.android.tools.idea.sdk.IdeSdks;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wizard step for JDK setup.
 * @deprecated use {@link com.android.tools.idea.welcome.wizard.JdkSetupStep}
 */
public class JdkSetupStep extends FirstRunWizardStep {
  @SuppressWarnings("unused") private ComboboxWithBrowseButton myJdkLocationComboBox;
  private JBLabel myJdkLocationHelp;
  private JBScrollPane myContents;

  public JdkSetupStep() {
    super("Select default JDK Location");
    setUpJdkLocationComboBox();
    setComponent(myContents);
    createUIComponents();
  }

  private void setUpJdkLocationComboBox() {
    FileChooserDescriptor descriptor = createSingleFolderDescriptor(file -> {
      Path validatedFile = validateJdkPath(file);
      if (validatedFile == null) {
        throw new IllegalArgumentException(generateChooseValidJdkDirectoryError());
      }
      setJdkLocationComboBox(file);
      return null;
    });

    myJdkLocationComboBox.addBrowseFolderListener(getProject(), descriptor);
    JComboBox comboBox = myJdkLocationComboBox.getComboBox();
    IdeSdks ideSdks = IdeSdks.getInstance();
    Path embeddedPath = ideSdks.getEmbeddedJdkPath();
    if (embeddedPath != null) {
      Path validatedPath = validateJdkPath(embeddedPath);
      if (validatedPath != null) {
        comboBox.addItem(new LabelAndFileForLocation("Embedded JDK", validatedPath));
      }
    }
    String javaHomePath = getJdkFromJavaHome();
    if (javaHomePath != null) {
      Path validatedPath = validateJdkPath(Paths.get(javaHomePath));
      boolean isJavaHomeValid = validatedPath != null;
      if (isJavaHomeValid) {
        comboBox.addItem(new LabelAndFileForLocation("JAVA_HOME", validatedPath));
      }
    }
    comboBox.setEditable(true);
    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          Object selectedItem = event.getItem();
          if (selectedItem instanceof LabelAndFileForLocation) {
            ApplicationManager.getApplication().invokeLater(() -> setJdkLocationComboBox(((LabelAndFileForLocation)selectedItem).getFile()));
          }
        }
      }
    });
    setJdkLocationComboBox(embeddedPath);
  }

  private void setJdkLocationComboBox(@Nullable Path path) {
    myJdkLocationComboBox.getComboBox().setSelectedItem(path == null ? null : path.toString());
    updateIsValidPath();
  }

  private void createUIComponents() {
    myJdkLocationHelp = ContextHelpLabel.createWithLink(null, JDK_LOCATION_TOOLTIP, "Learn more",
                                                        () -> BrowserUtil.browse(JDK_LOCATION_WARNING_URL));
  }

  @NotNull
  private static FileChooserDescriptor createSingleFolderDescriptor(@NotNull Function<? super Path, Void> validation) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
      @Override
      public void validateSelectedFiles(VirtualFile[] files) {
        for (VirtualFile virtualFile : files) {
          validation.apply(virtualFile.toNioPath());
        }
      }
    };
    if (SystemInfo.isMac) {
      descriptor.withShowHiddenFiles(true);
    }
    descriptor.setTitle("Choose JDK Location");
    return descriptor;
  }

  private void updateIsValidPath() {
    invokeUpdate(null);
  }

  @Nullable
  private Path validateJdkPath(@NotNull Path file) {
    Path possiblePath = IdeSdks.getInstance().validateJdkPath(file);
    if (possiblePath != null) {
      setJdkLocationComboBox(possiblePath);
      return possiblePath;
    }
    return null;
  }

  @Override
  public void init() {
    // Apply default selection
    IdeSdks ideSdks = IdeSdks.getInstance();
    setJdkLocationComboBox(ideSdks.getEmbeddedJdkPath());
  }

  @Nullable
  @Override
  public JLabel getMessageLabel() {
    return null;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myJdkLocationComboBox;
  }

  @Override
  public boolean validate() {
    if (!isValidJdkPath()) {
      return false;
    }
    return super.validate();
  }

  private boolean isValidJdkPath() {
    return validateJdkPath(getJdkLocation()) != null;
  }

  @Override
  public boolean commitStep() {
    if (!isValidJdkPath()) {
      return false;
    }
    Path path = getJdkLocation();
    IdeSdks.findOrCreateJdk(ANDROID_STUDIO_DEFAULT_JDK_NAME, path);
    myState.put(KEY_JDK_LOCATION, path.toString());
    return true;
  }

  @NotNull
  private Path getJdkLocation() {
    return getLocationFromComboBoxWithBrowseButton(myJdkLocationComboBox);
  }

  @Override
  public boolean isStepVisible() {
    return StudioFlags.NPW_SHOW_JDK_STEP.get() && Boolean.TRUE.equals(myState.get(FirstRunWizard.KEY_CUSTOM_INSTALL));
  }
}
