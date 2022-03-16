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
package com.android.tools.profilers.network;

import static com.android.tools.profilers.ProfilerLayout.TOOLTIP_BORDER;

import com.android.tools.adtui.TabularLayout;
import com.android.tools.adtui.TooltipComponent;
import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.formatter.TimeFormatter;
import com.android.tools.adtui.stdui.BorderlessTableCellRenderer;
import com.android.tools.adtui.stdui.TimelineTable;
import com.android.tools.profilers.ProfilerColors;
import com.android.tools.profilers.ProfilerFonts;
import com.android.tools.profilers.network.httpdata.HttpData;
import com.google.common.collect.ImmutableMap;
import com.intellij.util.ui.JBUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Displays network connection information of all threads.
 */
final class ThreadsView {
  private static final int STATE_HEIGHT = JBUI.scale(15);
  private static final int SELECTION_OUTLINE_PADDING = JBUI.scale(3);
  private static final int SELECTION_OUTLINE_BORDER = JBUI.scale(2);
  private static final int ROW_HEIGHT = STATE_HEIGHT + 2 * (SELECTION_OUTLINE_BORDER + SELECTION_OUTLINE_PADDING);

  private enum Column {
    NAME,
    TIMELINE
  }

  @NotNull
  private final JTable myThreadsTable;

  @NotNull
  private final AspectObserver myObserver;

