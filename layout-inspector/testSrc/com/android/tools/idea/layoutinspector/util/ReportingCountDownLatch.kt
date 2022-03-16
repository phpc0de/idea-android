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
package com.android.tools.idea.layoutinspector.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A [CountDownLatch] that throws an error on timeout.
 */
class ReportingCountDownLatch(count: Int): CountDownLatch(count) {
  private var failure: Throwable? = null

  override fun await() = error("Please specify timeout")

  override fun await(timeout: Long, unit: TimeUnit): Boolean {
    val success = super.await(timeout, unit)
    failure?.let { throw it }
    if (!success) {
      error("Timeout")
    }
    return true
  }

  /**
   * Run [runnable] on the EDT and forward exceptions to [await].
   */
  fun runInEdt(runnable: () -> Unit) {
    com.intellij.openapi.application.runInEdt {
      try {
        runnable()
      }
      catch (ex: Throwable) {
        failure = ex
      }
    }
  }
}
