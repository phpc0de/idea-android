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
package com.android.tools.idea.diagnostics;

import com.android.tools.idea.diagnostics.error.ErrorReporter;
import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.PlatformTestCase;

/**
 * Tests for {@link SystemHealthMonitor}.
 */
public class AndroidStudioSystemHealthMonitorTest extends PlatformTestCase {

  public void testGetActionName() {
    // normal class in our packages should yield simple name
    assertEquals("AndroidStudioSystemHealthMonitorTest", AndroidStudioSystemHealthMonitor.getActionName(AndroidStudioSystemHealthMonitorTest.class, new Presentation("foo")));
    // ExecutorAction class should yield simple name plus presentation text.
    assertEquals("ExecutorAction#Run", AndroidStudioSystemHealthMonitor.getActionName(ExecutorAction.class, new Presentation("Run")));
    // Anonymous inner-class should yield name of enclosing class.
    assertEquals("AnAction@AndroidStudioSystemHealthMonitorTest", AndroidStudioSystemHealthMonitor.getActionName(new AnAction(){
      @Override
      public void actionPerformed(AnActionEvent e) {

      }
    }.getClass(), new Presentation("foo")));
    // class outside of our packages should yield full class name.
    assertEquals("java.lang.String", AndroidStudioSystemHealthMonitor.getActionName(String.class, new Presentation("Foo")));
  }

  public void testAndroidErrorReporter() {
    // Regression test for b/130834409.
    assertTrue(
      "Unexpected type returned from IdeErrorsDialog.getAndroidErrorReporter()",
      false);
      //IdeErrorsDialog.getAndroidErrorReporter() instanceof ErrorReporter); // FIXME-ank
  }

}

/**
 * Dummy class needed for {@link #testGetActionName()}.
 */
class ExecutorAction {}