  ThreadsView(@NotNull NetworkProfilerStageView stageView) {
    ThreadsTableModel model = new ThreadsTableModel(stageView.getStage().getHttpDataFetcher());
    myThreadsTable = TimelineTable.create(model, stageView.getStage().getTimeline(), Column.TIMELINE.ordinal());
    TimelineRenderer timelineRenderer = new TimelineRenderer(myThreadsTable, stageView.getStage());

    myThreadsTable.getColumnModel().getColumn(Column.NAME.ordinal()).setCellRenderer(new BorderlessTableCellRenderer());
    myThreadsTable.getColumnModel().getColumn(Column.TIMELINE.ordinal()).setCellRenderer(timelineRenderer);
    myThreadsTable.setBackground(ProfilerColors.DEFAULT_BACKGROUND);
    myThreadsTable.setShowVerticalLines(true);
    myThreadsTable.setShowHorizontalLines(false);
    myThreadsTable.setCellSelectionEnabled(false);
    myThreadsTable.setFocusable(false);
    myThreadsTable.setRowMargin(0);
    myThreadsTable.setRowHeight(ROW_HEIGHT);
    myThreadsTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
    myThreadsTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    myThreadsTable.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        myThreadsTable.getColumnModel().getColumn(Column.NAME.ordinal()).setPreferredWidth((int)(myThreadsTable.getWidth() * 1.0 / 8));
        myThreadsTable.getColumnModel().getColumn(Column.TIMELINE.ordinal()).setPreferredWidth((int)(myThreadsTable.getWidth() * 7.0 / 8));
      }
    });

    TableRowSorter<ThreadsTableModel> sorter = new TableRowSorter<>(model);
    sorter.setComparator(Column.NAME.ordinal(), Comparator.comparing(String::toString));
    sorter.setComparator(Column.TIMELINE.ordinal(), Comparator.comparing((List<HttpData> data) -> data.get(0).getRequestStartTimeUs()));
    myThreadsTable.setRowSorter(sorter);

    myThreadsTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Range selection = stageView.getStage().getTimeline().getSelectionRange();
        HttpData data = findHttpDataUnderCursor(myThreadsTable, selection, e);
        if (data != null) {
          stageView.getStage().setSelectedConnection(data);
          e.consume();
        }
      }
    });

    ThreadsView.TooltipView.install(myThreadsTable, stageView);

    myObserver = new AspectObserver();
    stageView.getStage().getAspect().addDependency(myObserver)
      .onChange(NetworkProfilerAspect.SELECTED_CONNECTION, () -> {
        timelineRenderer.updateRows();
        myThreadsTable.repaint();
      });
  }

  @NotNull
  JComponent getComponent() {
    return myThreadsTable;
  }

  @Nullable
  private static HttpData findHttpDataUnderCursor(@NotNull JTable table, @NotNull Range range, @NotNull MouseEvent e) {
    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), table);
    int row = table.rowAtPoint(p);
    int column = table.columnAtPoint(p);

    if (row == -1 || column == -1) {
      return null;
    }

    if (column == Column.TIMELINE.ordinal()) {
      Rectangle cellBounds = table.getCellRect(row, column, false);
      int modelIndex = table.convertRowIndexToModel(row);
      List<HttpData> dataList = (List<HttpData>)table.getModel().getValueAt(modelIndex, 1);
      double at = positionToRange(p.x - cellBounds.x, cellBounds.getWidth(), range);
      for (HttpData data : dataList) {
        if (data.getRequestStartTimeUs() <= at && at <= data.getConnectionEndTimeUs()) {
          return data;
        }
      }
    }

    return null;
  }

  private static double positionToRange(double x, double width, @NotNull Range range) {
    return (x * range.getLength()) / width + range.getMin();
  }

  private static final class ThreadsTableModel extends AbstractTableModel {
    @NotNull private final List<List<HttpData>> myThreads;

    private ThreadsTableModel(@NotNull HttpDataFetcher httpDataFetcher) {
      myThreads = new ArrayList<>();
      httpDataFetcher.addListener(this::httpDataChanged);
    }

    private void httpDataChanged(List<HttpData> dataList) {
      myThreads.clear();
      if (dataList.isEmpty()) {
        fireTableDataChanged();
        return;
      }

      Map<Long, List<HttpData>> threads = new HashMap<>();
      for (HttpData data : dataList) {
        if (data.getJavaThreads().isEmpty()) {
          continue;
        }
        if (!threads.containsKey(data.getJavaThreads().get(0).getId())) {
          threads.put(data.getJavaThreads().get(0).getId(), new ArrayList<>());
        }
        threads.get(data.getJavaThreads().get(0).getId()).add(data);
      }

      // Sort by thread name, so that they're consistently displayed in alphabetical order.
      // TODO: Implement sorting mechanism in JList and move this responsibility to the JList.
      threads.values().stream().sorted((o1, o2) -> {
        HttpData.JavaThread thread1 = o1.get(0).getJavaThreads().get(0);
        HttpData.JavaThread thread2 = o2.get(0).getJavaThreads().get(0);
        int nameCompare = thread1.getName().compareTo(thread2.getName());
        return (nameCompare != 0) ? nameCompare : Long.compare(thread1.getId(), thread2.getId());
      }).forEach(myThreads::add);

      fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
      return myThreads.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int column) {
      return column == Column.NAME.ordinal() ? "Initiating thread" : "Timeline";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == Column.NAME.ordinal()) {
        return myThreads.get(rowIndex).get(0).getJavaThreads().get(0).getName();
      }
      else {
        return myThreads.get(rowIndex);
      }
    }
  }

  private static final class TimelineRenderer extends TimelineTable.CellRenderer implements TableModelListener {
    @NotNull private final JTable myTable;
    @NotNull private final List<JComponent> myConnectionsInfo;
    @NotNull private final NetworkProfilerStage myStage;

    TimelineRenderer(@NotNull JTable table, @NotNull NetworkProfilerStage stage) {
      super(stage.getTimeline());
      myTable = table;
      myConnectionsInfo = new ArrayList<>();
      myStage = stage;
      myTable.getModel().addTableModelListener(this);
      tableChanged(new TableModelEvent(myTable.getModel()));
    }

    @Override
    public void tableChanged(TableModelEvent e) {
      updateRows();
    }

    private void updateRows() {
      myConnectionsInfo.clear();
      for (int index = 0; index < myTable.getModel().getRowCount(); ++index) {
        List<HttpData> data = (List<HttpData>)myTable.getModel().getValueAt(index, 1);
        myConnectionsInfo.add(new ConnectionsInfoComponent(myTable, data, myStage));
      }
    }

    @NotNull
    @Override
    protected Component getTableCellRendererComponent(boolean isSelected, int row) {
      return myConnectionsInfo.get(myTable.convertRowIndexToModel(row));
    }
  }

  /**
   * A component that responsible for rendering information of the given connections,
   * such as connection names, warnings, and lifecycle states.
   */
  private static final class ConnectionsInfoComponent extends JComponent {
    private static final int NAME_PADDING = 6;

    @NotNull private final List<HttpData> myDataList;
    @NotNull private final Range myRange;
    @NotNull private final JTable myTable;
    @NotNull private final NetworkProfilerStage myStage;

    private ConnectionsInfoComponent(@NotNull JTable table, @NotNull List<HttpData> data, @NotNull NetworkProfilerStage stage) {
      myStage = stage;
      myDataList = data;
      myRange = stage.getTimeline().getSelectionRange();
      setFont(ProfilerFonts.SMALL_FONT);
      setForeground(Color.BLACK);
      setBackground(ProfilerColors.DEFAULT_BACKGROUND);
      myTable = table;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D)g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      for (int i = 0; i < myDataList.size(); ++i) {
        HttpData data = myDataList.get(i);
        double endLimit = (i + 1 < myDataList.size()) ? rangeToPosition(myDataList.get(i + 1).getRequestStartTimeUs()) : getWidth();

        drawState(g2d, data, endLimit);
        drawConnectionName(g2d, data, endLimit);
      }

      if (myStage.getSelectedConnection() != null && myDataList.contains(myStage.getSelectedConnection())) {
        drawSelection(g2d, myStage.getSelectedConnection(), getWidth());
      }

      g2d.dispose();
    }

    private void drawState(@NotNull Graphics2D g2d, @NotNull HttpData data, double endLimit) {
      double prev = rangeToPosition(data.getRequestStartTimeUs());
      g2d.setColor(ProfilerColors.NETWORK_SENDING_COLOR);

      if (data.getResponseStartTimeUs() > 0) {
        double download = rangeToPosition(data.getResponseStartTimeUs());
        // draw sending
        g2d.fill(new Rectangle2D.Double(prev, (getHeight() - STATE_HEIGHT) / 2.0, download - prev, STATE_HEIGHT));
        g2d.setColor(ProfilerColors.NETWORK_RECEIVING_COLOR);
        prev = download;
      }

      double end = (data.getConnectionEndTimeUs() > 0) ? rangeToPosition(data.getConnectionEndTimeUs()) : endLimit;
      g2d.fill(new Rectangle2D.Double(prev, (getHeight() - STATE_HEIGHT) / 2.0, end - prev, STATE_HEIGHT));
    }

    private void drawConnectionName(@NotNull Graphics2D g2d, @NotNull HttpData data, double endLimit) {
      g2d.setFont(getFont());
      g2d.setColor(getForeground());
      double start = rangeToPosition(data.getRequestStartTimeUs());
      double end = (data.getConnectionEndTimeUs() > 0) ? rangeToPosition(data.getConnectionEndTimeUs()) : endLimit;

      FontMetrics metrics = getFontMetrics(getFont());
      String text =
        AdtUiUtils.shrinkToFit(HttpData.getUrlName(data.getUrl()), metrics, (float)(end - start - 2 * NAME_PADDING));

      double availableSpace = (end - start - metrics.stringWidth(text));
      g2d.drawString(text, (float)(start + availableSpace / 2.0), (float)((getHeight() - metrics.getHeight()) * 0.5 + metrics.getAscent()));
    }

    private void drawSelection(@NotNull Graphics2D g2d, @NotNull HttpData data, double endLimit) {
      double start = rangeToPosition(data.getRequestStartTimeUs());
      double end = (data.getConnectionEndTimeUs() > 0) ? rangeToPosition(data.getConnectionEndTimeUs()) : endLimit;
      g2d.setStroke(new BasicStroke(SELECTION_OUTLINE_BORDER));
      g2d.setColor(myTable.getSelectionBackground());
      Rectangle2D rect = new Rectangle2D.Double(start - SELECTION_OUTLINE_PADDING,
                                                (getHeight() - STATE_HEIGHT) / 2.0 - SELECTION_OUTLINE_PADDING,
                                                end - start + 2 * SELECTION_OUTLINE_PADDING,
                                                STATE_HEIGHT + 2 * SELECTION_OUTLINE_PADDING);
      g2d.draw(rect);
    }

    private double rangeToPosition(double r) {
      return (r - myRange.getMin()) / myRange.getLength() * getWidth();
    }
  }

  private final static class TooltipView extends MouseAdapter {
    @NotNull private final JTable myTable;
    @NotNull private final Range myRange;

    @NotNull private final TooltipComponent myTooltipComponent;
    @NotNull private final JPanel myContent;

    private TooltipView(@NotNull JTable table, @NotNull NetworkProfilerStageView stageView) {
      myTable = table;
      myRange = stageView.getStage().getTimeline().getSelectionRange();

      myContent = new JPanel(new TabularLayout("*", "*"));
      myContent.setBorder(TOOLTIP_BORDER);
      myContent.setBackground(ProfilerColors.TOOLTIP_BACKGROUND);
      myContent.setFont(com.android.tools.adtui.TooltipView.TOOLTIP_BODY_FONT);

      myTooltipComponent = new TooltipComponent.Builder(myContent, table, stageView.getProfilersView().getComponent()).build();
      myTooltipComponent.registerListenersOn(table);
      myTooltipComponent.setVisible(false);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      myTooltipComponent.setVisible(false);
      HttpData data = findHttpDataUnderCursor(myTable, myRange, e);
      if (data != null) {
        showTooltip(data);
      }
    }

    private void showTooltip(@NotNull HttpData data) {
      myTooltipComponent.setVisible(true);

      String urlName = HttpData.getUrlName(data.getUrl());
      long duration = data.getConnectionEndTimeUs() - data.getRequestStartTimeUs();

      myContent.removeAll();
      addToContent(newTooltipLabel(urlName));
      JLabel durationLabel = newTooltipLabel(TimeFormatter.getSingleUnitDurationString(duration));
      durationLabel.setForeground(ProfilerColors.TOOLTIP_TEXT);
      addToContent(durationLabel);

      if (data.getJavaThreads().size() > 1) {
        JPanel divider = new JPanel();
        divider.setPreferredSize(new Dimension(0, 5));
        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ProfilerColors.NETWORK_THREADS_VIEW_TOOLTIP_DIVIDER));
        divider.setBackground(myContent.getBackground());
        myContent.add(divider, new TabularLayout.Constraint(myContent.getComponentCount(), 0));

        JLabel alsoAccessedByLabel = newTooltipLabel("Also accessed by:");
        alsoAccessedByLabel.setFont(alsoAccessedByLabel.getFont().deriveFont(ImmutableMap.of(
          TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD
        )));

        addToContent(alsoAccessedByLabel);
        for (int i = 1; i < data.getJavaThreads().size(); ++i) {
          JLabel label = newTooltipLabel(data.getJavaThreads().get(i).getName());
          addToContent(label);
          if (i == data.getJavaThreads().size() - 1) {
            label.setBorder(JBUI.Borders.empty(5, 0));
          }
        }
      }
    }

    private void addToContent(@NotNull JComponent component) {
      component.setBorder(JBUI.Borders.emptyTop(5));
      myContent.add(component, new TabularLayout.Constraint(myContent.getComponentCount(), 0));
    }

    private static JLabel newTooltipLabel(String text) {
      JLabel label = new JLabel(text);
      label.setForeground(ProfilerColors.TOOLTIP_TEXT);
      label.setFont(com.android.tools.adtui.TooltipView.TOOLTIP_BODY_FONT);
      return label;
    }

    /**
     * Construct our tooltip view and attach it to the target table.
     */
    public static void install(@NotNull JTable table, @NotNull NetworkProfilerStageView stageView) {
      table.addMouseMotionListener(new TooltipView(table, stageView));
    }
  }
}
