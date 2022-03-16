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

import static com.android.tools.idea.tests.gui.framework.GuiTests.waitUntilShowing;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.truth.Truth.assertThat;
import static com.intellij.util.ui.UIUtil.findComponentOfType;
import static com.intellij.util.ui.UIUtil.findComponentsOfType;
import static org.fest.reflect.core.Reflection.method;

import com.android.annotations.Nullable;
import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.layout.impl.GridImpl;
import com.intellij.execution.ui.layout.impl.JBRunnerTabs;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionMenuItem;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.ThreeComponentsSplitter;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.ComponentWithMnemonics;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import java.awt.Component;
import java.awt.Container;
import java.awt.IllegalComponentStateException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.fest.swing.timing.Wait;
import org.fest.swing.util.TextMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public class ExecutionToolWindowFixture extends ToolWindowFixture {
  public static class ContentFixture {
    @NotNull private final ExecutionToolWindowFixture myParentToolWindow;
    @NotNull private final Robot myRobot;
    @NotNull private final Content myContent;

    private ContentFixture(@NotNull ExecutionToolWindowFixture parentToolWindow, @NotNull Robot robot, @NotNull Content content) {
      myParentToolWindow = parentToolWindow;
      myRobot = robot;
      myContent = content;
    }

    /**
     * Waits until it grabs the console window and then true if its text matches that of {@code matcher}.
     * Note: This method may not terminate if the console view cannot be found.
     */
    public void waitForOutput(@NotNull final TextMatcher matcher, long secondsToWait) {
      Wait.seconds(secondsToWait).
        // TODO We're not waiting on logcat. This message should be updated
        expecting("LogCat tool window output check for package name")
        .until(() -> outputMatches(matcher));
    }

    public boolean outputMatches(@NotNull TextMatcher matcher) {
      String output = pollOutput();
      if (output == null) {
        return false;
      }
      return matcher.isMatching(output);
    }

    @NotNull
    public String getOutput(long secondsToWait) {
      Ref<String> outputText = new Ref<>();
      Wait.seconds(secondsToWait)
        .expecting("output text to not be null")
        .until(() -> {
          String output = pollOutput();
          if (output != null) {
            outputText.set(output);
          }
          return output != null;
        });
      return outputText.get();
    }

    @Nullable
    private String pollOutput() {
      ConsoleViewImpl consoleView = findVisibleConsoleView();
      if (consoleView == null || consoleView.getEditor() == null) {
        JComponent consoleComponent = getTabComponent("Console");
        myRobot.click(consoleComponent);
        return null;
      }

      return consoleView.getEditor().getDocument().getText();
    }

    // Returns the currently visible console or null if it is not found.
    @Nullable
    private ConsoleViewImpl findVisibleConsoleView() {
      try {
        return myRobot.finder().findByType(myParentToolWindow.myToolWindow.getComponent(), ConsoleViewImpl.class, true);
      } catch (ComponentLookupException e) {
        return null;
      }
    }

    @NotNull
    public JComponent getTabComponent(@NotNull final String tabName) {
      return getTabContent(myParentToolWindow.myToolWindow.getComponent(), JBRunnerTabs.class, GridImpl.class, tabName);
    }

    // Returns the debugger tree or null.
    @Nullable
    public XDebuggerTree getDebuggerTree() {
      try {
        // There is a click on tab inside of getTabComponent() method.
        JComponent debuggerComponent = getTabComponent("Debugger");
        ThreeComponentsSplitter threeComponentsSplitter =
            myRobot.finder().findByType(debuggerComponent, ThreeComponentsSplitter.class, false);
        JComponent innerComponent = threeComponentsSplitter.getInnerComponent();
        return myRobot.finder().findByType(innerComponent, XDebuggerTree.class, false);
      } catch (ComponentLookupException | IllegalComponentStateException e) {
        return null;
      }
    }

    // Returns the root of the debugger tree or null.
    @Nullable
    public XDebuggerTreeNode getDebuggerTreeRoot() {
      XDebuggerTree debuggerTree = getDebuggerTree();
      if (debuggerTree == null) {
        return null;
      }
      return debuggerTree.getRoot();
    }

    @NotNull
    public JListFixture getFramesListFixture() {
      Wait.seconds(5).expecting("Frames list present").until(() -> getFramesList() != null);
      return new JListFixture(myRobot, getFramesList());
    }

    @Nullable
    private XDebuggerFramesList getFramesList() {
      try {
        JComponent debuggerComponent = getTabComponent("Debugger");
        myRobot.click(debuggerComponent);
        ThreeComponentsSplitter splitter =
          myRobot.finder().findByType(debuggerComponent, ThreeComponentsSplitter.class, false);
        JComponent component = splitter.getFirstComponent();
        return myRobot.finder().findByType(component, XDebuggerFramesList.class, false);
      } catch (ComponentLookupException | IllegalComponentStateException e) {
        return null;
      }
    }

    public JBPopupMenu rightClickVariableInDebuggerVariables(@NotNull IdeFrameFixture ideFrame, @NotNull String variableName) {
      Wait.seconds(5).expecting("Debugger tree present").until(() -> getDebuggerTree() != null);
      JTree tree = (JTree)getDebuggerTree();

      JTreeFixture jTreeFixture = new JTreeFixture(myRobot, tree);
      int index = -1;
      int rowCount = tree.getRowCount();
      for (int i = 0; i < rowCount; i++) {
        String value = jTreeFixture.valueAt(i);
        if (variableName.equals(value)) {
          index = i;
          break;
        }
      }
      assertThat(index > -1).isTrue();

      Wait.seconds(5).expecting("debugger tree to be enabled").until(() -> tree.isEnabled());
      jTreeFixture.rightClickRow(index);
      return GuiTests.waitUntilShowingAndEnabled(myRobot, ideFrame.target(), Matchers.byType(JBPopupMenu.class));
    }

    public WatchpointConfigFixture findWatchpointConfig(@NotNull IdeFrameFixture ideFrame, @Nullable Container popupMenu) {
      Component addWatchpoint = GuiTests.waitUntilShowingAndEnabled(
          myRobot,
          popupMenu,
          Matchers.byText(ActionMenuItem.class, "Add Watchpoint"));
      myRobot.click(addWatchpoint);

      JPanel watchpointConfig = myRobot.finder().find(ideFrame.target(), new GenericTypeMatcher<JPanel>(JPanel.class) {
        @Override
        protected boolean isMatching(@NotNull JPanel jPanel) {
          try {
            if (jPanel instanceof ComponentWithMnemonics) {
              myRobot.finder().find(jPanel, Matchers.byText(JCheckBox.class, "Enabled"));
              myRobot.finder().find(jPanel, Matchers.byText(JCheckBox.class, "Suspend"));
              myRobot.finder().find(jPanel, Matchers.byText(JLabel.class, "Access Type:"));
              myRobot.finder().find(jPanel, Matchers.byText(LinkLabel.class, "More (Ctrl+Shift+F8)"));
              myRobot.finder().find(jPanel, Matchers.byText(JButton.class, "Done"));
              return true;
            }
          }
          catch (ComponentLookupException e) {
            return false;
          }

          return false;
        }
      });

      return new WatchpointConfigFixture(myRobot, watchpointConfig);
    }

    public void clickResumeButton() {
      for (ActionButton button : getToolbarButtons()) {
        if ("com.intellij.xdebugger.impl.actions.ResumeAction".equals(button.getAction().getClass().getCanonicalName())) {
          myRobot.click(button);
          return;
        }
      }

      throw new IllegalStateException("Could not find the Resume button.");
    }

    @NotNull
    public ContentFixture clickClearAllButton() {
      List<ActionButton> debug_ActionButtons = getConsoleToolbarButtons();
      for (ActionButton button : debug_ActionButtons) {
        String tooltipText = button.getToolTipText();
        if ("Clear All".equals(tooltipText)) {
          myRobot.click(button);
          return this;
        }
      }

      throw new IllegalStateException("Could not find the Clear All button");
    }

    @NotNull
    private JComponent getTabContent(@NotNull final JComponent root,
                                     final Class<? extends JBTabsImpl> parentComponentType,
                                     @NotNull final Class<? extends JComponent> tabContentType,
                                     @NotNull final String tabName) {
      myParentToolWindow.activate();
      myParentToolWindow.waitUntilIsVisible();

      TabLabel tabLabel;
      if (parentComponentType == null) {
        tabLabel = waitUntilShowing(myRobot, new GenericTypeMatcher<TabLabel>(TabLabel.class) {
          @Override
          protected boolean isMatching(@NotNull TabLabel component) {
            return component.toString().equals(tabName);
          }
        });
      }
      else {
        final JComponent parent = myRobot.finder().findByType(root, parentComponentType, false);
        tabLabel = waitUntilShowing(myRobot, parent, new GenericTypeMatcher<TabLabel>(TabLabel.class) {
          @Override
          protected boolean isMatching(@NotNull TabLabel component) {
            return component.getParent() == parent && component.toString().equals(tabName);
          }
        });
      }
      myRobot.click(tabLabel);
      return waitUntilShowing(myRobot, Matchers.byType(tabContentType));
    }

    public boolean isExecutionInProgress() {
      // Consider that execution is in progress if 'stop' toolbar button is enabled.
      for (ActionButton button : getToolbarButtons()) {
        if ("com.intellij.execution.actions.StopAction".equals(button.getAction().getClass().getCanonicalName())) {
          //noinspection ConstantConditions
          return method("isButtonEnabled").withReturnType(boolean.class).in(button).invoke();
        }
      }
      return true;
    }

    public void waitForExecutionToFinish() {
      Wait.seconds(10).expecting("execution to finish").until(() -> !isExecutionInProgress());
    }

    @TestOnly
    public ContentFixture stop() {
      for (final ActionButton button : getToolbarButtons()) {
        final AnAction action = button.getAction();
        if (action != null && action.getClass().getName().equals("com.intellij.execution.actions.StopAction")) {
          //noinspection ConstantConditions
          boolean enabled = method("isButtonEnabled").withReturnType(boolean.class).in(button).invoke();
          if (enabled) {
            GuiTask.execute(() -> button.click());
            return this;
          }
          throw new IllegalStateException(button.getName() + " button is disabled");
        }
      }
      throw new IllegalStateException("no StopAction button found");
    }

    public void waitForStopClick() {
      Wait.seconds(5).expecting("Stop button clicked").until(() -> {
        for (ActionButton button : getToolbarButtons()) {
          if (button.getAction() == null || !button.getAction().getClass().getName().equals("com.intellij.execution.actions.StopAction")) {
            continue;
          }
          if (button.isEnabled() && button.isShowing()) {
            GuiTask.execute(() -> button.click());
            return true;
          }
        }
        return false;
      });
    }

    @NotNull
    private List<ActionButton> getToolbarButtons() {
      ActionToolbarImpl toolbar = verifyNotNull(findComponentOfType(myContent.getComponent(), ActionToolbarImpl.class));
      return findComponentsOfType(toolbar, ActionButton.class);
    }

    @NotNull
    private List<ActionButton> getConsoleToolbarButtons() {
      ConsoleViewImpl consoleView = verifyNotNull(findVisibleConsoleView());
      Container commonAncestor = SwingUtilities.getAncestorOfClass(JBTabs.class, consoleView);
      Container actionToolbar = myRobot.finder().find(commonAncestor, Matchers.byType(ActionToolbarImpl.class));
      return new ArrayList<>(myRobot.finder().findAll(actionToolbar, Matchers.byType(ActionButton.class)));
    }
  }  // End class ContentFixture

  protected ExecutionToolWindowFixture(@NotNull String toolWindowId, @NotNull IdeFrameFixture ideFrame) {
    super(toolWindowId, ideFrame.getProject(), ideFrame.robot());
  }

  @NotNull
  public ContentFixture findContent(@NotNull String tabName) {
    return new ContentFixture(this, myRobot, getContent(tabName));
  }

  @NotNull
  public ContentFixture findContent(@NotNull TextMatcher tabNameMatcher) {
    return new ContentFixture(this, myRobot, getContent(tabNameMatcher));
  }

  @NotNull
  private JPanel getContentPanel() {
    return (JPanel)myToolWindow.getContentManager().getComponent();
  }

  @NotNull
  private ActionButtonFixture findAction(@NotNull String text) {
    return ActionButtonFixture.findByText(text, myRobot, getContentPanel());
  }
}
