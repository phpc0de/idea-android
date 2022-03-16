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
package com.android.tools.idea.sdk.progress;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.ProgressIndicatorAdapter;
import com.intellij.openapi.diagnostic.Logger;

/**
 * {@link ProgressIndicator} that just wraps a {@link Logger}.
 * Does not have progress indicating functionality.
 */
public class StudioLoggerProgressIndicator extends ProgressIndicatorAdapter {
  private final Logger myLogger;

  public StudioLoggerProgressIndicator(Class c) {
    myLogger = Logger.getInstance(c);
  }

  @Override
  public void logWarning(@NonNull String s) {
    myLogger.warn(s);
  }

  @Override
  public void logWarning(@NonNull String s, @Nullable Throwable e) {
    myLogger.warn(s, e);
  }

  @Override
  public void logError(@NonNull String s) {
    myLogger.error(s);
  }

  @Override
  public void logError(@NonNull String s, @Nullable Throwable e) {
    myLogger.error(s, e);
  }

  @Override
  public void logInfo(@NonNull String s) {
    myLogger.info(s);
  }

  @Override
  public void logVerbose(@NonNull String s) {
    myLogger.debug(s);
  }
}
