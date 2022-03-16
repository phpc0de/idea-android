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
package com.android.tools.profilers.network;

import static com.android.tools.profilers.ProfilerLayout.ROW_HEIGHT_PADDING;
import static com.android.tools.profilers.ProfilerLayout.TOOLTIP_BORDER;

import com.android.tools.adtui.TooltipComponent;
import com.android.tools.adtui.TooltipView;
import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.adtui.model.StreamingTimeline;
import com.android.tools.adtui.model.formatter.NumberFormatter;
import com.android.tools.adtui.stdui.BorderlessTableCellRenderer;
import com.android.tools.adtui.stdui.TimelineTable;
import com.android.tools.profilers.ProfilerColors;
import com.android.tools.profilers.StageView;
import com.android.tools.profilers.network.httpdata.HttpData;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.util.text.StringUtil;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.jetbrains.annotations.NotNull;

/**
 * This class responsible for displaying table of connections information (e.g url, duration, timeline)
 * for network profiling. Each row in the table represents a single connection.
 */
final class ConnectionsView {
  /**
   * Columns for each connection information
   */
  @VisibleForTesting
  enum Column {
    NAME(0.25, String.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        return HttpData.getUrlName(data.getUrl());
      }
    },
    SIZE(0.25 / 4, Integer.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        return data.getResponsePayloadSize();
      }
    },
    TYPE(0.25 / 4, String.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        HttpData.ContentType type = data.getResponseHeader().getContentType();
        String[] mimeTypeParts = type.getMimeType().split("/");
        return mimeTypeParts[mimeTypeParts.length - 1];
      }
    },
    STATUS(0.25 / 4, Integer.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        return data.getResponseHeader().getStatusCode();
      }
    },
    TIME(0.25 / 4, Long.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        return data.getConnectionEndTimeUs() - data.getRequestStartTimeUs();
      }
    },
    TIMELINE(0.5, Long.class) {
      @Override
      Object getValueFrom(@NotNull HttpData data) {
        return data.getRequestStartTimeUs();
      }
    };

    private final double myWidthPercentage;
    private final Class<?> myType;

    Column(double widthPercentage, Class<?> type) {
      myWidthPercentage = widthPercentage;
      myType = type;
    }

    public double getWidthPercentage() {
      return myWidthPercentage;
    }

    public Class<?> getType() {
      return myType;
    }

    public String toDisplayString() {
      return StringUtil.capitalize(name().toLowerCase(Locale.getDefault()));
    }

    abstract Object getValueFrom(@NotNull HttpData data);
  }

  @NotNull
  private final NetworkProfilerStage myStage;

  @NotNull
  private final ConnectionsTableModel myTableModel;

  @NotNull
  private final JTable myConnectionsTable;

  @NotNull
  private final AspectObserver myAspectObserver;

  ConnectionsView(@NotNull NetworkProfilerStageView stageView) {
    myStage = stageView.getStage();

    myTableModel = new ConnectionsTableModel(myStage.getHttpDataFetcher());

    myConnectionsTable = TimelineTable.create(myTableModel, myStage.getTimeline(), Column.TIMELINE.ordinal());
    customizeConnectionsTable();
    createTooltip(stageView);

    myAspectObserver = new AspectObserver();
    myStage.getAspect().addDependency(myAspectObserver).onChange(NetworkProfilerAspect.SELECTED_CONNECTION, this::updateTableSelection);
  }

  @NotNull
  public JComponent getComponent() {
    return myConnectionsTable;
  }

  private void customizeConnectionsTable() {
    myConnectionsTable.setAutoCreateRowSorter(true);
    myConnectionsTable.getColumnModel().getColumn(Column.NAME.ordinal()).setCellRenderer(new BorderlessTableCellRenderer());
    myConnectionsTable.getColumnModel().getColumn(Column.SIZE.ordinal()).setCellRenderer(new SizeRenderer());
    myConnectionsTable.getColumnModel().getColumn(Column.TYPE.ordinal()).setCellRenderer(new BorderlessTableCellRenderer());
    myConnectionsTable.getColumnModel().getColumn(Column.STATUS.ordinal()).setCellRenderer(new StatusRenderer());
    myConnectionsTable.getColumnModel().getColumn(Column.TIME.ordinal()).setCellRenderer(new TimeRenderer());
    myConnectionsTable.getColumnModel().getColumn(Column.TIMELINE.ordinal()).setCellRenderer(
      new TimelineRenderer(myConnectionsTable, myStage.getTimeline()));

    myConnectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myConnectionsTable.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return; // Only handle listener on last event, not intermediate events
      }

      int selectedRow = myConnectionsTable.getSelectedRow();
      if (0 <= selectedRow && selectedRow < myTableModel.getRowCount()) {
        int modelRow = myConnectionsTable.convertRowIndexToModel(selectedRow);
        myStage.setSelectedConnection(myTableModel.getHttpData(modelRow));
      }
    });

    myConnectionsTable.setBackground(ProfilerColors.DEFAULT_BACKGROUND);
    myConnectionsTable.setShowVerticalLines(true);
    myConnectionsTable.setShowHorizontalLines(false);
    int defaultFontHeight = myConnectionsTable.getFontMetrics(myConnectionsTable.getFont()).getHeight();
    myConnectionsTable.setRowMargin(0);
    myConnectionsTable.setRowHeight(defaultFontHeight + ROW_HEIGHT_PADDING);
    myConnectionsTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
    myConnectionsTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

    myConnectionsTable.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        for (int i = 0; i < Column.values().length; ++i) {
          Column column = Column.values()[i];
          myConnectionsTable.getColumnModel().getColumn(i)
            .setPreferredWidth((int)(myConnectionsTable.getWidth() * column.getWidthPercentage()));
        }
      }
    });
    myStage.getHttpDataFetcher().addListener(httpDataList -> {
      // Although the selected row doesn't change on range moved, we do this here to prevent
      // flickering that otherwise occurs in our table.

      updateTableSelection();
    });
  }

  private void createTooltip(@NotNull StageView stageView) {
    JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setBorder(TOOLTIP_BORDER);
    textPane.setBackground(ProfilerColors.TOOLTIP_BACKGROUND);
    textPane.setForeground(ProfilerColors.TOOLTIP_TEXT);
    textPane.setFont(TooltipView.TOOLTIP_BODY_FONT);
    TooltipComponent tooltip =
      new TooltipComponent.Builder(textPane, myConnectionsTable, stageView.getProfilersView().getComponent()).build();
    tooltip.registerListenersOn(myConnectionsTable);
    myConnectionsTable.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int row = myConnectionsTable.rowAtPoint(e.getPoint());
        if (row >= 0) {
          tooltip.setVisible(true);
          String url = myTableModel.getHttpData(myConnectionsTable.convertRowIndexToModel(row)).getUrl();
          textPane.setText(url);
        }
        else {
          tooltip.setVisible(false);
        }
      }
    });
  }

  private void updateTableSelection() {
    HttpData selectedData = myStage.getSelectedConnection();
    if (selectedData != null) {
      for (int i = 0; i < myTableModel.getRowCount(); ++i) {
        if (myTableModel.getHttpData(i).getId() == selectedData.getId()) {
          int row = myConnectionsTable.convertRowIndexToView(i);
          myConnectionsTable.setRowSelectionInterval(row, row);
          return;
        }
      }
    }
    else {
      myConnectionsTable.clearSelection();
    }
  }

  private static final class ConnectionsTableModel extends AbstractTableModel {
    @NotNull private List<HttpData> myDataList = new ArrayList<>();

    private ConnectionsTableModel(HttpDataFetcher httpDataFetcher) {
      httpDataFetcher.addListener(httpDataList -> {
        myDataList = httpDataList;
        fireTableDataChanged();
      });
    }

    @Override
    public int getRowCount() {
      return myDataList.size();
    }

    @Override
    public int getColumnCount() {
      return Column.values().length;
    }

    @Override
    public String getColumnName(int column) {
      return Column.values()[column].toDisplayString();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      HttpData data = myDataList.get(rowIndex);
      return Column.values()[columnIndex].getValueFrom(data);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Column.values()[columnIndex].getType();
    }

    @NotNull
    public HttpData getHttpData(int rowIndex) {
      return myDataList.get(rowIndex);
    }
  }

  private static final class SizeRenderer extends BorderlessTableCellRenderer {
    private SizeRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    protected void setValue(Object value) {
      int bytes = (Integer)value;
      setText(bytes >= 0 ? NumberFormatter.formatFileSize(bytes) : "");
    }
  }

  private static final class StatusRenderer extends BorderlessTableCellRenderer {
    @Override
    protected void setValue(Object value) {
      Integer status = (Integer)value;
      setText(status > -1 ? Integer.toString(status) : "");
    }
  }

  private static final class TimeRenderer extends BorderlessTableCellRenderer {
    private TimeRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    protected void setValue(Object value) {
      Long durationUs = (Long)value;
      if (durationUs >= 0) {
        long durationMs = TimeUnit.MICROSECONDS.toMillis(durationUs);
        setText(StringUtil.formatDuration(durationMs));
      }
      else {
        setText("");
      }
    }
  }

  private static final class TimelineRenderer extends TimelineTable.CellRenderer implements TableModelListener {
    /**
     * Keep in sync 1:1 with {@link ConnectionsTableModel#myDataList}. When the table asks for the
     * chart to render, it will be converted from model index to view index.
     */
    @NotNull private final List<ConnectionsStateChart> myConnectionsCharts = new ArrayList<>();
    @NotNull private final JTable myTable;

    TimelineRenderer(@NotNull JTable table, @NotNull StreamingTimeline timeline) {
      super(timeline);
      myTable = table;
      myTable.getModel().addTableModelListener(this);
      tableChanged(new TableModelEvent(myTable.getModel()));
    }

    @NotNull
    @Override
    protected Component getTableCellRendererComponent(boolean isSelected, int row) {
      ConnectionsStateChart chart = myConnectionsCharts.get(myTable.convertRowIndexToModel(row));
      chart.getColors().setColorIndex(isSelected ? 1 : 0);
      return chart.getComponent();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
      myConnectionsCharts.clear();
      ConnectionsTableModel model = (ConnectionsTableModel)myTable.getModel();
      for (int i = 0; i < model.getRowCount(); ++i) {
        ConnectionsStateChart chart = new ConnectionsStateChart(model.getHttpData(i), getTimeline().getSelectionRange());
        chart.setHeightGap(0.3f);
        myConnectionsCharts.add(chart);
      }
    }
  }
}
