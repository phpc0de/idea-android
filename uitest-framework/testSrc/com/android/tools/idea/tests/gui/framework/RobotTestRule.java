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
package com.android.tools.idea.tests.gui.framework;

import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.Robot;
import org.junit.rules.ExternalResource;

/** Holds a {@link org.fest.swing.core.BasicRobot} and associated {@link org.fest.swing.lock.ScreenLock}. */
class RobotTestRule extends ExternalResource {

  private StudioRobot myRobot;

  @Override
  protected void before() {
    Robot baseRobot = BasicRobot.robotWithCurrentAwtHierarchy();  // acquires ScreenLock
    myRobot = new StudioRobot(baseRobot);
    myRobot.settings().delayBetweenEvents(30);
  }

  @Override
  protected void after() {
    myRobot.cleanUpWithoutDisposingWindows();  // releases ScreenLock
  }

  public StudioRobot getRobot() {
    return myRobot;
  }
}
