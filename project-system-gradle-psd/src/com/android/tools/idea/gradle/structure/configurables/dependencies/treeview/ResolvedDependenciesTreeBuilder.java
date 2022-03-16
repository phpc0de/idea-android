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
package com.android.tools.idea.gradle.structure.configurables.dependencies.treeview;

import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings;
import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractBaseTreeBuilder;
import com.android.tools.idea.gradle.structure.model.PsModule;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import org.jetbrains.annotations.NotNull;

public class ResolvedDependenciesTreeBuilder extends AbstractBaseTreeBuilder {
  public ResolvedDependenciesTreeBuilder(@NotNull PsModule module,
                                         @NotNull JTree tree,
                                         @NotNull DefaultTreeModel treeModel,
                                         @NotNull PsUISettings uiSettings) {
    super(tree, treeModel, new ResolvedDependenciesTreeStructure(module, uiSettings));
  }

  public void reset() {
    AbstractTreeStructure treeStructure = getTreeStructure();
    if (treeStructure instanceof ResolvedDependenciesTreeStructure) {
      ((ResolvedDependenciesTreeStructure)treeStructure).reset();
    }
  }
}
