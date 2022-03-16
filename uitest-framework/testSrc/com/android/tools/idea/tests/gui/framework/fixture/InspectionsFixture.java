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
import com.intellij.codeInspection.ui.InspectionTree;
import com.intellij.codeInspection.ui.InspectionTreeNode;
import com.intellij.openapi.wm.ToolWindowId;
import java.util.Collections;
import java.util.List;
import org.fest.swing.edt.GuiQuery;
import org.jetbrains.annotations.NotNull;

/**
 * Fixture for the Inspections window in the IDE
 */
public class InspectionsFixture extends ToolWindowFixture {

  @NotNull
  static InspectionsFixture find(IdeFrameFixture ideFrameFixture) {
    InspectionTree tree = GuiTests.waitUntilFound(ideFrameFixture.robot(), null, Matchers.byType(InspectionTree.class), 30);
    return new InspectionsFixture(ideFrameFixture, tree);
  }

  private final InspectionTree myTree;

  private InspectionsFixture(IdeFrameFixture ideFrameFixture, InspectionTree tree) {
    super(ToolWindowId.INSPECTION, ideFrameFixture.getProject(), ideFrameFixture.robot());
    myTree = tree;
  }

  @NotNull
  public String getResults() {
    activate();
    waitUntilIsVisible();
    GuiTests.waitForBackgroundTasks(myRobot);

    return GuiQuery.getNonNull(
      () -> {
        StringBuilder sb = new StringBuilder();
        InspectionsFixture.describe(myTree.getRoot(), sb, 0);
        return sb.toString();
      });
  }

  private static void describe(@NotNull InspectionTreeNode node, @NotNull StringBuilder sb, int depth) {
    for (int i = 0; i < depth; i++) {
      sb.append("    ");
    }
    sb.append(node.toString());
    sb.append("\n");

    // The exact order of the results sometimes varies so sort the children alphabetically
    // instead to ensure stable test output
    List<InspectionTreeNode> children = new ArrayList<>(node.getChildCount());
    for (int i = 0, n = node.getChildCount(); i < n; i++) {
      children.add((InspectionTreeNode)node.getChildAt(i));
    }
    Collections.sort(children, (node1, node2) -> node1.toString().compareTo(node2.toString()));
    for (InspectionTreeNode child : children) {
      describe(child, sb, depth + 1);
    }
  }
}
