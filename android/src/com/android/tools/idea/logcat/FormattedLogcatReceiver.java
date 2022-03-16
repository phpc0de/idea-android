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
package com.android.tools.idea.logcat;

import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import org.jetbrains.annotations.NotNull;

abstract class FormattedLogcatReceiver implements AndroidLogcatService.LogcatListener {
  private final AndroidLogcatFormatter myFormatter;
  private LogCatHeader myActiveHeader;

  FormattedLogcatReceiver(@NotNull AndroidLogcatFormatter formatter) {
    myFormatter = formatter;
  }

  @Override
  public final void onLogLineReceived(@NotNull LogCatMessage line) {
    LogCatHeader header = line.getHeader();

    // We want the if branch whenever logcat prints a header, even if it has the same value as the previous one. Check the reference values
    // (with !=) and not the object values (with equals) here because we get a new instance every time logcat prints a header.
    if (myActiveHeader != header) {
      myActiveHeader = header;
      receiveFormattedLogLine(myFormatter.formatMessageFull(header, line.getMessage()));
    }
    else {
      String message = AndroidLogcatFormatter.formatContinuation(line.getMessage());
      receiveFormattedLogLine(message);
    }
  }

  abstract void receiveFormattedLogLine(@NotNull String line);
}
