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
package com.android.tools.profilers

import com.android.tools.adtui.model.FakeTimer

interface WithFakeTimer {
  val timer: FakeTimer

  fun tickOneSec() = tick(FakeTimer.ONE_SECOND_IN_NS)
  fun tick(ns: Long) = timer.tick(ns)

  fun <T> tickOneSecThen(get: () -> T) = tickThen(FakeTimer.ONE_SECOND_IN_NS, get)
  fun <T> tickThen(ns: Long, get: () -> T): T {
    tick(ns)
    return get()
  }
}