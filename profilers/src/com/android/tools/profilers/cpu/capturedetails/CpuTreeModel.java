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
package com.android.tools.profilers.cpu.capturedetails;

import com.android.tools.adtui.model.AspectModel;
import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.adtui.model.Range;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.jetbrains.annotations.NotNull;

/**
 * The model for a JTree that updates for a given range. It uses a CpuTreeNode as it's backing tree.
 */
public abstract class CpuTreeModel<T extends CpuTreeNode<T>> extends DefaultTreeModel {
  public enum Aspect {
    // Tree Model changed
    TREE_MODEL
  }

  private final Range myRange;
  private final Range myCurrentRange;
  private final AspectObserver myAspectObserver;
  private final AspectModel<Aspect> myAspectModel;
  private final boolean myIsRootNodeIdValid;

  public CpuTreeModel(@NotNull Range range, @NotNull T node) {
    super(new DefaultMutableTreeNode(node));
    myIsRootNodeIdValid = !node.getId().isEmpty();
    myRange = range;
    myCurrentRange = new Range();
    myAspectModel = new AspectModel<>();
    myAspectObserver = new AspectObserver();
    myRange.addDependency(myAspectObserver).onChange(Range.Aspect.RANGE, this::rangeChanged);
    rangeChanged();
  }

  @NotNull
  public AspectModel<Aspect> getAspect() {
    return myAspectModel;
  }

  /**
   * @return True if the root node has a valid Id. Otherwise False.
   */
  public boolean isRootNodeIdValid() {
    return myIsRootNodeIdValid;
  }

  public void rangeChanged() {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)getRoot();

    List<Range> diffs = new LinkedList<>();
    // Add all the newly added ranges.
    diffs.addAll(myRange.subtract(myCurrentRange));
    // Add the ranges we don't have anymore
    diffs.addAll(myCurrentRange.subtract(myRange));

    update(root, myRange, diffs);

    myCurrentRange.set(myRange);
    myAspectModel.changed(Aspect.TREE_MODEL);
  }

  public boolean changes(T data, List<Range> ranges) {
    for (Range diff : ranges) {
      if (data.inRange(diff)) {
        return true;
      }
    }
    return false;
  }

  private void update(DefaultMutableTreeNode node, Range range, List<Range> ranges) {
    T data = (T)node.getUserObject();

    if (changes(data, ranges)) {
      Enumeration e = node.children();
      Map<T, DefaultMutableTreeNode> children = new HashMap<>();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
        children.put((T)child.getUserObject(), child);
      }
      Set<T> actual = new HashSet<>();
      for (T child : data.getChildren()) {
        if (child.inRange(range)) {
          actual.add(child);
          DefaultMutableTreeNode existing = children.get(child);
          if (existing == null) {
            existing = new DefaultMutableTreeNode(child);
            insertNodeInto(existing, node, node.getChildCount());
          }
          update(existing, range, ranges);
        } else {
          child.reset();
        }
      }
      for (Map.Entry<T, DefaultMutableTreeNode> entry : children.entrySet()) {
        if (!actual.contains(entry.getKey())) {
          removeNodeFromParent(entry.getValue());
        }
      }
      data.update(range);
      nodeChanged(node);
    }
  }

  @NotNull
  public Range getRange() {
    return myRange;
  }

  public boolean isEmpty() {
    T data = (T)((DefaultMutableTreeNode)getRoot()).getUserObject();
    return data.getGlobalTotal() == 0;
  }

  abstract void expand(@NotNull DefaultMutableTreeNode node);
}
