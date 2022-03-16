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

import static com.android.tools.adtui.common.AdtUiUtils.DEFAULT_TOP_BORDER;
import static com.android.tools.profilers.ProfilerLayout.ROW_HEIGHT_PADDING;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_CELL_INSETS;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_CELL_SPARKLINE_RIGHT_PADDING;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_CELL_SPARKLINE_TOP_BOTTOM_PADDING;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_HEADER_BORDER;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_RIGHT_ALIGNED_CELL_INSETS;
import static com.android.tools.profilers.ProfilerLayout.TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER;
import static com.android.tools.profilers.ProfilerLayout.TABLE_ROW_BORDER;

import com.android.tools.adtui.common.ColumnTreeBuilder;
import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.inspectors.common.api.stacktrace.CodeLocation;
import com.android.tools.inspectors.common.api.stacktrace.CodeNavigator;
import com.android.tools.profilers.ProfilerColors;
import com.android.tools.profilers.StudioProfilersView;
import com.android.tools.profilers.cpu.CaptureNode;
import com.android.tools.profilers.cpu.CpuCapture;
import com.android.tools.profilers.cpu.nodemodel.CaptureNodeModel;
import com.android.tools.profilers.cpu.nodemodel.CppFunctionModel;
import com.android.tools.profilers.cpu.nodemodel.JavaMethodModel;
import com.google.common.collect.ImmutableMap;
import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.tree.TreeModelAdapter;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A base view for {@link TopDownDetailsView} and {@link BottomUpDetailsView}.
 * They are almost similar except a few key differences, e.g bottom-up hides its root or lazy loads its children on expand.
 */
public abstract class TreeDetailsView<T extends CpuTreeNode<T>> extends CaptureDetailsView {
  private static final Comparator<DefaultMutableTreeNode> DEFAULT_SORT_ORDER =
    Collections.reverseOrder(new DoubleValueNodeComparator(CpuTreeNode::getGlobalTotal));

  @NotNull protected final JPanel myPanel;

  @SuppressWarnings("FieldCanBeLocal")
  @NotNull private final AspectObserver myObserver;
  @Nullable protected final JTree myTree;
  @Nullable private final CpuTraceTreeSorter mySorter;
  private final Set<TreePath> myExpandedPaths = new HashSet<>();

  protected TreeDetailsView(@NotNull StudioProfilersView profilersView,
                            @NotNull CpuCapture cpuCapture,
                            @Nullable CpuTreeModel<T> model) {
    super(profilersView);
    myObserver = new AspectObserver();
    if (model == null) {
      myPanel = getNoDataForThread();
      myTree = null;
      mySorter = null;
      return;
    }

    myPanel = new JPanel(new CardLayout());
    // Use JTree instead of IJ's tree, because IJ's tree does not happen border's Insets.
    //noinspection UndesirableClassUsage
    myTree = new JTree();
    int defaultFontHeight = myTree.getFontMetrics(myTree.getFont()).getHeight();
    myTree.setRowHeight(defaultFontHeight + ROW_HEIGHT_PADDING);
    myTree.setBorder(TABLE_ROW_BORDER);
    myTree.setModel(model);
    myTree.setRootVisible(model.isRootNodeIdValid());
    mySorter = new CpuTraceTreeSorter(myTree);
    mySorter.setModel(model, DEFAULT_SORT_ORDER);

    myPanel.add(createTableTree(), CARD_CONTENT);
    myPanel.add(getNoDataForRange(), CARD_EMPTY_INFO);

    CodeNavigator navigator = profilersView.getStudioProfilers().getIdeServices().getCodeNavigator();
    if (cpuCapture.getSystemTraceData() == null) {
      profilersView.getIdeProfilerComponents().createContextMenuInstaller()
        .installNavigationContextMenu(myTree, navigator, () -> getCodeLocation(myTree));
    }

    switchCardLayout(myPanel, model.isEmpty());

    // The structure of the tree changed, so sort with the previous sorting order.
    model.getAspect().addDependency(myObserver).onChange(CpuTreeModel.Aspect.TREE_MODEL, () -> {
      mySorter.sort();
      resetTreeExpansionState();
    });

    myTree.addTreeExpansionListener(new ExpansionListener());
  }

