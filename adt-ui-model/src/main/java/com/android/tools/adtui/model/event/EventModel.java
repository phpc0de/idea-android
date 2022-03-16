/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.adtui.model.event;

import com.android.tools.adtui.model.AspectModel;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.RangedSeries;
import org.jetbrains.annotations.NotNull;

public class EventModel<E> extends AspectModel<EventModel.Aspect> {

  @NotNull
  private final RangedSeries<EventAction<E>> myRangedSeries;

  public EventModel(@NotNull RangedSeries<EventAction<E>> rangedSeries) {
    myRangedSeries = rangedSeries;
    myRangedSeries.getXRange().addDependency(this).onChange(Range.Aspect.RANGE, () -> changed(Aspect.EVENT));
  }

  @NotNull
  public RangedSeries<EventAction<E>> getRangedSeries() {
    return myRangedSeries;
  }

  public enum Aspect {
    EVENT
  }
}
