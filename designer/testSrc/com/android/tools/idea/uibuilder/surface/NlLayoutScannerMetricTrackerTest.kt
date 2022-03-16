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
package com.android.tools.idea.uibuilder.surface

import com.android.SdkConstants
import com.android.tools.idea.common.analytics.CommonNopTracker
import com.android.tools.idea.common.analytics.CommonUsageTracker
import com.android.tools.idea.common.error.Issue
import com.android.tools.idea.common.error.IssueSource
import com.android.tools.idea.uibuilder.LayoutTestCase
import com.android.tools.idea.validator.ValidatorData
import com.google.wireless.android.sdk.stats.LayoutEditorEvent
import com.intellij.lang.annotation.HighlightSeverity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
class NlLayoutScannerMetricTrackerTest : LayoutTestCase() {

  @Mock
  lateinit var mockSurface: NlDesignSurface

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    (CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker).resetLastTrackedEvent()
  }

  @Test
  fun trackIssueNoIssues() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val nlAtfIssues = setOf<Issue>()
    val renderResultMetricData = RenderResultMetricData(1, 1, 1)

    tracker.trackIssues(nlAtfIssues, renderResultMetricData)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertNull(usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackIssueNoNlAtfIssues() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val nlAtfIssues = setOf<Issue>(TestIssue())
    val renderResultMetricData = RenderResultMetricData(1, 1, 1)

    tracker.trackIssues(nlAtfIssues, renderResultMetricData)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertNull(usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackIssues() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val nlAtfIssues = setOf(createTestNlAtfIssue())
    val renderResultMetricData = RenderResultMetricData(1, 1, 1)

    tracker.trackIssues(nlAtfIssues, renderResultMetricData)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertEquals(LayoutEditorEvent.LayoutEditorEventType.ATF_AUDIT_RESULT, usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackExpandedNotAtfIssue() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    tracker.trackFirstExpanded(TestIssue())

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertNull(usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackExpandedAlreadyContained() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val issue = createTestNlAtfIssue()
    tracker.expanded.add(issue)

    tracker.trackFirstExpanded(issue)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertNull(usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackExpanded() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val issue = createTestNlAtfIssue()
    tracker.trackFirstExpanded(issue)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertEquals(LayoutEditorEvent.LayoutEditorEventType.ATF_AUDIT_RESULT, usageTracker.lastTrackedEvent)
    assertTrue(tracker.expanded.contains(issue))
  }

  @Test
  fun trackIgnoreButtonClicked() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val issue = ScannerTestHelper.createTestIssueBuilder().build()
    tracker.trackIgnoreButtonClicked(issue)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertEquals(LayoutEditorEvent.LayoutEditorEventType.IGNORE_ATF_RESULT, usageTracker.lastTrackedEvent)
  }

  @Test
  fun trackApplyFixButtonClicked() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val fixDescription = "Set this item's android:textColor to #FFFFFF"
    val viewAttribute = ValidatorData.ViewAttribute(SdkConstants.ANDROID_URI, "android", "textColor")
    val setAttributeFix = ValidatorData.SetViewAttributeFix(viewAttribute, "#FFFFFF", fixDescription)
    val issue = ScannerTestHelper.createTestIssueBuilder(setAttributeFix).build()
    tracker.trackApplyFixButtonClicked(issue)

    val usageTracker = CommonUsageTracker.getInstance(mockSurface) as CommonNopTracker
    assertEquals(LayoutEditorEvent.LayoutEditorEventType.APPLY_ATF_FIX, usageTracker.lastTrackedEvent)
  }

  @Test
  fun clear() {
    val tracker = NlLayoutScannerMetricTracker(mockSurface)
    val issue = createTestNlAtfIssue()
    tracker.expanded.add(issue)

    tracker.clear()
    assertEmpty(tracker.expanded)
  }

  private fun createTestNlAtfIssue(): NlAtfIssue {
    val issue: ValidatorData.Issue = ScannerTestHelper.createTestIssueBuilder().build()
    return NlAtfIssue(issue, IssueSource.NONE)
  }

  private class TestIssue : Issue() {
    override val summary: String
      get() = ""
    override val description: String
      get() = ""
    override val severity: HighlightSeverity
      get() = HighlightSeverity.ERROR
    override val source: IssueSource
      get() = IssueSource.NONE
    override val category: String
      get() = ""
  }
}