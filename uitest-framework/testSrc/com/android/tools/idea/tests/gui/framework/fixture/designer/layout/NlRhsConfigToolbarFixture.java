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
package com.android.tools.idea.tests.gui.framework.fixture.designer.layout;

import static com.android.tools.idea.tests.gui.framework.GuiTests.waitUntilShowing;
import static com.android.tools.idea.tests.gui.framework.GuiTests.waitUntilShowingAndEnabled;

import com.android.tools.idea.common.actions.IssueNotificationAction;
import com.android.tools.idea.actions.DesignerActions;
import com.android.tools.idea.common.error.IssuePanel;
import com.android.tools.idea.tests.gui.framework.fixture.ActionButtonFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlEditorFixture;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.keymap.MacKeymapUtil;
import com.intellij.openapi.util.SystemInfo;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.jetbrains.annotations.NotNull;

/**
 * Fixture representing the right hand side configuration in the first line toolbar above an associated layout editor
 */
public class NlRhsConfigToolbarFixture {

  @NotNull private final NlEditorFixture myNlEditorFixture;
  @NotNull private final ActionToolbar myToolBar;

  public NlRhsConfigToolbarFixture(@NotNull NlEditorFixture nlEditorFixture, @NotNull ActionToolbar toolbar) {
    myNlEditorFixture = nlEditorFixture;
    myToolBar = toolbar;
  }

  public void zoomToFit() {
    Robot robot = myNlEditorFixture.robot();
    String key = (SystemInfo.isMac) ? MacKeymapUtil.COMMAND : "Ctrl";
    String toolTip = "Zoom to Fit Screen (" + key + "+0)";
    ActionButton zoomToFit =
      waitUntilShowingAndEnabled(robot, myToolBar.getComponent(), Matchers.byTooltip(ActionButton.class, toolTip));
    new ActionButtonFixture(robot, zoomToFit).click();
  }

  public void clickIssuePanelButton() {
    Robot robot = myNlEditorFixture.robot();
    ActionButton button = waitUntilShowing(
      robot, myToolBar.getComponent(), new GenericTypeMatcher<ActionButton>(ActionButton.class) {
        @Override
        protected boolean isMatching(@NotNull ActionButton component) {
          return component.getAction() == ActionManager.getInstance().getAction(DesignerActions.ACTION_TOGGLE_ISSUE_PANEL);
        }
      });
    new ActionButtonFixture(robot, button).click();
    waitUntilShowing(robot, Matchers.byType(IssuePanel.class));
  }
}
