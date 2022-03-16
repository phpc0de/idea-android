/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.updater.configure;

import com.android.repository.api.RepositorySource;
import com.android.repository.api.SettingsController;
import com.android.tools.idea.sdk.StudioSettingsController;
import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.AsyncProcessIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Panel that shows the current {@link RepositorySource}s.
 */
public class UpdateSitesPanel {
  private JPanel myRootPanel;
  private TableView myUpdateSitesTable;
  @SuppressWarnings("unused") private JPanel mySourcesPanel;
  private JPanel mySourcesLoadingPanel;
  @SuppressWarnings("unused") private AsyncProcessIcon mySourcesLoadingIcon;
  private JCheckBox myForceHttp;
  private JCheckBox myDisableSdkPatches;
  private SourcesTableModel mySourcesTableModel;
  private static SettingsController ourSettingsController = StudioSettingsController.getInstance();

  public UpdateSitesPanel(@NotNull Runnable refreshCallback) {
    init(refreshCallback);
  }

  // IJ tries to be smart and generates weird code if this isn't a separate method and is instead in the constructor...
  private void init(@NotNull Runnable refreshCallback) {
    mySourcesTableModel.setRefreshCallback(refreshCallback);
  }

  private void createUIComponents() {
    mySourcesLoadingIcon = new AsyncProcessIcon(CommonBundle.getLoadingTreeNodeText());
    mySourcesTableModel = new SourcesTableModel(() -> mySourcesLoadingPanel.setVisible(true),
                                                () -> mySourcesLoadingPanel.setVisible(false), ModalityState.current());
    myUpdateSitesTable = new TableView<>(mySourcesTableModel);
    ToolbarDecorator userDefinedDecorator = ToolbarDecorator.createDecorator(myUpdateSitesTable);
    mySourcesPanel = addExtraActions(userDefinedDecorator).createPanel();
    SdkUpdaterConfigPanel.setTableProperties(myUpdateSitesTable, null);
  }

  private ToolbarDecorator addExtraActions(final ToolbarDecorator decorator) {
    return decorator.setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        mySourcesTableModel.editRow(myUpdateSitesTable.getSelectedRow());
      }
    }).setEditActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(@NotNull AnActionEvent e) {
        return myUpdateSitesTable.getSelectedRowCount() == 1 && mySourcesTableModel.isEditable(myUpdateSitesTable.getSelectedRow());
      }
    }).setAddActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(@NotNull AnActionEvent e) {
        return mySourcesTableModel.isEditable();
      }
    }).addExtraAction(new AnActionButton(AndroidBundle.messagePointer("action.AnActionButton.update.sites.text.select.all"), AllIcons.Actions.Selectall) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        mySourcesTableModel.setAllEnabled(true);
      }

      @Override
      public boolean isEnabled() {
        return mySourcesTableModel.hasEditableRows();
      }
    }).addExtraAction(new AnActionButton(AndroidBundle.messagePointer("action.AnActionButton.update.sites.text.deselect.all"), AllIcons.Actions.Unselectall) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        mySourcesTableModel.setAllEnabled(false);
      }

      @Override
      public boolean isEnabled() {
        return mySourcesTableModel.hasEditableRows();
      }
    }).setMoveDownAction(null).setMoveUpAction(null).setRemoveActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(@NotNull AnActionEvent e) {
        if (myUpdateSitesTable.getSelectedRowCount() < 1) {
          return false;
        }
        for (int i : myUpdateSitesTable.getSelectedRows()) {
          if (!mySourcesTableModel.isEditable(i)) {
            return false;
          }
        }
        return true;
      }
    });
  }

  public boolean isModified() {
    return mySourcesTableModel.isSourcesModified()
           || ourSettingsController.getForceHttp() != myForceHttp.isSelected()
           || ourSettingsController.getDisableSdkPatches() != myDisableSdkPatches.isSelected();
  }

  public void reset() {
    mySourcesTableModel.reset();
    myForceHttp.setSelected(ourSettingsController.getForceHttp());
    myDisableSdkPatches.setSelected(ourSettingsController.getDisableSdkPatches());
  }

  public void setConfigurable(@NotNull SdkUpdaterConfigurable configurable) {
    mySourcesTableModel.setConfigurable(configurable);
  }

  public void save() {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        mySourcesTableModel.save(ProgressManager.getInstance().getProgressIndicator());
        ourSettingsController.setForceHttp(myForceHttp.isSelected());
        ourSettingsController.setDisableSdkPatches(myDisableSdkPatches.isSelected());
      }
    }, "Saving Sources", false, null, myRootPanel);
  }
}
