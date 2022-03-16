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
package com.android.tools.idea.updater.configure;

import com.android.utils.HtmlBuilder;
import com.android.utils.Pair;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class SdkUpdaterConfigurableTest {
  private static final long USABLE_DISK_SPACE = 1024*1024*1024L; // 1GB
  private static final String SDK_ROOT_PATH = "/sdk";

  private static final String DISK_USAGE_HTML_TEMPLATE =
    "Disk usage:\n" +
    "<DL><DD>-&NBSP;Disk space that will be freed: %1$s" +
    "<DD>-&NBSP;Estimated download size: %2$s" +
    "<DD>-&NBSP;Estimated disk space to be additionally occupied on SDK partition after installation: %3$s" +
    "<DD>-&NBSP;Currently available disk space in SDK root (" + SDK_ROOT_PATH + "): %4$s</DL>";

  private static final String DISK_USAGE_HTML_TEMPLATE_WITHOUT_SPACE_TO_FREE_UP =
    "Disk usage:\n" +
    "<DL><DD>-&NBSP;Estimated download size: %1$s" +
    "<DD>-&NBSP;Estimated disk space to be additionally occupied on SDK partition after installation: %2$s" +
    "<DD>-&NBSP;Currently available disk space in SDK root (" + SDK_ROOT_PATH + "): %3$s</DL>";

  private static final String DISK_USAGE_HTML_TEMPLATE_WITHOUT_DOWNLOADS =
    "Disk usage:\n" +
    "<DL><DD>-&NBSP;Disk space that will be freed: %1$s</DL>";

  private static final String WARNING_HTML =
    "<FONT color=\"#ff0000\"><B>WARNING: There might be insufficient disk space to perform this operation. " +
    "</B><BR/><BR/>Estimated disk usage is presented below. Consider freeing up more disk space before proceeding. </FONT><BR/><BR/>";

  private File mySdkRoot;

  @Before
  public void setUp() throws Exception {
    mySdkRoot = Mockito.mock(File.class);
    when(mySdkRoot.getUsableSpace()).thenReturn(USABLE_DISK_SPACE);
    when(mySdkRoot.getAbsolutePath()).thenReturn(SDK_ROOT_PATH);
  }

  @Test
  public void testDiskSpaceMessagesFullAndPatchAndUninstall() throws Exception {
    final long fullInstallationDownloadSize = 70 * 1024 * 1024L + 42; // +42 just to avoid "nice" numbers
    final long patchesDownloadSize = 20 * 1024 * 1024L + 42;
    final long spaceToBeFreedUp = 10 * 1024 * 1024L + 42;

    Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                          patchesDownloadSize, spaceToBeFreedUp);
    assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE, "10.0 MB", "90.0 MB", "290.0 MB", "1.0 GB"),
                 messages.getFirst().getHtml());
    assertNull(messages.getSecond());
  }

  @Test
  public void testDiskSpaceMessagesFullAndUninstall() throws Exception {
    final long fullInstallationDownloadSize = 70 * 1024 * 1024L + 42;
    final long patchesDownloadSize = 0;
    final long spaceToBeFreedUp = 10 * 1024 * 1024L + 42;

    Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                          patchesDownloadSize, spaceToBeFreedUp);
    assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE, "10.0 MB", "70.0 MB", "270.0 MB", "1.0 GB"),
                 messages.getFirst().getHtml());
    assertNull(messages.getSecond());
  }

  @Test
  public void testDiskSpaceMessagesPatchAndUninstall() throws Exception {
    final long fullInstallationDownloadSize = 0;
    final long patchesDownloadSize = 20 * 1024 * 1024L + 42;
    final long spaceToBeFreedUp = 9 * 1024 * 1024L + 42;

    Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                          patchesDownloadSize, spaceToBeFreedUp);
    assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE, "9.0 MB", "20.0 MB", "11.0 MB", "1.0 GB"),
                 messages.getFirst().getHtml());
    assertNull(messages.getSecond());
  }

  @Test
  public void testDiskSpaceMessagesPatchAndUninstallWhenInsufficientSpace() throws Exception {
    final long fullInstallationDownloadSize = 0;
    final long patchesDownloadSize = 1200 * 1024 * 1024L + 42;
    final long spaceToBeFreedUp = 9 * 1024 * 1024L + 42;

    Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                          patchesDownloadSize, spaceToBeFreedUp);
    assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE, "9.0 MB", "1.2 GB", "1.2 GB", "1.0 GB"),
                 messages.getFirst().getHtml());
    assertEquals(WARNING_HTML, messages.getSecond().getHtml());
  }

  @Test
  public void testDiskSpaceMessagesPatchWhenInsufficientSpace() throws Exception {
      final long fullInstallationDownloadSize = 0;
      final long patchesDownloadSize = 1200*1024*1024L + 42;
      final long spaceToBeFreedUp = 0;

      Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                            patchesDownloadSize, spaceToBeFreedUp);
      assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE_WITHOUT_SPACE_TO_FREE_UP, "1.2 GB", "1.2 GB", "1.0 GB"),
                   messages.getFirst().getHtml());
      assertEquals(WARNING_HTML, messages.getSecond().getHtml());
  }

  @Test
  public void testDiskSpaceMessagesUninstallOnly() throws Exception {
      final long fullInstallationDownloadSize = 0;
      final long patchesDownloadSize = 0;
      final long spaceToBeFreedUp = 800*1024*1024L + 42;

      Pair<HtmlBuilder, HtmlBuilder> messages = SdkUpdaterConfigurable.getDiskUsageMessages(mySdkRoot, fullInstallationDownloadSize,
                                                                                            patchesDownloadSize, spaceToBeFreedUp);
      assertEquals(String.format(DISK_USAGE_HTML_TEMPLATE_WITHOUT_DOWNLOADS, "800.0 MB"),
                   messages.getFirst().getHtml());
      assertNull(messages.getSecond());
  }
}
