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
package com.android.tools.idea.tests.gui.framework.fixture.npw;

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.tests.gui.framework.fixture.wizard.AbstractWizardFixture;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import org.fest.swing.timing.Wait;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NewActivityWizardFixture extends AbstractWizardFixture<NewActivityWizardFixture> {

  private final IdeFrameFixture myIdeFrameFixture;

  private NewActivityWizardFixture(@NotNull IdeFrameFixture ideFrameFixture, @NotNull JDialog target) {
    super(NewActivityWizardFixture.class, ideFrameFixture.robot(), target);
    myIdeFrameFixture = ideFrameFixture;
  }

  @NotNull
  public static NewActivityWizardFixture find(@NotNull IdeFrameFixture ideFrameFixture) {
    JDialog dialog = GuiTests.waitUntilShowing(ideFrameFixture.robot(), Matchers.byTitle(JDialog.class, "New Android Activity"));
    return new NewActivityWizardFixture(ideFrameFixture, dialog);
  }

  @NotNull
  public ConfigureBasicActivityStepFixture<NewActivityWizardFixture> getConfigureActivityStep(@NotNull String activityTitle) {
    JRootPane rootPane = findStepWithTitle(activityTitle);
    return new ConfigureBasicActivityStepFixture<>(this, rootPane);
  }

  private void clickFinish() {
    super.clickFinish(Wait.seconds(30));
  }

  @NotNull
  public IdeFrameFixture clickFinishAndWaitForSyncToFinish() {
    return myIdeFrameFixture.actAndWaitForGradleProjectSyncToFinish(it -> clickFinish());
  }

  @NotNull
  public IdeFrameFixture clickFinishAndWaitForSyncToFinish(@NotNull Wait waitSync) {
    return myIdeFrameFixture.actAndWaitForGradleProjectSyncToFinish(waitSync, it -> clickFinish());
  }
}
