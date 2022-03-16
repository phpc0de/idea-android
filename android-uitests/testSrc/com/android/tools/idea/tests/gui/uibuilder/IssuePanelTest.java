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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.RunIn;
import com.android.tools.idea.tests.gui.framework.TestGroup;
import com.android.tools.idea.tests.gui.framework.fixture.ChooseClassDialogFixture;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.MessagesFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlComponentFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlEditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.layout.IssuePanelFixture;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiTestRemoteRunner.class)
public class IssuePanelTest {

  @Rule public final GuiTestRule myGuiTest = new GuiTestRule();
  @Rule public final RenderTaskLeakCheckRule renderTaskLeakCheckRule = new RenderTaskLeakCheckRule();

  /**
   * Scenario:
   * - Open layout with TextView
   * - Open the IssuePanel
   * - Check that it has no issue by checking the title
   * - Select the textView
   * - Focus on the "text" property in the propertyPanel
   * - Enter the text "b"
   * - Ensure that a lint warning has been created for the hardcoded text
   * - select the issue in the IssuePanel
   * - click the fix button
   * - check that the issue is gone by checking the title and the JLabel of the issue
   */
  @RunIn(TestGroup.UNRELIABLE)
  @Test
  public void openIssuePanel() throws IOException {

    EditorFixture editor = myGuiTest.importProjectAndWaitForProjectSyncToFinish("LayoutTest")
      .getEditor()
      .open("app/src/main/res/layout/activity_my.xml", EditorFixture.Tab.DESIGN);
    NlEditorFixture layoutEditor = editor.getLayoutEditor();

    NlComponentFixture textView = layoutEditor
      .showOnlyDesignView()
      .findView("TextView", 0);
    textView.getSceneComponent().click();

    layoutEditor.waitForRenderToFinish();
    IssuePanelFixture panelFixture = layoutEditor.getIssuePanel();
    layoutEditor.getRhsConfigToolbar().clickIssuePanelButton();
    panelFixture.requireVisible();
    assertEquals("No issues", panelFixture.getTitle());

    layoutEditor.getAttributesPanel()
      .waitForId("<unnamed>")
      .findSectionByName("Common Attributes")
      .findEditorOf("text")
      .getTextField()
      .selectAll()
      .enterText("Hello My World!");

    textView.getSceneComponent().click();

    layoutEditor.waitForRenderToFinish();
    assertEquals("1 Warning", panelFixture.getTitle().trim());

    String hardcodedTextErrorTitle = "Hardcoded text";
    GuiTests.waitUntilShowing(myGuiTest.robot(), panelFixture.target(),
                              Matchers.byText(JLabel.class, hardcodedTextErrorTitle));
    panelFixture.findIssueWithTitle(hardcodedTextErrorTitle).doubleClick();
    panelFixture.clickFixButton();

    panelFixture.dialog().button(Matchers.byText(JButton.class, "OK")).click();
    layoutEditor.waitForRenderToFinish();
    GuiTests.waitUntilGone(myGuiTest.robot(), panelFixture.target(),
                           Matchers.byText(JLabel.class, hardcodedTextErrorTitle));
    assertEquals("No issues", panelFixture.getTitle());
  }

  @Test
  public void testFixMissingFragmentNameWithoutCustomFragmentsAvailable() throws Exception {
    myGuiTest.importSimpleApplication();

    // Open file as XML and switch to design tab, wait for successful render
    EditorFixture editor = myGuiTest.ideFrame().getEditor();
    editor.open("app/src/main/res/layout/activity_my.xml", EditorFixture.Tab.EDITOR);
    editor.moveBetween(">\n", "\n</RelativeLayout");
    editor.enterText("<fragment android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>\n");
    editor.switchToTab("Design");

    NlEditorFixture layout = editor.getLayoutEditor();
    layout.waitForRenderToFinish();
    layout.getRhsConfigToolbar().clickIssuePanelButton();
    IssuePanelFixture panelFixture = layout.getIssuePanel();
    layout.enlargeBottomComponentSplitter();
    String errorTitle = "Unknown fragments";
    GuiTests.waitUntilShowing(myGuiTest.robot(), panelFixture.target(), Matchers.byText(JLabel.class, errorTitle));
    panelFixture.findIssueWithTitle(errorTitle).doubleClick();
    panelFixture.clickOnLink("Choose Fragment Class...");

    MessagesFixture classesDialog = MessagesFixture.findByTitle(myGuiTest.robot(), "No Fragments Found");
    classesDialog.requireMessageContains("You must first create one or more Fragments in code");
    classesDialog.clickOk();
  }

  @Test
  public void testFixMissingFragmentNameWithCustomFragmentsAvailable() throws Exception {
    myGuiTest.importProjectAndWaitForProjectSyncToFinish("FragmentApplication");

    EditorFixture editor = myGuiTest.ideFrame().getEditor();
    editor.open("app/src/main/res/layout/fragment_empty.xml", EditorFixture.Tab.DESIGN);

    NlEditorFixture layout = editor.getLayoutEditor();
    layout.waitForRenderToFinish();
    layout.getRhsConfigToolbar().clickIssuePanelButton();
    IssuePanelFixture panelFixture = layout.getIssuePanel();
    layout.enlargeBottomComponentSplitter();
    String errorTitle = "Unknown fragments";
    GuiTests.waitUntilShowing(myGuiTest.robot(), panelFixture.target(), Matchers.byText(JLabel.class, errorTitle));
    panelFixture.findIssueWithTitle(errorTitle).doubleClick();
    panelFixture.clickOnLink("Choose Fragment Class...");

    ChooseClassDialogFixture dialog = ChooseClassDialogFixture.find(myGuiTest.ideFrame());
    assertThat(dialog.getTitle()).isEqualTo("Fragments");
    assertThat(dialog.getList().contents().length).isEqualTo(2);

    dialog.getList().selectItem("PsiClass:YourCustomFragment");
    dialog.clickOk();

    editor.switchToTab("Text");
    String content = editor.getCurrentFileContents();
    assertThat(content).contains("<fragment android:layout_width=\"match_parent\"\n" +
                                 "            android:layout_height=\"match_parent\"\n" +
                                 "      android:name=\"google.fragmentapplication.YourCustomFragment\" />");
  }
}
