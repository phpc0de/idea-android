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
package com.android.tools.inspectors.common.api.stacktrace;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CodeLocationTest {
  @Test
  public void testGetOuterClassName() {
    CodeLocation simpleClass = new CodeLocation.Builder("outerClass").build();
    assertThat(simpleClass.getOuterClassName()).isEqualTo("outerClass");

    CodeLocation innerClass = new CodeLocation.Builder("outerClass$innerClass").build();
    assertThat(innerClass.getOuterClassName()).isEqualTo("outerClass");

    CodeLocation anonymousClass = new CodeLocation.Builder("outerClass$innerClass$1").build();
    assertThat(anonymousClass.getOuterClassName()).isEqualTo("outerClass");
  }
}