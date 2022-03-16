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
package com.android.tools.utp

import com.google.testing.platform.proto.api.core.TestCaseProto.TestCase
import com.google.testing.platform.proto.api.core.TestResultProto.TestResult
import com.google.testing.platform.proto.api.core.TestSuiteResultProto.TestSuiteMetaData
import com.google.testing.platform.proto.api.core.TestSuiteResultProto.TestSuiteResult

/**
 * An interface to receive test progress from [TaskOutputProcessor].
 */
interface TaskOutputProcessorListener {
  /**
   * Called when a test suite execution is started.
   */
  fun onTestSuiteStarted(testSuite: TestSuiteMetaData)

  /**
   * Called when a test case execution is started.
   */
  fun onTestCaseStarted(testCase: TestCase)

  /**
   * Called when a test case execution is finished.
   */
  fun onTestCaseFinished(testCaseResult: TestResult)

  /**
   * Called when a test suite execution is finished.
   */
  fun onTestSuiteFinished(testSuiteResult: TestSuiteResult)
}