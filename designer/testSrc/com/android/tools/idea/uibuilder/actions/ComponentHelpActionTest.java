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
package com.android.tools.idea.uibuilder.actions;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.android.SdkConstants;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.android.AndroidTestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ComponentHelpActionTest extends AndroidTestCase {
  @Mock
  private DataContext myDataContext;
  @Mock
  private BrowserLauncher myBrowserLauncher;
  @Mock
  private AnActionEvent myEvent;
  private ComponentHelpAction myAction;
  private String myTagName;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    when(myEvent.getDataContext()).thenReturn(myDataContext);
    myAction = new ComponentHelpAction(getProject(), () -> myTagName);
    registerApplicationService(BrowserLauncher.class, myBrowserLauncher);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      // Null out all fields, since otherwise they're retained for the lifetime of the suite (which can be long if e.g. you're running many
      // tests through IJ)
      myDataContext = null;
      myBrowserLauncher = null;
      myEvent = null;
      myAction = null;
      myTagName = null;
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testNullTagName() {
    myTagName = null;
    myAction.actionPerformed(myEvent);
    verifyZeroInteractions(myBrowserLauncher);
  }

  public void testUnknownTagName() {
    myTagName = "UnknownComponentTagName";
    myAction.actionPerformed(myEvent);
    verifyZeroInteractions(myBrowserLauncher);
  }

  public void testButton() {
    myTagName = "Button";
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/android/widget/Button.html"), isNull(), isNull());
  }

  public void testTextureView() {
    myTagName = "TextureView";
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/android/view/TextureView.html"), isNull(), isNull());
  }

  public void testWebView() {
    myTagName = "WebView";
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/android/webkit/WebView.html"), isNull(), isNull());
  }

  public void testFullyQualifiedButton() {
    myTagName = "android.widget.Button";
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/android/widget/Button.html"), isNull(), isNull());
  }

  public void testSupportLibraryTag() {
    myTagName = SdkConstants.CONSTRAINT_LAYOUT.defaultName();
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/android/support/constraint/ConstraintLayout.html"), isNull(), isNull());
  }

  public void testGoogleLibraryTag() {
    myTagName = SdkConstants.AD_VIEW;
    myAction.actionPerformed(myEvent);
    verify(myBrowserLauncher).browse(eq("https://developer.android.com/reference/com/google/android/gms/ads/AdView.html"), isNull(), isNull());
  }
}
