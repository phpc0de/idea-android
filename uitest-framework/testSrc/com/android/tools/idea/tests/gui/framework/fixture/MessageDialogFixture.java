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

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.messages.MessageDialog;
import com.intellij.openapi.util.Ref;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiQuery;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.google.common.base.Strings.nullToEmpty;

class MessageDialogFixture extends IdeaDialogFixture<DialogWrapper> implements MessagesFixture.Delegate {
  @NotNull
  static MessageDialogFixture findByTitle(@NotNull Robot robot, @NotNull final String title, long secondsToWait) {
    final Ref<DialogWrapper> wrapperRef = new Ref<>();
    JDialog dialog = GuiTests.waitUntilShowing(robot, null, Matchers.byTitle(JDialog.class, title).and(
      new GenericTypeMatcher<JDialog>(JDialog.class) {
        @Override
        protected boolean isMatching(@NotNull JDialog dialog) {
          DialogWrapper wrapper = getDialogWrapperFrom(dialog, DialogWrapper.class);
          if (wrapper != null) {
            if (MessageDialog.class.getName().equals(wrapper.getClass().getName())) {
              wrapperRef.set(wrapper);
              return true;
            }
          }
          return false;
        }
      }), secondsToWait);
    return new MessageDialogFixture(robot, dialog, wrapperRef.get());
  }

  private MessageDialogFixture(@NotNull Robot robot, @NotNull JDialog target, @NotNull DialogWrapper dialogWrapper) {
    super(robot, target, dialogWrapper);
  }

  @Override
  @NotNull
  public String getMessage() {
    final JTextPane textPane = robot().finder().findByType(target(), JTextPane.class);
    return GuiQuery.getNonNull(() -> nullToEmpty(textPane.getText()));
  }
}
