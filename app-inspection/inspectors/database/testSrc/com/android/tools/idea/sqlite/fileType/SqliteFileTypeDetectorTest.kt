/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.sqlite.fileType

import com.android.tools.idea.sqlite.DatabaseInspectorFlagController
import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory

class SqliteFileTypeDetectorTest : LightPlatformTestCase() {
  private lateinit var mySqliteUtil: SqliteTestUtil
  private var myFeaturePreviousEnabled: Boolean = false
  private var myFileSupportPreviousEnabled: Boolean = false

  override fun setUp() {
    super.setUp()
    mySqliteUtil = SqliteTestUtil(IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture())
    mySqliteUtil.setUp()
    myFeaturePreviousEnabled = DatabaseInspectorFlagController.enableFeature(true)
    myFileSupportPreviousEnabled = DatabaseInspectorFlagController.enableOpenFile(true)
  }

  override fun tearDown() {
    try {
      mySqliteUtil.tearDown()
      DatabaseInspectorFlagController.enableFeature(myFeaturePreviousEnabled)
      DatabaseInspectorFlagController.enableOpenFile(myFileSupportPreviousEnabled)
    }
    finally {
      super.tearDown()
    }
  }

  fun testSqliteFileDetectionDoesNotWorkWhenFlagIsOff() {
    // Prepare
    DatabaseInspectorFlagController.enableOpenFile(false)
    val file = mySqliteUtil.createTestSqliteDatabase()
    val detector = SqliteFileTypeDetector()
    val byteSequence = mySqliteUtil.createByteSequence(file, 4096)

    // Act
    val fileType = detector.detect(file, byteSequence, null)

    // Assert
    assertThat(fileType).isNull()
  }

  fun testSqliteFileDetection() {
    // Prepare
    val file = mySqliteUtil.createTestSqliteDatabase()
    val detector = SqliteFileTypeDetector()
    val byteSequence = mySqliteUtil.createByteSequence(file, 4096)

    // Act
    val fileType = detector.detect(file, byteSequence, null)

    // Assert
    assertThat(fileType).isNotNull()
  }

  fun testSqliteFileDetectionShortSequence() {
    // Prepare
    val file = mySqliteUtil.createTestSqliteDatabase()
    val detector = SqliteFileTypeDetector()
    // Note: 10 bytes is smaller than the Sqlite header
    val byteSequence = mySqliteUtil.createByteSequence(file, 10)

    // Act
    val fileType = detector.detect(file, byteSequence, null)

    // Assert
    assertThat(fileType).isNull()
  }

  fun testSqliteFileDetectionEmptyDatabase() {
    // Prepare
    val file = mySqliteUtil.createTestSqliteDatabase()
    val detector = SqliteFileTypeDetector()
    // Note: 10 bytes is smaller than the Sqlite header
    val byteSequence = mySqliteUtil.createByteSequence(file, 10)

    // Act
    val fileType = detector.detect(file, byteSequence, null)

    // Assert
    assertThat(fileType).isNull()
  }

  fun testRandomBinaryFileDetection() {
    // Prepare
    val file = mySqliteUtil.createTempBinaryFile(30000)
    val detector = SqliteFileTypeDetector()
    val byteSequence = mySqliteUtil.createByteSequence(file, 4096)

    // Act
    val fileType = detector.detect(file, byteSequence, null)

    // Assert
    assertThat(fileType).isNull()
  }
}
