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
package com.android.tools.idea.run;

/**
 * A simple enum to express whether this launch supports a single device or multiple.
 * This serves mostly to clarify code a bit and avoid confusing boolean switches.
 */
public enum DeviceCount {
  SINGLE,
  MULTIPLE;

  public boolean isSingle() {
    return this == SINGLE;
  }

  public boolean isMultiple() {
    return this == MULTIPLE;
  }

  public static DeviceCount fromBoolean(boolean supportMultipleDevices) {
    return supportMultipleDevices ? MULTIPLE : SINGLE;
  }
}
