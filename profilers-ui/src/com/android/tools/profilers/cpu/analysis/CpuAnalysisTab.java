/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.profilers.cpu.analysis;

import com.android.tools.adtui.common.StudioColorsKt;
import com.android.tools.profilers.StudioProfilersView;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * The view component for {@link CpuAnalysisTabModel}. Each model can have its own unique data type as such its is the responsibility
 * of the child view type to do proper type checking. An example of a child tab is the {@link CpuAnalysisSummaryTab}.
 */
public class CpuAnalysisTab<T extends CpuAnalysisTabModel> extends JComponent {
  @NotNull private final StudioProfilersView myProfilersView;
  @NotNull private final T myModel;

  public CpuAnalysisTab(@NotNull StudioProfilersView profilersView, @NotNull T model) {
    myProfilersView = profilersView;
    myModel = model;
    setBackground(StudioColorsKt.getPrimaryContentBackground());
  }

  @NotNull
  public StudioProfilersView getProfilersView() {
    return myProfilersView;
  }

  @NotNull
  public T getModel() {
    return myModel;
  }
}
