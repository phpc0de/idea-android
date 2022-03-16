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
package com.android.tools.idea.uibuilder

import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.sdklib.AndroidVersion
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

const val MOST_RECENT_API_LEVEL = AndroidVersion.VersionCodes.O_MR1
const val DEFAULT_MIN_API_LEVEL = AndroidVersion.VersionCodes.LOLLIPOP_MR1

/**
 * Extension of [LayoutTestCase] that provides support for using a minAPI in a test.
 *
 * This is achieved specifying a manifest file with minSdkVersion and targetSdkVersion specified.
 * The minSdkVersion can be specified by the test name:
 *    testXyzMinApi17   -   will cause a manifest with minSdkVersion set to 17.
 * If the test name has no MinApi specified in the test name, the default [DEFAULT_MIN_API_LEVEL] is used.
 * Alternatively a test can override the [setUpManifest] method to customize the manifest.
 */
abstract class MinApiLayoutTestCase(private val provideManifest: Boolean = true) : LayoutTestCase() {

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    if (provideManifest) {
      setUpManifest(myFixture, getTestName(true))
    }
  }

  override fun providesCustomManifest() = provideManifest

  companion object {

    @Throws(Exception::class)
    fun setUpManifest(fixture: CodeInsightTestFixture, testName: String? = null) {
      val minApiAsString = testName?.substringAfter("MinApi", DEFAULT_MIN_API_LEVEL.toString())
      val minApi = minApiAsString?.toIntOrNull() ?: DEFAULT_MIN_API_LEVEL
      val manifest = """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example">
              <uses-sdk android:minSdkVersion="$minApi"
                        android:targetSdkVersion="$MOST_RECENT_API_LEVEL"/>
      </manifest>
""".trimIndent()
      fixture.addFileToProject(FN_ANDROID_MANIFEST_XML, manifest)
    }
  }
}
