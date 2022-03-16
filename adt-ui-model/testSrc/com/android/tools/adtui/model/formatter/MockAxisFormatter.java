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
package com.android.tools.adtui.model.formatter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

/**
 * A mock {@link BaseAxisFormatter} that works with length units.
 * e.g. mm, cm, m, km
 */
public final class MockAxisFormatter extends BaseAxisFormatter {
  private static final int[] MULTIPLIERS = new int[]{10, 100, 10};   //
  private static final int BASE = 10;
  private static final int MIN_INTERVAL = 1;
  private static final String[] UNITS = new String[]{"mm", "cm", "m"};
  private static final IntList BASE_FACTORS = IntArrayList.wrap(new int[]{10, 5, 1});

  public MockAxisFormatter(int maxMinorTicks, int maxMajorTicks, int switchThreshold) {
    super(maxMinorTicks, maxMajorTicks, switchThreshold);
  }

  public MockAxisFormatter(int maxMinorTicks, int maxMajorTicks, int switchThreshold, boolean hasSeparator) {
    super(maxMinorTicks, maxMajorTicks, switchThreshold, hasSeparator);
  }

  @Override
  protected int getNumUnits() {
    return UNITS.length;
  }

  @Override
  @NotNull
  protected String getUnit(int index) {
    return UNITS[index];
  }

  @Override
  protected int getUnitBase(int index) {
    return BASE;
  }

  @Override
  protected int getUnitMultiplier(int index) {
    return MULTIPLIERS[index];
  }

  @Override
  protected int getUnitMinimalInterval(int index) {
    return MIN_INTERVAL;
  }

  @Override
  @NotNull
  protected IntList getUnitBaseFactors(int index) {
    return BASE_FACTORS;
  }
}
