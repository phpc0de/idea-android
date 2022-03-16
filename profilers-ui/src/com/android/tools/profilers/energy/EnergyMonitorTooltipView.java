// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.android.tools.profilers.energy;

import com.android.tools.adtui.LegendComponent;
import com.android.tools.adtui.LegendConfig;
import com.android.tools.profilers.ProfilerColors;
import com.android.tools.profilers.ProfilerMonitorTooltipView;
import com.android.tools.profilers.StageView;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public class EnergyMonitorTooltipView extends ProfilerMonitorTooltipView<EnergyMonitor> {
  public EnergyMonitorTooltipView(StageView parent, @NotNull EnergyMonitorTooltip tooltip) {
    super(tooltip.getMonitor());
  }

  @NotNull
  @Override
  protected JComponent createTooltip() {
    LegendComponent.Builder legendBuilder = new LegendComponent.Builder(getMonitor().getTooltipLegends());
    LegendComponent legend = legendBuilder.setVerticalPadding(0).setOrientation(LegendComponent.Orientation.VERTICAL).build();
    legend.configure(getMonitor().getTooltipLegends().getLegends().get(0),
                     new LegendConfig(LegendConfig.IconType.BOX, ProfilerColors.ENERGY_USAGE));
    return legend;
  }
}
