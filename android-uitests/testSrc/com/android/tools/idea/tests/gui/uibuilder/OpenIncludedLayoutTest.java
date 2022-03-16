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
package com.android.tools.idea.tests.gui.uibuilder;

import com.android.SdkConstants;
import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlEditorFixture;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.fest.swing.fixture.JTreeFixture;
import org.fest.swing.timing.Wait;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * UI test for the layout preview window
 */
@RunWith(GuiTestRemoteRunner.class)
public class OpenIncludedLayoutTest {

  public static final String INCLUDED_XML = "inner.xml";
  public static final String OUTER_XML = "app/src/main/res/layout/outer.xml";
  @Rule public final GuiTestRule guiTest = new GuiTestRule();
  @Rule public final RenderTaskLeakCheckRule renderTaskLeakCheckRule = new RenderTaskLeakCheckRule();

  @Test
  public void testOpenIncludedLayoutFromComponentTree() throws Exception {
    guiTest.importProjectAndWaitForProjectSyncToFinish("LayoutTest");
    IdeFrameFixture ideFrame = guiTest.ideFrame();
    EditorFixture editor = ideFrame.getEditor()
      .open(OUTER_XML, EditorFixture.Tab.DESIGN);

    NlEditorFixture layoutEditor = editor.getLayoutEditor();
    layoutEditor.waitForRenderToFinish();
    JTreeFixture tree = layoutEditor.getComponentTree();
    tree.click();
    tree.requireFocused();
    tree.doubleClickPath("LinearLayout/include");
    assertEquals(INCLUDED_XML, editor.getCurrentFileName());

    layoutEditor.getBackNavigationPanel().click();
    Wait.seconds(2).expecting("outer.xml to be selected").until(() -> "outer.xml".equals(editor.getCurrentFileName()));
  }

  @Test
  public void testOpenIncludedLayoutFromEditor() throws Exception {
    guiTest.importProjectAndWaitForProjectSyncToFinish("LayoutTest");
    IdeFrameFixture ideFrame = guiTest.ideFrame();
    EditorFixture editor = ideFrame.getEditor()
      .open(OUTER_XML, EditorFixture.Tab.DESIGN);

    NlEditorFixture layoutEditor = editor.getLayoutEditor();
    layoutEditor.waitForRenderToFinish();
    layoutEditor.getAllComponents()
      .stream()
      .filter(fixture -> fixture.getComponent().getTagName().equalsIgnoreCase(SdkConstants.VIEW_INCLUDE))
      .findFirst().get().getSceneComponent().doubleClick();
    assertEquals(INCLUDED_XML, editor.getCurrentFileName());
  }
}
