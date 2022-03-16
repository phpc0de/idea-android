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
package com.android.tools.idea.emulator

import com.android.tools.idea.npw.assetstudio.roundToInt
import java.awt.Color

/**
 * Interpolates between two colors.
 */
fun interpolate(start: Color, end: Color, fraction: Double): Color {
  return Color(interpolate(start.red, end.red, fraction).coerceIn(0, 255),
               interpolate(start.green, end.green, fraction).coerceIn(0, 255),
               interpolate(start.blue, end.blue, fraction).coerceIn(0, 255),
               interpolate(start.alpha, end.alpha, fraction).coerceIn(0, 255))
}

/**
 * Interpolates between two integers.
 */
private fun interpolate(start: Int, end: Int, fraction: Double): Int {
  return start + ((end - start) * fraction).roundToInt()
}
