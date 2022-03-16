/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.idea.logcat;

import com.google.common.annotations.VisibleForTesting;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatLongEpochMessageParser;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.logcat.LogCatMessageParser;
import com.android.tools.idea.logcat.AndroidLogcatService.LogcatListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.android.util.AndroidOutputReceiver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * An {@link AndroidOutputReceiver} which receives output from logcat and processes each line,
 * searching for callstacks and reformatting the final output before it is printed to the
 * logcat console.
 *
 * <p>This class expects the logcat format to be 'logcat -v long' (which prints out a header and then
 * 1+ lines of log text below, for each log message).
 */
public final class AndroidLogcatReceiver extends AndroidOutputReceiver implements Disposable {
  /**
   * Prefix to use for stack trace lines.
   */
  private static final String STACK_TRACE_LINE_PREFIX = StringUtil.repeatSymbol(' ', 4);

  /**
   * Prefix to use for the stack trace "Caused by:" lines.
   */
  private static final String STACK_TRACE_CAUSE_LINE_PREFIX = Character.toString(' ');

  private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r", Pattern.LITERAL);

  private final LogCatMessageParser myLongEpochParser;
  private final LogCatMessageParser myLongParser;
  private final IDevice myDevice;
  private final StackTraceExpander myStackTraceExpander;
  private final LogcatListener myLogcatListener;

  /**
   * We don't always want to add a newline when we get one, as we can't tell if it came from the
   * user or from logcat. We'll flush any enqueued newlines to the log if we get more context that
   * verifies the newlines came from a user.
   */
  private int myDelayedNewlineCount;

  @Nullable private LogCatHeader myActiveHeader;
  private int myLineIndex;
  private volatile boolean myCanceled;

  AndroidLogcatReceiver(@NotNull IDevice device, @NotNull LogcatListener listener) {
    myLongEpochParser = new LogCatLongEpochMessageParser();
    myLongParser = new LogCatMessageParser();
    myDevice = device;
    myStackTraceExpander = new StackTraceExpander(STACK_TRACE_LINE_PREFIX, STACK_TRACE_CAUSE_LINE_PREFIX);
    myLogcatListener = listener;
  }

  @Override
  public void processNewLine(@NotNull String line) {
    // Really, the user's log should never put any system characters in it ever - that will cause
    // it to get filtered by our strict regex patterns (see AndroidLogcatFormatter). The reason
    // this might happen in practice is due to a bug where either adb or logcat (not sure which)
    // is too aggressive about converting \n's to \r\n's, including those that are quoted. This
    // means that a user's log, if it uses \r\n itself, is converted to \r\r\n. Then, when
    // MultiLineReceiver, which expects valid input, strips out \r\n, it leaves behind an extra \r.
    //
    // Unfortunately this isn't a case where we can fix the root cause because adb and logcat are
    // both external to Android Studio. In fact, the latest adb/logcat versions have already fixed
    // this issue! But we still need to run properly with older versions. Also, putting this fix in
    // MultiLineReceiver isn't right either because it is used for more than just receiving logcat.
    line = CARRIAGE_RETURN.matcher(line).replaceAll("");

    if (line.isEmpty()) {
      myDelayedNewlineCount++;
      return;
    }

    LogCatHeader header = myLongEpochParser.processLogHeader(line, myDevice);

    if (header == null) {
      header = myLongParser.processLogHeader(line, myDevice);
    }

    if (header != null) {
      myStackTraceExpander.reset();
      myActiveHeader = header;
      myLineIndex = 0;
      // Intentionally drop any trailing newlines once we hit a new header. Usually, logcat
      // separates log entries with a single newline but sometimes it outputs more than one. As we
      // can't know which is user newlines vs. system newlines, just drop all of them.
      myDelayedNewlineCount = 0;
    }
    else if (myActiveHeader != null) {
      if (myDelayedNewlineCount > 0 && myLineIndex == 0) {
        // Note: Since we trim trailing newlines, we trim leading newlines too. Most users won't
        // use them intentionally and they don't look great, anyway.
        myDelayedNewlineCount = 0;
      }
      else {
        processAnyDelayedNewlines(myActiveHeader);
      }
      for (String processedLine : myStackTraceExpander.process(line)) {
        notifyLine(myActiveHeader, processedLine);
      }
    }
  }

  // This method is package protected so other Logcat components can feed receiver processed log lines if they need to
  void notifyLine(@NotNull LogCatHeader header, @NotNull String line) {
    myLogcatListener.onLogLineReceived(new LogCatMessage(header, line));
    myLineIndex++;
  }

  private void processAnyDelayedNewlines(@NotNull LogCatHeader header) {
    if (myDelayedNewlineCount == 0) {
      return;
    }
    for (int i = 0; i < myDelayedNewlineCount; i++) {
      notifyLine(header, "");
    }
    myDelayedNewlineCount = 0;
  }

  @Override
  public boolean isCancelled() {
    return myCanceled;
  }

  @Override
  public void dispose() {
    cancel();
  }

  public void cancel() {
    myCanceled = true;
  }

  @VisibleForTesting
  int getDelayedNewlineCount() {
    return myDelayedNewlineCount;
  }
}
