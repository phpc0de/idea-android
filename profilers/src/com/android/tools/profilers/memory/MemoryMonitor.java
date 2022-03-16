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

import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.TooltipModel;
import com.android.tools.adtui.model.axis.AxisComponentModel;
import com.android.tools.adtui.model.axis.ClampedAxisComponentModel;
import com.android.tools.adtui.model.formatter.BaseAxisFormatter;
import com.android.tools.adtui.model.formatter.MemoryAxisFormatter;
import com.android.tools.adtui.model.legend.Legend;
import com.android.tools.adtui.model.legend.LegendComponentModel;
import com.android.tools.adtui.model.legend.SeriesLegend;
import com.android.tools.profilers.ProfilerMonitor;
import com.android.tools.profilers.StudioProfilers;
import org.jetbrains.annotations.NotNull;

public class MemoryMonitor extends ProfilerMonitor {

  @NotNull
  private final ClampedAxisComponentModel myMemoryAxis;

  private static final BaseAxisFormatter MEMORY_AXIS_FORMATTER = new MemoryAxisFormatter(1, 2, 5);
  private final MemoryUsage myMemoryUsage;
  private final MemoryLegend myMemoryLegend;
  private MemoryLegend myTooltipLegend;

  public MemoryMonitor(@NotNull StudioProfilers profilers) {
    super(profilers);
    myMemoryUsage = new MemoryUsage(profilers);

    myMemoryAxis = new ClampedAxisComponentModel.Builder(myMemoryUsage.getMemoryRange(), MEMORY_AXIS_FORMATTER).build();
    myMemoryLegend = new MemoryLegend(myMemoryUsage, getTimeline().getDataRange());
    myTooltipLegend = new MemoryLegend(myMemoryUsage, getTimeline().getTooltipRange());
  }

  @Override
  public String getName() {
    return "MEMORY";
  }

  @Override
  public TooltipModel buildTooltip() {
    return new MemoryMonitorTooltip(this);
  }

  @Override
  public void enter() {
    myProfilers.getUpdater().register(myMemoryUsage);
    myProfilers.getUpdater().register(myMemoryAxis);
  }

  @Override
  public void exit() {
    myProfilers.getUpdater().unregister(myMemoryUsage);
    myProfilers.getUpdater().unregister(myMemoryAxis);
    myProfilers.removeDependencies(this);
  }

  @Override
  public void expand() {
    myProfilers.setStage(new MainMemoryProfilerStage(myProfilers));
  }

  @NotNull
  public AxisComponentModel getMemoryAxis() {
    return myMemoryAxis;
  }

  public MemoryUsage getMemoryUsage() {
    return myMemoryUsage;
  }

  public MemoryLegend getMemoryLegend() {
    return myMemoryLegend;
  }

  public MemoryLegend getTooltipLegend() {
    return myTooltipLegend;
  }

  public static class MemoryLegend extends LegendComponentModel {

    @NotNull
    private final SeriesLegend myTotalLegend;

    public MemoryLegend(@NotNull MemoryUsage usage, @NotNull Range range) {
      super(range);
      myTotalLegend = new SeriesLegend(usage.getTotalMemorySeries(), MEMORY_AXIS_FORMATTER, range);
      add(myTotalLegend);
    }

    @NotNull
    public Legend getTotalLegend() {
      return myTotalLegend;
    }
  }
}