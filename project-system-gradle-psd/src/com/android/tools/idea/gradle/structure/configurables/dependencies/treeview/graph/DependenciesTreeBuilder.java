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
package com.android.tools.idea.gradle.structure.configurables.dependencies.treeview.graph;

import com.android.tools.idea.gradle.structure.configurables.dependencies.treeview.AbstractDependencyNode;
import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractBaseTreeBuilder;
import com.android.tools.idea.gradle.structure.model.PsBaseDependency;
import com.android.tools.idea.gradle.structure.model.PsDeclaredDependency;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.util.ActionCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class DependenciesTreeBuilder extends AbstractBaseTreeBuilder {
  public DependenciesTreeBuilder(@NotNull JTree tree,
                                 @NotNull DefaultTreeModel treeModel,
                                 @NotNull DependenciesTreeStructure treeStructure) {
    super(tree, treeModel, treeStructure);
  }

  public void reset(@Nullable Runnable onDone) {
    AbstractTreeStructure treeStructure = getTreeStructure();
    if (treeStructure instanceof DependenciesTreeStructure) {
      ((DependenciesTreeStructure)treeStructure).reset();
      ActionCallback callback = queueUpdate();
      if (onDone != null) {
        callback.doWhenDone(onDone);
      }
    }
  }

  @Nullable
  public AbstractDependencyNode findDeclaredDependency(@NotNull PsDeclaredDependency dependency) {
    DefaultMutableTreeNode rootNode = getRootNode();
    if (rootNode == null) {
      return null;
    }
    int childCount = rootNode.getChildCount();
    for (int i = 0; i < childCount; i++) {
      TreeNode child = rootNode.getChildAt(i);
      if (!(child instanceof DefaultMutableTreeNode)) {
        continue;
      }
      Object userObject = ((DefaultMutableTreeNode)child).getUserObject();
      if (!(userObject instanceof AbstractDependencyNode)) {
        continue;
      }
      AbstractDependencyNode<?, PsBaseDependency> node = (AbstractDependencyNode)userObject;
      for (PsBaseDependency model : node.getModels()) {
        // Checking for reference equality since declared dependencies are always reloaded.
        if (dependency == model) {
          return node;
        }
      }
    }
    return null;
  }
}
