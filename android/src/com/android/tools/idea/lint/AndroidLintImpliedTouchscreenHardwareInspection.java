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
package com.android.tools.idea.lint;

import com.android.tools.idea.lint.common.AndroidLintInspectionBase;
import com.android.tools.idea.lint.common.LintIdeQuickFix;
import com.android.tools.lint.checks.AndroidTvDetector;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

public class AndroidLintImpliedTouchscreenHardwareInspection extends AndroidLintInspectionBase {
  public AndroidLintImpliedTouchscreenHardwareInspection() {
    super(AndroidBundle.message("android.lint.inspections.implied.touchscreen.hardware"), AndroidTvDetector.IMPLIED_TOUCHSCREEN_HARDWARE);
  }

  @NotNull
  @Override
  public LintIdeQuickFix[] getQuickFixes(@NotNull String message) {
    return new LintIdeQuickFix[]{new AddUsesFeatureQuickFix(AndroidTvDetector.HARDWARE_FEATURE_TOUCHSCREEN)};
  }
}
