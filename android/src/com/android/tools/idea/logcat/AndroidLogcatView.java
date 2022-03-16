// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.android.tools.idea.logcat;

import com.google.common.annotations.VisibleForTesting;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.ddms.DeviceContext;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A UI panel which wraps a console that prints output from Android's logging system.
 */
public class AndroidLogcatView {
  public static final Key<AndroidLogcatView> ANDROID_LOGCAT_VIEW_KEY = Key.create("ANDROID_LOGCAT_VIEW_KEY");

  /**
   * This is a fake version of the selected app filter that acts as a placeholder before a real one
   * is swapped in, which happens when the pulldown of processes is populated.
   */
  static final AndroidLogcatFilter FAKE_SHOW_ONLY_SELECTED_APPLICATION_FILTER = new MatchAllFilter(getSelectedAppFilter());
  static final AndroidLogcatFilter NO_FILTERS_ITEM = new MatchAllFilter(getNoFilters());

  // TODO Refactor all this filter combo box stuff to its own class
  static final AndroidLogcatFilter EDIT_FILTER_CONFIGURATION_ITEM = new MatchAllFilter(getEditFilterConfiguration());

  private final Project myProject;
  final Disposable parentDisposable;
  private final FormattedLogcatReceiver myLogcatReceiver;
  private final AndroidLogConsole myLogConsole;
  private final DeviceContext myDeviceContext;
  private final AndroidLogFilterModel myLogFilterModel;

  private volatile IDevice myDevice;
  private DefaultComboBoxModel<AndroidLogcatFilter> myFilterComboBoxModel;
  private ActionToolbar myToolbar;
  private JPanel myPanel;