  /**
   * Helper function to load the tree expansion state from a set of cached TreePaths.
   * Tree paths are captured each time the tree is expanded / collapsed this includes programmatically.
   */
  private void resetTreeExpansionState() {
    // Grab a copy of the expanded paths because as we expand each path we modify this list directly.
    Set<TreePath> paths = new HashSet<>(myExpandedPaths);
    // Clear the global state since we have a copy of it that we will be enumerating.
    // This will be reset by the TreeExpansionListener.
    myExpandedPaths.clear();
    assert myTree != null; // Shouldn't be possible for the tree to be null at this point.
    paths.forEach(myTree::expandPath);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @NotNull
  private JComponent createTableTree() {
    assert myTree != null && mySorter != null;

    return new ColumnTreeBuilder(myTree)
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Name")
                   .setPreferredWidth(900)
                   .setMinWidth(160)
                   .setHeaderBorder(TABLE_COLUMN_HEADER_BORDER)
                   .setHeaderAlignment(SwingConstants.LEFT)
                   .setRenderer(new MethodNameRenderer())
                   .setComparator(new NameValueNodeComparator()))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Total (μs)")
                   .setPreferredWidth(100)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setMinWidth(80)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRendererWithSparkline(CpuTreeNode::getGlobalTotal, false, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getGlobalTotal)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("%")
                   .setPreferredWidth(60)
                   .setMinWidth(60)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRenderer(CpuTreeNode::getGlobalTotal, true, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getGlobalTotal)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Self (μs)")
                   .setPreferredWidth(100)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setMinWidth(80)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRenderer(CpuTreeNode::getSelf, false, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getSelf)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("%")
                   .setPreferredWidth(60)
                   .setMinWidth(60)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRenderer(CpuTreeNode::getSelf, true, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getSelf)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("Children (μs)")
                   .setPreferredWidth(100)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setMinWidth(80)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRenderer(CpuTreeNode::getGlobalChildrenTotal, false, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getGlobalChildrenTotal)))
      .addColumn(new ColumnTreeBuilder.ColumnBuilder()
                   .setName("%")
                   .setPreferredWidth(60)
                   .setMinWidth(60)
                   .setHeaderBorder(TABLE_COLUMN_RIGHT_ALIGNED_HEADER_BORDER)
                   .setHeaderAlignment(SwingConstants.RIGHT)
                   .setRenderer(new DoubleValueCellRenderer(CpuTreeNode::getGlobalChildrenTotal, true, SwingConstants.RIGHT))
                   .setSortOrderPreference(SortOrder.DESCENDING)
                   .setComparator(new DoubleValueNodeComparator(CpuTreeNode::getGlobalChildrenTotal)))
      .setTreeSorter(mySorter)
      .setBorder(DEFAULT_TOP_BORDER)
      .setBackground(ProfilerColors.DEFAULT_BACKGROUND)
      .setShowVerticalLines(true)
      .setTableIntercellSpacing(new Dimension())
      .build();
  }

  @Nullable
  private static CodeLocation getCodeLocation(@NotNull JTree tree) {
    if (tree.getSelectionPath() == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent();
    return modelToCodeLocation(((CpuTreeNode<?>)node.getUserObject()).getMethodModel());
  }

  private static CpuTreeNode getNode(Object value) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
    return (CpuTreeNode)node.getUserObject();
  }

  /**
   * Produces a {@link CodeLocation} corresponding to a {@link CaptureNodeModel}. Returns null if the model is not navigatable.
   */
  @Nullable
  private static CodeLocation modelToCodeLocation(CaptureNodeModel model) {
    if (model instanceof CppFunctionModel) {
      CppFunctionModel nativeFunction = (CppFunctionModel)model;
      return new CodeLocation.Builder(nativeFunction.getClassOrNamespace())
        .setMethodName(nativeFunction.getName())
        .setMethodParameters(nativeFunction.getParameters())
        .setNativeCode(true)
        .setFileName(nativeFunction.getFileName())
        .setNativeVAddress(nativeFunction.getVAddress())
        .build();
    }
    else if (model instanceof JavaMethodModel) {
      JavaMethodModel javaMethod = (JavaMethodModel)model;
      return new CodeLocation.Builder(javaMethod.getClassName())
        .setMethodName(javaMethod.getName())
        .setMethodSignature(javaMethod.getSignature())
        .setNativeCode(false)
        .build();
    }
    // Code is not navigatable.
    return null;
  }

  private static class NameValueNodeComparator implements Comparator<DefaultMutableTreeNode> {
    @Override
    public int compare(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {
      CpuTreeNode o1 = ((CpuTreeNode)a.getUserObject());
      CpuTreeNode o2 = ((CpuTreeNode)b.getUserObject());
      return o1.getMethodModel().getName().compareTo(o2.getMethodModel().getName());
    }
  }

  private static class DoubleValueNodeComparator implements Comparator<DefaultMutableTreeNode> {
    private final Function<CpuTreeNode, Double> myGetter;

    DoubleValueNodeComparator(Function<CpuTreeNode, Double> getter) {
      myGetter = getter;
    }

    @Override
    public int compare(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {
      CpuTreeNode o1 = ((CpuTreeNode)a.getUserObject());
      CpuTreeNode o2 = ((CpuTreeNode)b.getUserObject());
      return Double.compare(myGetter.apply(o1), myGetter.apply(o2));
    }
  }

  private static abstract class CpuCaptureCellRenderer extends ColoredTreeCellRenderer {

    private static final Map<CaptureNode.FilterType, SimpleTextAttributes> TEXT_ATTRIBUTES =
      ImmutableMap.<CaptureNode.FilterType, SimpleTextAttributes>builder()
        .put(CaptureNode.FilterType.MATCH, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        .put(CaptureNode.FilterType.EXACT_MATCH, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        .put(CaptureNode.FilterType.UNMATCH, SimpleTextAttributes.GRAY_ATTRIBUTES)
        .build();

    @NotNull
    protected SimpleTextAttributes getTextAttributes(@NotNull CpuTreeNode node) {
      return TEXT_ATTRIBUTES.get(node.getFilterType());
    }
  }

  private static class DoubleValueCellRenderer extends CpuCaptureCellRenderer {
    private final Function<CpuTreeNode, Double> myGetter;
    private final boolean myShowPercentage;
    private final int myAlignment;

    DoubleValueCellRenderer(Function<CpuTreeNode, Double> getter, boolean showPercentage, int alignment) {
      myGetter = getter;
      myShowPercentage = showPercentage;
      myAlignment = alignment;
      setTextAlign(alignment);
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      CpuTreeNode node = getNode(value);
      if (node != null) {
        SimpleTextAttributes attributes = getTextAttributes(node);
        double v = myGetter.apply(node);
        if (myShowPercentage) {
          CpuTreeNode root = getNode(tree.getModel().getRoot());
          append(String.format(Locale.getDefault(), "%.2f", v / root.getGlobalTotal() * 100), attributes);
        }
        else {
          append(String.format(Locale.getDefault(), "%,.0f", v), attributes);
        }
      }
      else {
        // TODO: We should improve the visual feedback when no data is available.
        append(value.toString());
      }
      if (myAlignment == SwingConstants.LEFT) {
        setIpad(TABLE_COLUMN_CELL_INSETS);
      }
      else {
        setIpad(TABLE_COLUMN_RIGHT_ALIGNED_CELL_INSETS);
      }
    }

    protected Function<CpuTreeNode, Double> getGetter() {
      return myGetter;
    }
  }

  private static class DoubleValueCellRendererWithSparkline extends DoubleValueCellRenderer {
    private Color mySparkLineColor;

    /**
     * Stores cell value divided by root cell total in order to compute the sparkline width.
     */
    private double myPercentage;

    DoubleValueCellRendererWithSparkline(Function<CpuTreeNode, Double> getter, boolean showPercentage, int alignment) {
      super(getter, showPercentage, alignment);
      mySparkLineColor = ProfilerColors.CAPTURE_SPARKLINE;
      myPercentage = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      CpuTreeNode node = getNode(value);
      if (node != null) {
        // We grab the global children total in the case of multi-select this value ends up being the sum of our childrens time
        // and what we want to display is what percentage of all our childrens time do we consume.
        myPercentage = getGetter().apply(node) / getNode(tree.getModel().getRoot()).getGlobalChildrenTotal();
      }
      mySparkLineColor = selected ? ProfilerColors.CAPTURE_SPARKLINE_SELECTED : ProfilerColors.CAPTURE_SPARKLINE;
    }

    @Override
    protected void paintComponent(Graphics g) {
      if (myPercentage > 0) {
        g.setColor(mySparkLineColor);
        // The sparkline aligns to the right of the cell and is proportional to the value, occupying the full cell.
        int sparkLineWidth = (int)(myPercentage * (getWidth() - TABLE_COLUMN_CELL_SPARKLINE_RIGHT_PADDING));
        g.fillRect(getWidth() - sparkLineWidth - TABLE_COLUMN_CELL_SPARKLINE_RIGHT_PADDING,
                   TABLE_COLUMN_CELL_SPARKLINE_TOP_BOTTOM_PADDING,
                   sparkLineWidth,
                   getHeight() - TABLE_COLUMN_CELL_SPARKLINE_TOP_BOTTOM_PADDING * 2);
      }
      super.paintComponent(g);
    }
  }

  private static class MethodNameRenderer extends CpuCaptureCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof DefaultMutableTreeNode &&
          ((DefaultMutableTreeNode)value).getUserObject() instanceof CpuTreeNode) {
        CpuTreeNode node = (CpuTreeNode)((DefaultMutableTreeNode)value).getUserObject();
        SimpleTextAttributes attributes = getTextAttributes(node);
        CaptureNodeModel model = node.getMethodModel();
        String classOrNamespace = "";
        if (model instanceof CppFunctionModel) {
          classOrNamespace = ((CppFunctionModel)model).getClassOrNamespace();
        }
        else if (model instanceof JavaMethodModel) {
          classOrNamespace = ((JavaMethodModel)model).getClassName();
        }

        if (model.getName().isEmpty()) {
          setIcon(AllIcons.Debugger.ThreadSuspended);
          append(classOrNamespace, attributes);
        }
        else {
          setIcon(PlatformIcons.METHOD_ICON);
          append(model.getName() + "()", attributes);
          append(" (" + classOrNamespace + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
      }
      else {
        append(value.toString());
      }
    }
  }

  public static class TopDownDetailsView extends TreeDetailsView<TopDownNode> {
    public TopDownDetailsView(@NotNull StudioProfilersView profilersView, @NotNull CaptureDetails.TopDown topDown) {
      super(profilersView, topDown.getCapture(), topDown.getModel());
      TopDownTreeModel model = topDown.getModel();
      if (model == null) {
        return;
      }
      assert myTree != null;

      expandTreeNodes(myTree);

      model.addTreeModelListener(new TreeModelAdapter() {
        @Override
        protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
          switchCardLayout(myPanel, model.isEmpty());
        }
      });
    }

    /**
     * Expands a few nodes in order to improve the visual feedback of the list.
     */
    private static void expandTreeNodes(JTree tree) {
      int maxRowsToExpand = 8; // TODO: adjust this value if necessary.
      int i = 0;
      while (i < tree.getRowCount() && i < maxRowsToExpand) {
        tree.expandRow(i++);
      }
    }
  }

  public static class BottomUpDetailsView extends TreeDetailsView<BottomUpNode> {
    public BottomUpDetailsView(@NotNull StudioProfilersView profilersView, @NotNull CaptureDetails.BottomUp bottomUp) {
      super(profilersView, bottomUp.getCapture(), bottomUp.getModel());
      BottomUpTreeModel model = bottomUp.getModel();
      if (model == null) {
        return;
      }
      assert myTree != null;

      myTree.setRootVisible(false);
      myTree.addTreeWillExpandListener(new TreeWillExpandListener() {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
          ((BottomUpTreeModel)myTree.getModel()).expand(node);
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) {
        }
      });

      model.addTreeModelListener(new TreeModelAdapter() {
        @Override
        protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
          // When the root loses all of its children it can't be expanded and when they're added it is still collapsed.
          // As a result, nothing will be visible as the root itself isn't visible. So, expand it if it's the case.
          if (type == EventType.NodesInserted && event.getTreePath().getPathCount() == 1) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
            Object[] inserted = event.getChildren();
            if (inserted != null && inserted.length == root.getChildCount()) {
              myTree.expandPath(new TreePath(root));
            }
          }
          switchCardLayout(myPanel, model.isEmpty());
        }
      });
    }
  }

  private class ExpansionListener implements TreeExpansionListener {
    /**
     * Set to hold the paths of previously expanded children that are now hidden due to a collapsed parent.
     * This set allows us to handle expanding/collapsing a parent (or grandparent) but maintain the hidden elements state.
     * Eg.
     * A
     *  -> B
     *     -> C
     * When A is collapsed the path for B, and C are saved in this set and removed from the expanded paths set. This allows us to restore
     * the state of B and C when A is expanded again.
     */
    private final Set<TreePath> myCollapsedParentChildExpandedPaths = new HashSet<>();

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
      TreePath toBeExpandedPath = event.getPath();
      Set<TreePath> expandedChildren = new HashSet<>();
      expandedChildren.add(toBeExpandedPath);
      // Find cached children paths under this newly expanded node and reset their state.
      myCollapsedParentChildExpandedPaths.forEach(path -> {
        // We only want paths that are a child of our new path.
        // Specifically we only want paths that are direct children, this function will be called recursively
        // as we expand TreePaths in the expandedChildren set.
        // Note: x.isDescendant(y) is backwards from how you may expect. It really means if y is a descendant of x.
        if (toBeExpandedPath.isDescendant(path) && path.getParentPath().equals(toBeExpandedPath)) {
          expandedChildren.add(path);
        }
      });
      myCollapsedParentChildExpandedPaths.removeAll(expandedChildren);
      myExpandedPaths.addAll(expandedChildren);
      // Expand any children that were previously expanded under our newly expanded node. This forces a recursive call to treeExpanded.
      expandedChildren.forEach(myTree::expandPath);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
      TreePath toBeCollapsedPath = event.getPath();
      Set<TreePath> childExpandedPaths = new HashSet<>();
      childExpandedPaths.add(toBeCollapsedPath);
      // Cache off the state of all children under the newly collapsed node.
      myExpandedPaths.forEach(path -> {
        if (toBeCollapsedPath.isDescendant(path)) {
          childExpandedPaths.add(path);
        }
      });
      myExpandedPaths.removeAll(childExpandedPaths);
      myCollapsedParentChildExpandedPaths.addAll(childExpandedPaths);
    }
  }
}
