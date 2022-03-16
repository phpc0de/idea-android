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
package com.android.tools.profilers.cpu.analysis;

import com.android.tools.adtui.model.Range;
import com.android.tools.profiler.proto.Cpu;
import org.jetbrains.annotations.NotNull;

/**
 * Analysis tab model for capture nodes.
 */
public class CaptureNodeAnalysisSummaryTabModel extends CpuAnalysisSummaryTabModel<CaptureNodeAnalysisModel> {
  @NotNull private final Cpu.CpuTraceType myTraceType;

  public CaptureNodeAnalysisSummaryTabModel(@NotNull Range captureRange, @NotNull Cpu.CpuTraceType traceType) {
    super(captureRange);
    myTraceType = traceType;
  }

  @NotNull
  @Override
  public String getLabel() {
    switch (myTraceType) {
      case ATRACE: // fall through
      case PERFETTO:
        return "Trace Event";
      case ART: // fall through
      case SIMPLEPERF:
      default:
        return "Stack Frame";
    }
  }

  @NotNull
  @Override
  public Range getSelectionRange() {
    Range selectionRange = new Range();
    // Find the smallest min and largest max of all nodes and return that as the selection range of multiple nodes.
    for (CaptureNodeAnalysisModel analysisModel : getDataSeries()) {
      if (analysisModel.getNodeRange().getMin() < selectionRange.getMin()) {
        selectionRange.setMin(analysisModel.getNodeRange().getMin());
      }
      if (analysisModel.getNodeRange().getMax() > selectionRange.getMax()) {
        selectionRange.setMax(analysisModel.getNodeRange().getMax());
      }
    }
    return selectionRange;
  }
}