  /**
   * Called internally when the device may have changed, or been significantly altered.
   *
   * @param forceReconnect Forces the logcat connection to restart even if the device has not changed.
   */
  private void notifyDeviceUpdated(final boolean forceReconnect) {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      if (myProject.isDisposed()) {
        return;
      }
      if (forceReconnect) {
        if (myDevice != null) {
          AndroidLogcatService.getInstance().removeListener(myDevice, myLogcatReceiver);
        }
        myDevice = null;
      }
      updateLogConsole();
    });
  }

  @NotNull
  public final Project getProject() {
    return myProject;
  }

  @NotNull
  public final AndroidLogConsole getLogConsole() {
    return myLogConsole;
  }

  @NotNull
  DeviceContext getDeviceContext() {
    return myDeviceContext;
  }

  @NotNull
  AndroidLogFilterModel getLogFilterModel() {
    return myLogFilterModel;
  }

  @VisibleForTesting
  @NotNull
  ListModel<AndroidLogcatFilter> getEditFiltersComboBoxModel() {
    return myFilterComboBoxModel;
  }

  @NotNull
  ActionToolbar getToolbar() {
    return myToolbar;
  }

  /**
   * Logcat view with device obtained from {@link DeviceContext}
   */
  AndroidLogcatView(@NotNull Project project, @NotNull DeviceContext deviceContext, @NotNull Disposable parentDisposable) {
    myDeviceContext = deviceContext;
    myProject = project;
    this.parentDisposable = parentDisposable;

    AndroidLogcatPreferences preferences = AndroidLogcatPreferences.getInstance(project);
    AndroidLogcatFormatter formatter = new AndroidLogcatFormatter(ZoneId.systemDefault(), preferences);

    myLogFilterModel = new AndroidLogFilterModel(formatter, preferences);
    myLogConsole = new AndroidLogConsole(project, myLogFilterModel, formatter, this);
    myLogcatReceiver = new ViewListener(formatter, this);

    Disposer.register(parentDisposable, () -> {
      if (myDevice != null) {
        AndroidLogcatService.getInstance().removeListener(myDevice, myLogcatReceiver);
      }
    });

    DeviceContext.DeviceSelectionListener deviceSelectionListener =
      new DeviceContext.DeviceSelectionListener() {
        @Override
        public void deviceSelected(@Nullable IDevice device) {
          notifyDeviceUpdated(false);
        }

        @Override
        public void deviceChanged(@NotNull IDevice device, int changeMask) {
          if (device == myDevice && ((changeMask & IDevice.CHANGE_STATE) == IDevice.CHANGE_STATE)) {
            notifyDeviceUpdated(true);
          }
        }

        @Override
        public void clientSelected(@Nullable final Client c) {
          if (myFilterComboBoxModel == null) {
            return;
          }

          AndroidLogcatFilter selected = (AndroidLogcatFilter)myFilterComboBoxModel.getSelectedItem();
          updateDefaultFilters(c != null ? c.getClientData() : null);

          // Attempt to preserve selection as best we can. Often we don't have to do anything,
          // but it's possible an old filter was replaced with an updated version - so, new
          // instance, but the same name.
          if (selected != null && myFilterComboBoxModel.getSelectedItem() != selected) {
            selectFilterByName(selected.getName());
          }
        }
      };
    deviceContext.addListener(deviceSelectionListener, parentDisposable);

    JComponent consoleComponent = myLogConsole.getComponent();

    final ConsoleView console = myLogConsole.getConsole();
    if (console != null) {
      myToolbar = ActionManager.getInstance().createActionToolbar("AndroidLogcatView", myLogConsole.getOrCreateActions(), false);
      myToolbar.setTargetComponent(console.getComponent());

      myPanel.add(myToolbar.getComponent(), BorderLayout.WEST);
    }

    myPanel.add(consoleComponent, BorderLayout.CENTER);
    Disposer.register(parentDisposable, myLogConsole);

    updateLogConsole();
  }

  @NotNull
  public Component createEditFiltersComboBox() {
    JComboBox<AndroidLogcatFilter> editFiltersCombo = new ComboBox<>();
    myFilterComboBoxModel = new DefaultComboBoxModel<>();
    myFilterComboBoxModel.addElement(NO_FILTERS_ITEM);
    myFilterComboBoxModel.addElement(EDIT_FILTER_CONFIGURATION_ITEM);

    updateDefaultFilters(null);
    updateUserFilters();
    String selectName = AndroidLogcatPreferences.getInstance(myProject).TOOL_WINDOW_CONFIGURED_FILTER;
    if (StringUtil.isEmpty(selectName)) {
      selectName = myDeviceContext != null ? getSelectedAppFilter() : getNoFilters();
    }
    selectFilterByName(selectName);

    editFiltersCombo.setModel(myFilterComboBoxModel);
    applySelectedFilter();
    // note: the listener is added after the initial call to populate the combo
    // boxes in the above call to updateConfiguredFilters
    editFiltersCombo.addItemListener(new ItemListener() {
      @Nullable private AndroidLogcatFilter myLastSelected;

      @Override
      public void itemStateChanged(ItemEvent e) {
        Object item = e.getItem();
        if (e.getStateChange() == ItemEvent.DESELECTED) {
          if (item instanceof AndroidLogcatFilter) {
            myLastSelected = (AndroidLogcatFilter)item;
          }
        }
        else if (e.getStateChange() == ItemEvent.SELECTED) {
          if (item.equals(EDIT_FILTER_CONFIGURATION_ITEM)) {
            final EditLogFilterDialog dialog =
              new EditLogFilterDialog(AndroidLogcatView.this, myLastSelected == null ? null : myLastSelected.getName());
            dialog.setTitle(AndroidBundle.message("android.logcat.new.filter.dialog.title"));
            if (dialog.showAndGet()) {
              final PersistentAndroidLogFilters.FilterData filterData = dialog.getActiveFilter();
              updateUserFilters();
              if (filterData != null) {
                selectFilterByName(filterData.getName());
              }
            }
            else {
              editFiltersCombo.setSelectedItem(myLastSelected);
            }
          }
          else {
            applySelectedFilter();
          }
        }
      }
    });

    editFiltersCombo.setRenderer(new ColoredListCellRenderer<Object>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof AndroidLogcatFilter) {
          setBorder(null);
          append(((AndroidLogcatFilter)value).getName());
        }
        else {
          setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
          append(value.toString());
        }
      }
    });

    return editFiltersCombo;
  }

  boolean isActive() {
    return ToolWindowManager.getInstance(myProject).getToolWindow("Logcat").isVisible();
  }

  public final void activate() {
    if (isActive()) {
      updateLogConsole();
    }
    if (myLogConsole != null) {
      myLogConsole.activate();
    }
  }

  private void updateLogConsole() {
    IDevice device = getSelectedDevice();
    if (myDevice != device) {
      AndroidLogcatService androidLogcatService = AndroidLogcatService.getInstance();
      if (myDevice != null) {
        androidLogcatService.removeListener(myDevice, myLogcatReceiver);
      }
      // We check for null, because myLogConsole.clear() depends on myLogConsole.getConsole() not being null
      if (myLogConsole.getConsole() != null) {
        myLogConsole.clear();
      }

      myDevice = device;

      myLogFilterModel.processingStarted();
      androidLogcatService.addListener(myDevice, myLogcatReceiver, true);
    }
  }

  @Nullable
  IDevice getSelectedDevice() {
    if (myDeviceContext != null) {
      return myDeviceContext.getSelectedDevice();
    }
    else {
      return null;
    }
  }

  private void applySelectedFilter() {
    final Object filter = myFilterComboBoxModel.getSelectedItem();
    if (filter instanceof AndroidLogcatFilter) {
      ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Applying Filter...") {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          myLogFilterModel.updateLogcatFilter((AndroidLogcatFilter)filter);
        }
      });
    }
  }

  /**
   * Update the list of filters which are provided by default (selected app and filters provided
   * by plugins). These show up in the top half of the filter pulldown.
   */
  @VisibleForTesting
  void updateDefaultFilters(@Nullable ClientData client) {
    int noFilterIndex = myFilterComboBoxModel.getIndexOf(NO_FILTERS_ITEM);
    for (int i = 0; i < noFilterIndex; i++) {
      myFilterComboBoxModel.removeElementAt(0);
    }

    AndroidLogcatFilter filter = client == null ? FAKE_SHOW_ONLY_SELECTED_APPLICATION_FILTER : new SelectedProcessFilter(client.getPid());
    int insertIndex = 0;

    myFilterComboBoxModel.insertElementAt(filter, insertIndex++);

    for (LogcatFilterProvider filterProvider : LogcatFilterProvider.EP_NAME.getExtensions()) {
      myFilterComboBoxModel.insertElementAt(filterProvider.getFilter(client), insertIndex++);
    }
  }

  /**
   * Update the list of filters which have been created by the user. These show up in the bottom
   * half of the filter pulldown.
   */
  private void updateUserFilters() {
    int editFilterConfigurationItemIndex = myFilterComboBoxModel.getIndexOf(EDIT_FILTER_CONFIGURATION_ITEM);
    assert editFilterConfigurationItemIndex != -1;

    int userFiltersStartIndex = editFilterConfigurationItemIndex + 1;

    while (myFilterComboBoxModel.getSize() > userFiltersStartIndex) {
      myFilterComboBoxModel.removeElementAt(userFiltersStartIndex);
    }

    final List<PersistentAndroidLogFilters.FilterData> filters = PersistentAndroidLogFilters.getInstance(myProject).getFilters();
    for (PersistentAndroidLogFilters.FilterData filter : filters) {
      final String name = filter.getName();
      assert name != null; // The UI that creates filters should ensure a name was created

      AndroidLogcatFilter compiled = DefaultAndroidLogcatFilter.compile(filter, name);
      myFilterComboBoxModel.addElement(compiled);
    }
  }

  private void selectFilterByName(String name) {
    Optional<AndroidLogcatFilter> optionalFilter = IntStream.range(0, myFilterComboBoxModel.getSize())
                                                            .mapToObj(i -> myFilterComboBoxModel.getElementAt(i))
                                                            .filter(filter -> filter.getName().equals(name))
                                                            .findFirst();

    optionalFilter.ifPresent(filter -> myFilterComboBoxModel.setSelectedItem(filter));
  }

  @NotNull
  public final JPanel getContentPanel() {
    return myPanel;
  }

  static final class MyRestartAction extends AnAction {
    private final AndroidLogcatView myView;

    MyRestartAction(@NotNull AndroidLogcatView view) {
      super(AndroidBundle.message("android.restart.logcat.action.text"), AndroidBundle.message("android.restart.logcat.action.description"),
            AllIcons.Actions.Restart);

      myView = view;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myView.notifyDeviceUpdated(true);
    }
  }

  static final class MyConfigureLogcatHeaderAction extends AnAction {
    private final AndroidLogcatView myView;

    MyConfigureLogcatHeaderAction(@NotNull AndroidLogcatView view) {
      super(AndroidBundle.message("android.configure.logcat.header.text"),
            AndroidBundle.message("android.configure.logcat.header.description"), AllIcons.General.GearPlain);

      myView = view;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = myView.myProject;
      AndroidLogcatPreferences preferences = AndroidLogcatPreferences.getInstance(project);
      ConfigureLogcatHeaderDialog dialog = new ConfigureLogcatHeaderDialog(project, preferences, ZoneId.systemDefault());

      if (dialog.showAndGet()) {
        preferences.LOGCAT_FORMAT_STRING = dialog.getFormat();
        preferences.SHOW_AS_SECONDS_SINCE_EPOCH = dialog.getShowAsSecondsSinceEpochCheckBox().isSelected();

        myView.myLogConsole.refresh();
      }
    }
  }

  static String getSelectedAppFilter() {
    return AndroidBundle.message("android.logcat.filters.selected");
  }

  static String getNoFilters() {
    return AndroidBundle.message("android.logcat.filters.none");
  }

  static String getEditFilterConfiguration() {
    return AndroidBundle.message("android.logcat.filters.edit");
  }
}
