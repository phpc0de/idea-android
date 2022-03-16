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
package com.android.tools.idea.run;

import junit.framework.TestCase;

import static com.android.tools.idea.run.tasks.AndroidDeepLinkLaunchTask.getLaunchDeepLinkCommand;

public class AndroidDeepLinkLaunchTaskTest extends TestCase {
  public void testAmStartCommandForDeepLink() {
    assertEquals("am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'example://host/path'",
                 getLaunchDeepLinkCommand("example://host/path", ""));
    assertEquals("am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'example://host/path' -D",
                 getLaunchDeepLinkCommand("example://host/path", "-D"));
    assertEquals("am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'example://host/path' -D --es aa 'bb'",
                 getLaunchDeepLinkCommand("example://host/path", "-D --es aa 'bb'"));
  }

  public void testEscapeAdbShell() {
    assertEquals("am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://example.com/example?foo=bar&baz=duck'",
                 getLaunchDeepLinkCommand("https://example.com/example?foo=bar&baz=duck", ""));
    assertEquals("am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'text'\\''with'\\''single'\\''quotes'",
                 getLaunchDeepLinkCommand("text'with'single'quotes", ""));
  }
}
