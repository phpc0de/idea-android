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

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture.EditorAction;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture.Tab;
import com.android.tools.idea.tests.gui.framework.fixture.MessagesFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlComponentFixture;
import com.android.tools.idea.tests.gui.framework.fixture.designer.NlEditorFixture;
import com.android.tools.idea.tests.util.WizardUtils;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.fest.swing.fixture.JListFixture;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.awt.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

@RunWith(GuiTestRemoteRunner.class)
public final class MenuTest {
  @Language("XML")
  @SuppressWarnings("XmlUnusedNamespaceDeclaration")
  private static final String MENU_MAIN_XML_CONTENTS = "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                                                       "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                                                       "    xmlns:tools=\"http://schemas.android.com/tools\">\n" +
                                                       "    <item\n" +
                                                       "        android:id=\"@+id/action_settings\"\n" +
                                                       "        android:title=\"@string/action_settings\"\n" +
                                                       "        app:showAsAction=\"always\" />\n" +
                                                       "</menu>\n";

  private static final Path MENU_MAIN_XML_RELATIVE_PATH =
    FileSystems.getDefault().getPath("app", "src", "main", "res", "menu", "menu_main.xml");

  private static final Path IC_SEARCH_BLACK_24DP_XML_RELATIVE_PATH =
    FileSystems.getDefault().getPath("app", "src", "main", "res", "drawable", "ic_search_black_24dp.xml");

  @Rule
  public final GuiTestRule myGuiTest = new GuiTestRule();
  @Rule
  public final RenderTaskLeakCheckRule renderTaskLeakCheckRule = new RenderTaskLeakCheckRule();

  private Path myIcSearchBlack24dpXmlAbsolutePath;

  private EditorFixture myEditor;

  @Before
  public void setUp() {
    WizardUtils.createNewProject(myGuiTest, "Basic Activity");

    Path path = myGuiTest.getProjectPath().toPath();
    myIcSearchBlack24dpXmlAbsolutePath = path.resolve(IC_SEARCH_BLACK_24DP_XML_RELATIVE_PATH);

    myEditor = myGuiTest.ideFrame().getEditor();
  }

  @Test
  public void dragCastButtonIntoActionBar() {
    NlComponentFixture settingsItem = myEditor.open(MENU_MAIN_XML_RELATIVE_PATH)
      .replaceText(MENU_MAIN_XML_CONTENTS)
      .getLayoutEditor()
      .waitForRenderToFinish()
      .findView("item", 0);
    dragAndDrop("Cast Button", settingsItem.getSceneComponent().getLeftCenterPoint());
    myGuiTest.ideFrame().actAndWaitForGradleProjectSyncToFinish(
      it ->
        MessagesFixture.findByTitle(myGuiTest.robot(), "Add Project Dependency")
          .clickOk());

    myEditor.open(MENU_MAIN_XML_RELATIVE_PATH, Tab.EDITOR);

    @Language("XML")
    String expected = "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                      "    xmlns:tools=\"http://schemas.android.com/tools\">\n" +
                      "    <item\n" +
                      "        android:id=\"@+id/media_route_menu_item\"\n" +
                      "        android:title=\"Cast\"\n" +
                      "        app:actionProviderClass=\"androidx.mediarouter.app.MediaRouteActionProvider\"\n" +
                      "        app:showAsAction=\"always\"\n" +
                      "        tools:icon=\"@drawable/mr_button_light\" />\n" +
                      "    <item\n" +
                      "        android:id=\"@+id/action_settings\"\n" +
                      "        android:title=\"@string/action_settings\"\n" +
                      "        app:showAsAction=\"always\" />\n" +
                      "</menu>\n";

    assertEquals(expected, myEditor.getCurrentFileContents());
  }

  @Test
  public void dragMenuItemIntoActionBar() {
    NlComponentFixture settingsItem = myEditor.open(MENU_MAIN_XML_RELATIVE_PATH)
      .getLayoutEditor()
      .waitForRenderToFinish()
      .findView("item", 0);
    dragAndDrop("Menu Item", settingsItem.getSceneComponent().getTopCenterPoint());
    myEditor.open(MENU_MAIN_XML_RELATIVE_PATH, Tab.EDITOR);

    @Language("XML")
    String expected = "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                      "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                      "    tools:context=\"com.google.myapplication.MainActivity\">\n" +
                      "    <item\n" +
                      "        android:orderInCategory=\"100\"\n" +
                      "        android:title=\"Item\" />\n" +
                      "    <item\n" +
                      "        android:id=\"@+id/action_settings\"\n" +
                      "        android:orderInCategory=\"101\"\n" +
                      "        android:title=\"@string/action_settings\"\n" +
                      "        app:showAsAction=\"never\" />\n" +
                      "</menu>";

    assertEquals(expected, myEditor.getCurrentFileContents().trim());
  }

  @Test
  public void dragSearchItemIntoActionBar() {
    NlComponentFixture settingsItem = myEditor.open(MENU_MAIN_XML_RELATIVE_PATH)
      .replaceText(MENU_MAIN_XML_CONTENTS)
      .getLayoutEditor()
      .waitForRenderToFinish()
      .findView("item", 0);
    dragAndDrop("Search Item", settingsItem.getSceneComponent().getLeftCenterPoint());
    myEditor.open(MENU_MAIN_XML_RELATIVE_PATH, Tab.EDITOR);

    @Language("XML")
    @SuppressWarnings("XmlUnusedNamespaceDeclaration")
    String expected = "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                      "    xmlns:tools=\"http://schemas.android.com/tools\">\n" +
                      "    <item\n" +
                      "        android:id=\"@+id/app_bar_search\"\n" +
                      "        android:icon=\"@drawable/ic_search_black_24dp\"\n" +
                      "        android:title=\"Search\"\n" +
                      "        app:actionViewClass=\"android.widget.SearchView\"\n" +
                      "        app:showAsAction=\"always\" />\n" +
                      "    <item\n" +
                      "        android:id=\"@+id/action_settings\"\n" +
                      "        android:title=\"@string/action_settings\"\n" +
                      "        app:showAsAction=\"always\" />\n" +
                      "</menu>\n";

    assertEquals(expected, myEditor.getCurrentFileContents());
    assertTrue(Files.exists(myIcSearchBlack24dpXmlAbsolutePath));

    myEditor.invokeAction(EditorAction.UNDO);

    assertEquals(MENU_MAIN_XML_CONTENTS, myEditor.getCurrentFileContents());
    assertFalse(Files.exists(myIcSearchBlack24dpXmlAbsolutePath));
  }

  private void dragAndDrop(@NotNull String item, @NotNull Point point) {
    NlEditorFixture editor = myEditor.getLayoutEditor();
    editor.waitForRenderToFinish();

    JListFixture list = editor.getPalette().getItemList("");
    list.replaceCellReader(new ItemTitleListCellReader());
    list.drag(item);

    editor.getSurface().drop(point);
  }
}