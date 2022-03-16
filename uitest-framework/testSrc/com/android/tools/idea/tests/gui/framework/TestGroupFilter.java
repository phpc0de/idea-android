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
package com.android.tools.idea.tests.gui.framework;

import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

class TestGroupFilter extends Filter {

  @NotNull private final TestGroup testGroup;

  TestGroupFilter(@NotNull TestGroup testGroup) {
    this.testGroup = testGroup;
  }

  @Override
  public boolean shouldRun(Description description) {
    return (description.isTest() && description.getTestClass() != null && testGroupOf(description) == testGroup)
           || description.getChildren().stream().anyMatch(this::shouldRun);
  }

  @NotNull
  private static TestGroup testGroupOf(Description description) {
    RunIn methodAnnotation = description.getAnnotation(RunIn.class);
    return (methodAnnotation != null) ? methodAnnotation.value() : testGroupOf(description.getTestClass());
  }

  @NotNull
  private static TestGroup testGroupOf(@NotNull Class<?> testClass) {
    RunIn classAnnotation = testClass.getAnnotation(RunIn.class);
    return (classAnnotation != null) ? classAnnotation.value() : TestGroup.DEFAULT;
  }

  @Override
  public String describe() {
    return TestGroupFilter.class.getSimpleName() + " for " + testGroup;
  }
}
