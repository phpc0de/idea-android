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

import com.android.tools.adtui.model.DefaultConfigurableDurationData;
import com.android.tools.profilers.memory.adapters.CaptureObject;
import org.jetbrains.annotations.NotNull;

public class CaptureDurationData<T extends CaptureObject> extends DefaultConfigurableDurationData {
  @NotNull private final CaptureEntry<T> myCaptureEntry;
  private final Class<? super T> myCaptureObjectType;

  public CaptureDurationData(long duration,
                             boolean selectableWhenUnspecifiedDuration,
                             boolean selectPartialRange,
                             @NotNull CaptureEntry<T> captureEntry) {
    this(duration, selectableWhenUnspecifiedDuration, selectPartialRange, captureEntry, CaptureObject.class);
  }

  /**
   * @param captureObjectType a subclass of CaptureObject that any capture object produced by `captureEntry`
   *                          is guaranteed to be an instance of
   */
  public CaptureDurationData(long duration,
                             boolean selectableWhenUnspecifiedDuration,
                             boolean selectPartialRange,
                             @NotNull CaptureEntry<T> captureEntry,
                             Class<? super T> captureObjectType) {
    super(duration, selectableWhenUnspecifiedDuration, selectPartialRange);
    myCaptureEntry = captureEntry;
    myCaptureObjectType = captureObjectType;
  }

  @NotNull
  public CaptureEntry<T> getCaptureEntry() {
    return myCaptureEntry;
  }

  /**
   * @return an upperbound of the capture object type.
   * The capture object produced by `getCaptureEntry()` is guaranteed to be a instance of this class.
   */
  public final Class<? super T> getCaptureObjectType() {
    return myCaptureObjectType;
  }
}
