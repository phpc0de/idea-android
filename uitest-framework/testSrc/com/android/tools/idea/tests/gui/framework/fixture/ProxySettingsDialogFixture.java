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
package com.android.tools.idea.tests.gui.framework.fixture;

import static com.android.tools.idea.tests.gui.framework.GuiTests.findAndClickButton;

import com.android.tools.idea.gradle.project.ProxySettingsDialog;
import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.PortField;
import com.intellij.ui.RawCommandLineEditor;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.JCheckBoxFixture;
import org.jetbrains.annotations.NotNull;

public class ProxySettingsDialogFixture extends IdeaDialogFixture<DialogWrapper> {
  @NotNull
  public static ProxySettingsDialogFixture find(@NotNull Robot robot) {
    final Ref<DialogWrapper> wrapperRef = new Ref<>();
    JDialog dialog = GuiTests.waitUntilShowing(robot, new GenericTypeMatcher<JDialog>(JDialog.class) {
      @Override
      protected boolean isMatching(@NotNull JDialog dialog) {
        DialogWrapper wrapper = getDialogWrapperFrom(dialog, DialogWrapper.class);
        if (wrapper != null && wrapper.getClass() == ProxySettingsDialog.class) {
          wrapperRef.set(wrapper);
          return true;
        }
        return false;
      }
    });

    return new ProxySettingsDialogFixture(robot, dialog, wrapperRef.get());
  }

  private ProxySettingsDialogFixture(@NotNull Robot robot, @NotNull JDialog target, @NotNull DialogWrapper dialogWrapper) {
    super(robot, target, dialogWrapper);
  }

  public void clickYes() {
    findAndClickButton(this, "Yes");
  }

  public void clickNo() {
    findAndClickButton(this, "No");
  }

  public void enableHttpsProxy() {
    JCheckBox checkBox = robot().finder().find(target(), Matchers.byText(JCheckBox.class, "Enable HTTPS Proxy"));
    new JCheckBoxFixture(robot(), checkBox).select();
  }

  public void setDoNotShowThisDialog(boolean selected) {
    JCheckBox checkBox = robot().finder().find(Matchers.byText(JCheckBox.class, "Do not show this dialog in the future"));
    new JCheckBoxFixture(robot(), checkBox).setSelected(selected);
  }

  @NotNull
  public String getHttpHost() {
    JTextField hostField = robot().finder().find(Matchers.byName(JTextField.class, "httpHost"));
    return hostField.getText();
  }

  public int getHttpPort() {
    PortField portField = robot().finder().find(Matchers.byName(PortField.class, "httpPort"));
    return portField.getNumber();
  }

  @NotNull
  public String getHttpUser() {
    JTextField userField = robot().finder().find(Matchers.byName(JTextField.class, "httpUser"));
    return userField.getText();
  }

  @NotNull
  public String getHttpExceptions() {
    RawCommandLineEditor userField = robot().finder().find(Matchers.byName(RawCommandLineEditor.class, "httpExceptions"));
    return userField.getText();
  }

  @NotNull
  public String getHttpsHost() {
    JTextField hostField = robot().finder().find(Matchers.byName(JTextField.class, "httpsHost"));
    return hostField.getText();
  }

  public int getHttpsPort() {
    PortField portField = robot().finder().find(Matchers.byName(PortField.class, "httpsPort"));
    return portField.getNumber();
  }

  @NotNull
  public String getHttpsUser() {
    JTextField userField = robot().finder().find(Matchers.byName(JTextField.class, "httpsUser"));
    return userField.getText();
  }

  @NotNull
  public String getHttpsExceptions() {
    RawCommandLineEditor userField = robot().finder().find(Matchers.byName(RawCommandLineEditor.class, "httpsExceptions"));
    return userField.getText();
  }
}
