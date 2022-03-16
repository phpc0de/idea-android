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
package com.android.tools.idea.avdmanager;

import com.android.resources.Density;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkVersionInfo;
import com.android.sdklib.devices.Device;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.repository.IdDisplay;
import com.android.sdklib.repository.targets.SystemImage;
import com.android.tools.adtui.common.ColoredIconGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.TableView;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import icons.StudioIcons;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A UI component which lists the existing AVDs
 */
public class AvdDisplayList extends JPanel implements ListSelectionListener, AvdActionPanel.AvdRefreshProvider,
                                                      AvdUiAction.AvdInfoProvider {
  public static final String NONEMPTY = "nonempty";
  public static final String EMPTY = "empty";

  private static final String MOBILE_TAG_STRING = "mobile-device";

  @Nullable private final Project myProject;
  private final JPanel myCenterCardPanel;
  private final JPanel myNotificationPanel;

  private final AvdInfoTableView myTable = new AvdInfoTableView();
  private final ListTableModel<AvdInfo> myModel = new ListTableModel<>();
  private final Set<AvdSelectionListener> myListeners = new HashSet<>();
  private final AvdActionsColumnInfo myActionsColumnRenderer = new AvdActionsColumnInfo("Actions", 2 /* Num Visible Actions */);
  private static final HashMap<String, HighlightableIconPair> myDeviceClassIcons = new HashMap<>(8);
  private final Logger myLogger = Logger.getInstance(AvdDisplayList.class);

  /**
   * Components which wish to receive a notification when the user has selected an AVD from this
   * table must implement this interface and register themselves through {@link #addSelectionListener(AvdSelectionListener)}.
   */
  public interface AvdSelectionListener {
    void onAvdSelected(@Nullable AvdInfo avdInfo);
  }

  public AvdDisplayList(@Nullable Project project) {
    myProject = project;

    myModel.setColumnInfos(newColumns().toArray(ColumnInfo.EMPTY_ARRAY));
    myModel.setSortable(true);

    myTable.setModelAndUpdateColumns(myModel);
    myTable.setDefaultRenderer(Object.class, new MyRenderer(myTable.getDefaultRenderer(Object.class)));
    TableRowSorter<TableModel> sorter = new TableRowSorter<>(myModel);
    sorter.setSortKeys(ImmutableList.of(new RowSorter.SortKey(1, SortOrder.ASCENDING))); // Sort by name.
    myTable.setRowSorter(sorter);
    setLayout(new BorderLayout());
    myCenterCardPanel = new JPanel(new CardLayout());
    myNotificationPanel = new JPanel();
    myNotificationPanel.setLayout(new BoxLayout(myNotificationPanel, BoxLayout.Y_AXIS));
    JPanel nonemptyPanel = new JPanel(new BorderLayout());
    myCenterCardPanel.add(nonemptyPanel, NONEMPTY);
    nonemptyPanel.add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);
    nonemptyPanel.add(myNotificationPanel, BorderLayout.NORTH);
    myCenterCardPanel.add(new EmptyAvdListPanel(this), EMPTY);
    add(myCenterCardPanel, BorderLayout.CENTER);
    JPanel southPanel = new JPanel(new BorderLayout());
    JButton helpButton = new JButton(AllIcons.Actions.Help);
    helpButton.putClientProperty("JButton.buttonType", "segmented-only");
    helpButton.addActionListener(e -> BrowserUtil.browse("http://developer.android.com/r/studio-ui/virtualdeviceconfig.html"));
    JButton refreshButton = new JButton(AllIcons.Actions.Refresh);
    refreshButton.putClientProperty("JButton.buttonType", "segmented-only");
    refreshButton.addActionListener(e -> refreshAvds());
    JButton newButton = new JButton(new CreateAvdAction(this));
    newButton.putClientProperty("JButton.buttonType", "segmented-only");

    JPanel southEastPanel = new JPanel(new FlowLayout());
    JPanel southWestPanel = new JPanel(new FlowLayout());
    southEastPanel.add(refreshButton);
    if (UIUtil.isUnderAquaBasedLookAndFeel()) {
      southWestPanel.add(helpButton);
    }
    else {
      southEastPanel.add(helpButton);
    }
    southWestPanel.add(newButton);
    southPanel.add(southEastPanel, BorderLayout.EAST);
    southPanel.add(southWestPanel, BorderLayout.WEST);
    nonemptyPanel.add(southPanel, BorderLayout.SOUTH);
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.getSelectionModel().addListSelectionListener(this);
    MouseAdapter editingListener = new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        possiblySwitchEditors(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        possiblySwitchEditors(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        possiblySwitchEditors(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        possiblySwitchEditors(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        possiblyShowPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        possiblyShowPopup(e);
      }
    };
    myTable.addMouseListener(editingListener);
    myTable.addMouseMotionListener(editingListener);
    LaunchListener launchListener = new LaunchListener();
    myTable.addMouseListener(launchListener);
    ActionMap am = myTable.getActionMap();
    am.put("selectPreviousColumnCell", new CycleAction(true));
    am.put("selectNextColumnCell", new CycleAction(false));
    am.put("deleteAvd", new DeleteAvdAction(this));
    myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
    myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enter");
    myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteAvd");
    myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteAvd");
    am.put("enter", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doAction();
      }
    });
    refreshAvds();
  }

  public void addSelectionListener(AvdSelectionListener listener) {
    myListeners.add(listener);
  }

  public void removeSelectionListener(AvdSelectionListener listener) {
    myListeners.remove(listener);
  }

  /**
   * This class implements the table selection interface and passes the selection events on to its listeners.
   */
  @Override
  public void valueChanged(@NotNull ListSelectionEvent event) {
    // Required so the editor component is updated to know it's selected.
    myTable.editCellAt(myTable.getSelectedRow(), myTable.getSelectedColumn());
    AvdInfo selected = myTable.getSelectedObject();
    for (AvdSelectionListener listener : myListeners) {
      listener.onAvdSelected(selected);
    }
  }

  @Nullable
  @Override
  public AvdInfo getAvdInfo() {
    return myTable.getSelectedObject();
  }

  /**
   * Reload AVD definitions from disk and repopulate the table
   */
  @Override
  public void refreshAvds() {
    List<AvdInfo> avds = AvdManagerConnection.getDefaultAvdManagerConnection().getAvds(true);
    myModel.setItems(avds);
    if (avds.isEmpty()) {
      ((CardLayout)myCenterCardPanel.getLayout()).show(myCenterCardPanel, EMPTY);
    } else {
      ((CardLayout)myCenterCardPanel.getLayout()).show(myCenterCardPanel, NONEMPTY);
    }
    refreshErrorCheck();
  }

  /**
   * Reload AVD definitions from disk, repopulate the table,
   * and select the indicated AVD
   */
  @Override
  public void refreshAvdsAndSelect(@Nullable AvdInfo avdToSelect) {
    refreshAvds();
    if (avdToSelect != null) {
      for (AvdInfo listItem : myTable.getItems()) {
        if (listItem.getName().equals(avdToSelect.getName())) {
          ArrayList<AvdInfo> selectedAvds = new ArrayList<>();
          selectedAvds.add(listItem);
          myTable.setSelection(selectedAvds);
          break;
        }
      }
    }
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return this;
  }

  @NotNull
  @Override
  public JComponent getAvdProviderComponent() {
    return this;
  }

  private void possiblySwitchEditors(MouseEvent e) {
    Point p = e.getPoint();
    int row = myTable.rowAtPoint(p);
    int col = myTable.columnAtPoint(p);
    if (row != myTable.getEditingRow() || col != myTable.getEditingColumn()) {
      if (row != -1 && col != -1 && myTable.isCellEditable(row, col)) {
        myTable.editCellAt(row, col);
      }
    }
  }
  private void possiblyShowPopup(MouseEvent e) {
    if (!e.isPopupTrigger()) {
      return;
    }
    Point p = e.getPoint();
    int row = myTable.rowAtPoint(p);
    int col = myTable.columnAtPoint(p);
    if (row != -1 && col != -1) {
      int lastColumn = myTable.getColumnCount() - 1;
      Component maybeActionPanel = myTable.getCellRenderer(row, lastColumn).
          getTableCellRendererComponent(myTable, myTable.getValueAt(row, lastColumn), false, true, row, lastColumn);
      if (maybeActionPanel instanceof AvdActionPanel) {
        ((AvdActionPanel)maybeActionPanel).showPopup(myTable, e);
      }
    }
  }

  /**
   * @return the device screen size of this AVD
   */
  @VisibleForTesting
  static Dimension getScreenSize(@NotNull AvdInfo info) {
    DeviceManagerConnection deviceManager = DeviceManagerConnection.getDefaultDeviceManagerConnection();
    Device device = deviceManager.getDevice(info.getDeviceName(), info.getDeviceManufacturer());
    if (device == null) {
      return null;
    }
    return device.getScreenSize(device.getDefaultState().getOrientation());
  }

  /**
   * @return the resolution of a given AVD as a string of the format [width]x[height] - [density]
   * (e.g. 1200x1920 - xhdpi) or "Unknown Resolution" if the AVD does not define a resolution.
   */
  @VisibleForTesting
  static String getResolution(@NotNull AvdInfo info) {
    DeviceManagerConnection deviceManager = DeviceManagerConnection.getDefaultDeviceManagerConnection();
    Device device = deviceManager.getDevice(info.getDeviceName(), info.getDeviceManufacturer());
    Dimension res = null;
    Density density = null;
    if (device != null) {
      res = device.getScreenSize(device.getDefaultState().getOrientation());
      density = device.getDefaultHardware().getScreen().getPixelDensity();
    }
    String resolution;
    String densityString = density == null ? "Unknown Density" : density.getResourceValue();
    if (res != null) {
      resolution = String.format(Locale.getDefault(), "%1$d \u00D7 %2$d: %3$s", res.width, res.height, densityString);
    } else {
      resolution = "Unknown Resolution";
    }
    return resolution;
  }

  /**
   * Get the icons representing the device class of the given AVD (e.g. phone/tablet, Wear, TV)
   */
  @VisibleForTesting
  static HighlightableIconPair getDeviceClassIconPair(@NotNull AvdInfo info) {
    String id = info.getTag().getId();
    String path;
    HighlightableIconPair thisClassPair;
    if (id.contains("android-")) {
      path = "StudioIcons.Avd.DEVICE_" + id.substring("android-".length()).toUpperCase(Locale.ENGLISH) + "_LARGE";
      thisClassPair = myDeviceClassIcons.get(path);
      if (thisClassPair == null) {
        thisClassPair = new HighlightableIconPair(IconLoader.getReflectiveIcon(path, StudioIcons.class.getClassLoader()));
        myDeviceClassIcons.put(path, thisClassPair);
      }
    } else {
      // Phone/tablet
      thisClassPair = myDeviceClassIcons.get(MOBILE_TAG_STRING);
      if (thisClassPair == null) {
        thisClassPair = new HighlightableIconPair(StudioIcons.Avd.DEVICE_MOBILE_LARGE);
        myDeviceClassIcons.put(MOBILE_TAG_STRING, thisClassPair);
      }
    }
    return thisClassPair;
  }

  @VisibleForTesting
  static class HighlightableIconPair {
    private final @Nullable Icon myBaseIcon;
    private final @Nullable Icon highlightedIcon;

    public HighlightableIconPair(@Nullable Icon baseIcon) {
      myBaseIcon = baseIcon;
      highlightedIcon = baseIcon == null ? null : ColoredIconGenerator.generateWhiteIcon(baseIcon);
    }

    public @Nullable Icon getBaseIcon() {
      return myBaseIcon;
    }

    public @Nullable Icon getHighlightedIcon() {
      return highlightedIcon;
    }
  }

  @NotNull
  private Collection<ColumnInfo<AvdInfo, ?>> newColumns() {
    return Arrays.asList(
      new AvdIconColumnInfo("Type") {
        @Override
        public @NotNull HighlightableIconPair valueOf(@NotNull AvdInfo avdInfo) {
          return getDeviceClassIconPair(avdInfo);
        }
      },
      new AvdColumnInfo("Name") {
        private final Collator collator = Collator.getInstance();

        @Override
        public @NotNull String valueOf(@NotNull AvdInfo info) {
          return info.getDisplayName();
        }

        @Override
        public @NotNull Comparator<AvdInfo> getComparator() {
          return (avd1, avd2) -> collator.compare(avd1.getDisplayName(), avd2.getDisplayName());
        }
      },
      new AvdIconColumnInfo("Play Store") {
        private final HighlightableIconPair emptyIconPair = new HighlightableIconPair(null);
        private final HighlightableIconPair playStoreIconPair = new HighlightableIconPair(StudioIcons.Avd.DEVICE_PLAY_STORE);

        @Override
        public @NotNull HighlightableIconPair valueOf(@NotNull AvdInfo avdInfo) {
          return avdInfo.hasPlayStore() ? playStoreIconPair : emptyIconPair;
        }

        @Override
        public @NotNull Comparator<AvdInfo> getComparator() {
          return (avd1, avd2) -> Boolean.compare(avd2.hasPlayStore(), avd1.hasPlayStore());
        }
      },
      new AvdColumnInfo("Resolution") {
        @Override
        public @NotNull String valueOf(@NotNull AvdInfo avdInfo) {
          return getResolution(avdInfo);
        }

        /**
         * We override the comparator here to sort the AVDs by total number of pixels on the screen rather than
         * the default sort order (lexicographically by string representation).
         */
        @Override
        public @NotNull Comparator<AvdInfo> getComparator() {
          return (o1, o2) -> {
            Dimension d1 = getScreenSize(o1);
            Dimension d2 = getScreenSize(o2);
            if (d1 == d2) {
              return 0;
            } else if (d1 == null) {
              return -1;
            } else if (d2 == null) {
              return 1;
            } else {
              return d1.width * d1.height - d2.width * d2.height;
            }
          };
        }
      },
      new AvdColumnInfo("API") {
        @Override
        public @NotNull String valueOf(@NotNull AvdInfo avdInfo) {
          return avdInfo.getAndroidVersion().getApiString();
        }

        /**
         * We override the comparator here to sort the API levels numerically (when possible;
         * with preview platforms codenames are compared alphabetically).
         */
        @Override
        public @NotNull Comparator<AvdInfo> getComparator() {
          ApiLevelComparator comparator = new ApiLevelComparator();
          return (o1, o2) -> comparator.compare(valueOf(o1), valueOf(o2));
        }
      },
      new AvdColumnInfo("Target") {
        @Override
        public @NotNull String valueOf(@NotNull AvdInfo info) {
          return targetString(info.getAndroidVersion(), info.getTag());
        }
      },
      new AvdColumnInfo("CPU/ABI") {
        @Override
        public @NotNull String valueOf(@NotNull AvdInfo avdInfo) {
          return avdInfo.getCpuArch();
        }
      },
      new SizeOnDiskColumn(myTable),
      myActionsColumnRenderer);
  }

  @VisibleForTesting
  static @NotNull String targetString(@NotNull AndroidVersion version, @NotNull IdDisplay tag) {
    StringBuilder resultBuilder = new StringBuilder(32);
    resultBuilder.append("Android ");
    resultBuilder.append(SdkVersionInfo.getVersionStringSanitized(version.getFeatureLevel()));
    if (!tag.equals(SystemImage.DEFAULT_TAG)) {
      resultBuilder.append(" (").append(tag.getDisplay()).append(")");
    }
    return resultBuilder.toString();
  }

  private void refreshErrorCheck() {
    AtomicBoolean refreshUI = new AtomicBoolean(myNotificationPanel.getComponentCount() > 0);
    myNotificationPanel.removeAll();
    ListenableFuture<AccelerationErrorCode> error = AvdManagerConnection.getDefaultAvdManagerConnection().checkAccelerationAsync();
    Futures.addCallback(error, new FutureCallback<AccelerationErrorCode>() {
      @Override
      public void onSuccess(AccelerationErrorCode result) {
        if (result != AccelerationErrorCode.ALREADY_INSTALLED) {
          refreshUI.set(true);
          myNotificationPanel.add(new AccelerationErrorNotificationPanel(result, myProject, () -> refreshErrorCheck()));
        }
        if (refreshUI.get()) {
          myNotificationPanel.revalidate();
          myNotificationPanel.repaint();
        }
      }

      @Override
      public void onFailure(@NotNull Throwable t) {
        myLogger.warn("Check for emulation acceleration failed", t);
      }
    }, EdtExecutorService.getInstance());
  }

  /**
   * This class extends {@link ColumnInfo} in order to pull an {@link Icon} value from a given {@link AvdInfo}.
   * This is the column info used for the Type and Status columns.
   * It uses the icon field renderer ({@link #ourIconRenderer}) and does not sort by default.
   */
  private static abstract class AvdIconColumnInfo extends ColumnInfo<AvdInfo, HighlightableIconPair> {
    /**
     * Renders an icon in a small square field
     */
    private static final TableCellRenderer ourIconRenderer = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        HighlightableIconPair iconPair = (HighlightableIconPair)value;
        JBLabel label = new JBLabel(iconPair.getBaseIcon());
        if (value == StudioIcons.Avd.DEVICE_PLAY_STORE) {
          // (No accessible name for the Device Type column)
          AccessibleContextUtil.setName(label, "Play Store");
        }
        if (table.getSelectedRow() == row) {
          label.setBackground(table.getSelectionBackground());
          label.setForeground(table.getSelectionForeground());
          label.setOpaque(true);
          Icon theIcon = label.getIcon();
          if (theIcon != null) {
            Icon highlightedIcon = iconPair.getHighlightedIcon();
            if (highlightedIcon != null) {
              label.setIcon(highlightedIcon);
            }
          }
        }
        return label;
      }

    };

    public AvdIconColumnInfo(@NotNull String name) {
      super(name);
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(AvdInfo o) {
      return ourIconRenderer;
    }
  }

  /**
   * This class extends {@link ColumnInfo} in order to pull a string value from a given {@link AvdInfo}.
   * This is the column info used for most of our table, including the Name, Resolution, and API level columns.
   */
  public abstract static class AvdColumnInfo extends ColumnInfo<AvdInfo, String> {
    public AvdColumnInfo(@NotNull String name) {
      super(name);
    }

    @Nullable
    @Override
    public Comparator<AvdInfo> getComparator() {
      return (o1, o2) -> {
        String s1 = valueOf(o1);
        String s2 = valueOf(o2);
        return Comparing.compare(s1, s2);
      };
    }
  }

  private class ActionRenderer extends AbstractTableCellEditor implements TableCellRenderer {
    private final AvdActionPanel myComponent;

    ActionRenderer(int numVisibleActions, AvdInfo info) {
      myComponent = new AvdActionPanel(info, numVisibleActions, AvdDisplayList.this);
    }

    private @NotNull Component getComponent(@NotNull JTable table, int row, int column) {
      if (table.getSelectedRow() == row) {
        myComponent.setBackground(table.getSelectionBackground());
        myComponent.setForeground(table.getSelectionForeground());
        myComponent.setHighlighted(true);
      } else {
        myComponent.setBackground(table.getBackground());
        myComponent.setForeground(table.getForeground());
        myComponent.setHighlighted(false);
      }
      myComponent.setFocused(table.getSelectedRow() == row && table.getSelectedColumn() == column);
      return myComponent;
    }

    public boolean cycleFocus(boolean backward) {
      return myComponent.cycleFocus(backward);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      return getComponent(table, row, column);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      return getComponent(table, row, column);
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }
  }
  /**
   * Custom table cell renderer that renders an action panel for a given AVD entry
   */
  private class AvdActionsColumnInfo extends ColumnInfo<AvdInfo, AvdInfo> {
    private final int myNumVisibleActions;

    /**
     * This cell renders an action panel for both the editor component and the display component
     */
    private final Map<AvdInfo, ActionRenderer> ourActionPanelRendererEditor = new HashMap<>();

    public AvdActionsColumnInfo(@NotNull String name, int numVisibleActions) {
      super(name);
      myNumVisibleActions = numVisibleActions;
    }

    @Nullable
    @Override
    public AvdInfo valueOf(AvdInfo avdInfo) {
      return avdInfo;
    }

    /**
     * We override the comparator here so that we can sort by healthy vs not healthy AVDs
     */
    @Nullable
    @Override
    public Comparator<AvdInfo> getComparator() {
      return Comparator.comparing(AvdInfo::getStatus);
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(AvdInfo avdInfo) {
      return getComponent(avdInfo);
    }

    public ActionRenderer getComponent(AvdInfo avdInfo) {
      ActionRenderer renderer = ourActionPanelRendererEditor.get(avdInfo);
      if (renderer == null) {
        renderer = new ActionRenderer(myNumVisibleActions, avdInfo);
        ourActionPanelRendererEditor.put(avdInfo, renderer);
      }
      return renderer;
    }

    @Nullable
    @Override
    public TableCellEditor getEditor(AvdInfo avdInfo) {
      return getComponent(avdInfo);
    }

    @Override
    public boolean isCellEditable(AvdInfo avdInfo) {
      return true;
    }

    public boolean cycleFocus(AvdInfo info, boolean backward) {
      return getComponent(info).cycleFocus(backward);
    }
  }

  private class LaunchListener extends MouseAdapter {
    @Override
    public void mouseClicked(@NotNull MouseEvent event) {
      if (event.getClickCount() == 2) {
        doAction();
      }
    }
  }

  private void doAction() {
    AvdInfo info = getAvdInfo();
    if (info != null) {
      if (info.getStatus() == AvdInfo.AvdStatus.OK) {
        new RunAvdAction(this).actionPerformed(null);
      } else {
        new EditAvdAction(this).actionPerformed(null);
      }
    }
  }


  private class CycleAction extends AbstractAction {
    boolean myBackward;

    CycleAction(boolean backward) {
      myBackward = backward;
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent event) {
      int selectedRow = myTable.getSelectedRow();
      int selectedColumn = myTable.getSelectedColumn();
      int actionsColumn = myModel.findColumn(myActionsColumnRenderer.getName());
      if (myBackward) {
        cycleBackward(selectedRow, selectedColumn, actionsColumn);
      }
      else {
        cycleForward(selectedRow, selectedColumn, actionsColumn);
      }
      selectedRow = myTable.getSelectedRow();
      if (selectedRow != -1) {
        myTable.editCellAt(selectedRow, myTable.getSelectedColumn());
      }
      repaint();
    }

    private void cycleForward(int selectedRow, int selectedColumn, int actionsColumn) {
      if (selectedColumn == actionsColumn && selectedRow == myTable.getRowCount() - 1) {
        // We're in the last cell of the table. Check whether we can cycle action buttons
        if (!myActionsColumnRenderer.cycleFocus(myTable.getSelectedObject(), false)) {
          // At the end of action buttons. Remove selection and leave table.
          TableCellEditor cellEditor = myActionsColumnRenderer.getEditor(getAvdInfo());
          if (cellEditor != null) {
            cellEditor.stopCellEditing();
          }
          myTable.removeRowSelectionInterval(selectedRow, selectedRow);
          KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
          manager.focusNextComponent(myTable);
        }
      } else if (selectedColumn != actionsColumn && selectedRow != -1) {
        // We're in the table, but not on the action column. Select the action column.
        myTable.setColumnSelectionInterval(actionsColumn, actionsColumn);
        myActionsColumnRenderer.cycleFocus(myTable.getSelectedObject(), false);
      } else if (selectedRow == -1 || !myActionsColumnRenderer.cycleFocus(myTable.getSelectedObject(), false)) {
        // We aren't in the table yet, or we are in the actions column and at the end of the focusable actions. Move to the next row
        // and select the first column
        myTable.setColumnSelectionInterval(0, 0);
        myTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
      }
    }

    private void cycleBackward(int selectedRow, int selectedColumn, int actionsColumn) {
      if (selectedColumn == 0 && selectedRow == 0) {
        // We're in the first cell of the table. Remove selection and leave table.
        myTable.removeRowSelectionInterval(selectedRow, selectedRow);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.focusPreviousComponent();
      } else if (selectedColumn == actionsColumn && selectedRow != -1 && !myActionsColumnRenderer.cycleFocus(myTable.getSelectedObject(), true)) {
        // We're on an actions column. If we fail to cycle actions, select the first cell in the row.
        myTable.setColumnSelectionInterval(0, 0);
      } else if (selectedRow == -1 || selectedColumn != actionsColumn) {
        // We aren't in the table yet, or we're not in the actions column. Move to the previous (or last) row.
        // and select the actions column
        if (selectedRow == -1) {
          selectedRow = myTable.getRowCount();
        }
        myTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
        myTable.setColumnSelectionInterval(actionsColumn, actionsColumn);
        myActionsColumnRenderer.cycleFocus(myTable.getSelectedObject(), true);
      }
    }
  }


  /**
   * Renders a cell with borders.
   */
  private static class MyRenderer implements TableCellRenderer {
    private static final Border myBorder = JBUI.Borders.empty(10);
    TableCellRenderer myDefaultRenderer;

    MyRenderer(TableCellRenderer defaultRenderer) {
      myDefaultRenderer = defaultRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JComponent result = (JComponent)myDefaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      result.setBorder(myBorder);
      return result;
    }
  }

  /**
   * Set preferred widths when rendering
   */
  private static class AvdInfoTableView extends TableView<AvdInfo> {
    // Column indices
    private static final int NAME = 1;
    private static final int RESOLUTION = 3;
    private static final int TARGET = 5;

    private @Nullable List<Integer> myPreferredColumnWidths;

    public AvdInfoTableView() {
      setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      addPropertyChangeListener(event -> {
        if (event.getPropertyName().equals("model")) {
          myPreferredColumnWidths = null;
        }
      });
      addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(@NotNull ComponentEvent e) {
          myPreferredColumnWidths = null;
        }
      });
    }

    @Override
    public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
      Component component = super.prepareRenderer(renderer, row, column);
      columnModel.getColumn(column).setPreferredWidth(getColumnPreferredWidth(column));
      return component;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
      myPreferredColumnWidths = null;
      super.tableChanged(e);
    }

    private int getColumnPreferredWidth(int columnIndex) {
      if (myPreferredColumnWidths == null) {
        myPreferredColumnWidths = calculatePreferredWidths();
      }
      return myPreferredColumnWidths.get(columnIndex);
    }

    private @NotNull List<Integer> calculatePreferredWidths() {
      List<Integer> widths = new ArrayList<>(Collections.nCopies(columnModel.getColumnCount(), 0));
      int tableWidth = getWidth() - getInsets().left - getInsets().right;
      if (tableWidth <= 0) {
        return widths;
      }

      int totalWidth = 0;
      for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
        int preferredWidth = calculateColumnPreferredWidth(columnIndex);
        widths.set(columnIndex, preferredWidth);
        totalWidth += preferredWidth;
      }

      // Give remaining width to the name, resolution, and target columns
      int remaining = tableWidth - totalWidth;
      if (remaining > 0) {
        widths.set(NAME, widths.get(NAME) + remaining * 3 / 5);
        widths.set(RESOLUTION, widths.get(RESOLUTION) + remaining / 5);
        widths.set(TARGET, widths.get(TARGET) + remaining - remaining * 3 / 5 - remaining / 5);
      }
      else {
        // If remaining is negative, that means there isn't enough space. Reduce the resolution and target first
        widths.set(RESOLUTION, widths.get(RESOLUTION) + remaining / 2);
        widths.set(TARGET, widths.get(TARGET) + remaining / 2);
      }

      return widths;
    }

    private int calculateColumnPreferredWidth(int columnIndex) {
      // Get header width
      JTableHeader header = getTableHeader();
      int headerGap = 17;
      int width = header.getFontMetrics(header.getFont()).stringWidth(getColumnName(columnIndex))
                  + JBUIScale.scale(headerGap);

      // Check if any cells have wider data
      for (int rowIndex = 0; rowIndex < getRowCount(); rowIndex++) {
        TableCellRenderer cellRenderer = getCellRenderer(rowIndex, columnIndex);
        Component component = super.prepareRenderer(cellRenderer, rowIndex, columnIndex);
        int cellWidth = component.getPreferredSize().width + getIntercellSpacing().width;
        if (cellWidth > width) {
          width = cellWidth;
        }
      }

      return width;
    }
  }
}
