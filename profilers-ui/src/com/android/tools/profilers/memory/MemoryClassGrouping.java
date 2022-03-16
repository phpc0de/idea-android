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
package com.android.tools.profilers.memory;

import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.profilers.ProfilerCombobox;
import com.android.tools.profilers.ProfilerComboboxCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import org.jetbrains.annotations.NotNull;

public class MemoryClassGrouping extends AspectObserver {
  @NotNull private final MemoryCaptureSelection mySelection;
  @NotNull private final JComboBox<ClassGrouping> myComboBox;

  public MemoryClassGrouping(@NotNull MemoryCaptureSelection selection) {
    mySelection = selection;

    mySelection.getAspect().addDependency(this).onChange(CaptureSelectionAspect.CLASS_GROUPING, this::groupingChanged);
    myComboBox = new ProfilerCombobox<>(mySelection.getClassGroupingModel());
    myComboBox.setRenderer(new ProfilerComboboxCellRenderer<ClassGrouping>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList<? extends ClassGrouping> list,
                                           ClassGrouping value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        append(value.getLabel());
      }
    });
    myComboBox.addActionListener(e -> {
      Object item = myComboBox.getSelectedItem();
      if (item instanceof ClassGrouping) {
        mySelection.setClassGrouping((ClassGrouping)item);
      }
    });
  }

  @NotNull
  JComboBox<ClassGrouping> getComponent() {
    return myComboBox;
  }

  public void groupingChanged() {
    if (myComboBox.getSelectedItem() != mySelection.getClassGrouping()) {
      myComboBox.setSelectedItem(mySelection.getClassGrouping());
    }
  }
}
