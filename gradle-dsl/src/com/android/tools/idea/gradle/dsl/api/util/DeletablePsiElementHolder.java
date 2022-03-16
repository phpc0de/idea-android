/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.gradle.dsl.api.util;

import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;

/**
 * This interface is implemented by Dsl models which can be marked for deletion, which is essentially all of them.
 */
public interface DeletablePsiElementHolder extends PsiElementHolder {
  /**
   * Marks this model for deletion, which when {@link GradleBuildModel#applyChanges()} is called, removes it and any associated
   * values from the Dsl build files.
   */
  void delete();
}
