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
import com.android.tools.lint.checks.InefficientWeightDetector;
import org.jetbrains.android.util.AndroidBundle;

public class AndroidLintSuspicious0dpInspection extends AndroidLintInspectionBase {
  public AndroidLintSuspicious0dpInspection() {
    super(AndroidBundle.message("android.lint.inspections.suspicious0dp"), InefficientWeightDetector.WRONG_0DP);
  }
}