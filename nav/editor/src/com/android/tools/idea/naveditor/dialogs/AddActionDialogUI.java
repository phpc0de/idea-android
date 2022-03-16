/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.naveditor.dialogs;

import com.google.common.annotations.VisibleForTesting;
import com.android.tools.idea.common.model.NlComponent;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;

/**
 * This is just a container for the fields in the add action dialog form. The logic is all in {@link AddActionDialog}
 */
@VisibleForTesting
public class AddActionDialogUI {
  JComboBox<NlComponent> myFromComboBox;
  JComboBox<AddActionDialog.DestinationListEntry> myDestinationComboBox;
  JComboBox<ValueWithDisplayString> myEnterComboBox;
  JComboBox<ValueWithDisplayString> myExitComboBox;
  JComboBox<AddActionDialog.DestinationListEntry> myPopToComboBox;
  JCheckBox myInclusiveCheckBox;
  JComboBox<ValueWithDisplayString> myPopEnterComboBox;
  JComboBox<ValueWithDisplayString> myPopExitComboBox;
  JCheckBox mySingleTopCheckBox;
  JPanel myContentPanel;
  JBTextField myIdTextField;
}
