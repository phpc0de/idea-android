/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.ddmlib.Client;
import com.android.tools.idea.logcat.AndroidLogConsole;
import com.android.tools.idea.logcat.AndroidLogcatToolWindowFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.openapi.project.Project;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.fest.swing.core.Robot;
import org.fest.swing.core.matcher.JLabelMatcher;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.timing.Wait;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidLogcatToolWindowFixture extends ToolWindowFixture {
  @NotNull private final ProcessListFixture myProcessListFixture;

  public AndroidLogcatToolWindowFixture(@NotNull Project project, final @NotNull Robot robot) {
    super(AndroidLogcatToolWindowFactory.getToolWindowId(), project, robot);
    show();

    final JPanel contentPanel = getContentPanel();
    Wait.seconds(1).expecting("window to finish initializing")
      .until(() -> {
        try {
          myRobot.finder().find(contentPanel, JLabelMatcher.withText("Initializing..."));
        }
        catch (ComponentLookupException e) {
          // Didn't find it. Great, we're done!
          return true;
        }
        return false;
      });

    myProcessListFixture = new ProcessListFixture(robot, robot.finder().findByName(contentPanel, "Processes", JComboBox.class));
  }

  @NotNull
  public AndroidLogcatToolWindowFixture selectProcess(@NotNull String packageName) {
    myProcessListFixture.waitForProcess(packageName).selectItem(packageName);
    return this;
  }

  @Nullable
  public String getLogcatPrint() {
    AndroidLogConsole androidLogConsole = myRobot.finder().findByType(myToolWindow.getComponent(), AndroidLogConsole.class, true);
    ConsoleViewImpl consoleView = myRobot.finder().findByType(androidLogConsole.getComponent(), ConsoleViewImpl.class, true /* showing */);
    return consoleView.getText();
  }

  @NotNull
  private ActionButtonFixture findAction(@NotNull String text) {
    return ActionButtonFixture.findByText(text, myRobot, getContentPanel());
  }

  @NotNull
  private JPanel getContentPanel() {
    return (JPanel)myToolWindow.getContentManager().getComponent();
  }

  public @NotNull AndroidLogcatToolWindowFixture show() {
    activate();
    waitUntilIsVisible();

    return this;
  }

  private static class ProcessListFixture extends JComboBoxFixture {
    ProcessListFixture(@NotNull Robot robot, @NotNull JComboBox target) {
      super(robot, target);
    }

    @NotNull
    public ProcessListFixture waitForProcess(@NotNull final String packageName) {
      Wait.seconds(5).expecting("the process list to show the package name").until(() -> GuiQuery.getNonNull(() -> {
        ComboBoxModel model = target().getModel();
        int size = model.getSize();
        for (int i = 0; i < size; ++i) {
          Client client = (Client)model.getElementAt(i);
          String clientDescription = client.getClientData().getClientDescription();
          if (packageName.equals(clientDescription)) {
            return true;
          }
        }
        return false;
      }));
      return this;
    }

    @Override
    @NotNull
    public ProcessListFixture selectItem(@Nullable final String packageName) {
      clearSelection();
      GuiActionRunner.execute(new GuiTask() {
        @Override
        protected void executeInEDT() {
          for (int i = 0; i < target().getModel().getSize(); ++i) {
            Client client = (Client)target().getModel().getElementAt(i);
            if (packageName.equals(client.getClientData().getClientDescription())) {
              target().getModel().setSelectedItem(client);
              return;
            }
          }
          throw new RuntimeException("Failed to find " + packageName + " in process list.");
        }
      });
      return this;
    }
  }
}
