// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.android.tools.idea.logcat;

import com.android.ddmlib.Log;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import com.google.common.collect.ImmutableList;
import com.intellij.diagnostic.logging.LogConsoleBase;
import com.intellij.diagnostic.logging.LogFilter;
import com.intellij.diagnostic.logging.LogFilterListener;
import com.intellij.diagnostic.logging.LogFilterModel;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A filter which plugs into {@link LogConsoleBase} for custom logcat filtering.
 * This deliberately drops the custom pattern behaviour of LogFilterModel, replacing it with a new version that allows regex support.
 */
final class AndroidLogFilterModel extends LogFilterModel {

  private final List<LogFilterListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  /**
   * LogCat messages can span multiple lines, and sometimes you won't get a filter match until
   * you're a couple lines down. Therefore, we keep track of the part of the messages that came
   * before the current line and, if we get a match a few lines down, we include the previous part
   * as a prefix.
   */
  private final StringBuilder myMessageSoFar = new StringBuilder();

  @Nullable private LogCatHeader myPrevHeader;
  @Nullable private LogCatHeader myRejectBeforeHeader;

  /**
   * A regex which is tested against unprocessed log input. Contrast with
   * {@link #myConfiguredFilter} which, if non-null, does additional filtering on input after
   * it has been parsed and broken up into component parts.
   * This is normally set by the Android Monitor search bar.
   */
  @Nullable private Pattern myCustomPattern;
  private boolean myCustomApplicable = false; // True if myCustomPattern matches this message
  private boolean myConfiguredApplicable = false;  // True if the active filter matches this message

  @Nullable private AndroidLogcatFilter myConfiguredFilter;

  @NotNull private final ImmutableList<AndroidLogLevelFilter> myLogLevelFilters;
  @NotNull private final AndroidLogcatFormatter myFormatter;
  @NotNull private final AndroidLogcatPreferences myPreferences;

  AndroidLogFilterModel(@NotNull AndroidLogcatFormatter formatter, @NotNull AndroidLogcatPreferences preferences) {
    ImmutableList.Builder<AndroidLogLevelFilter> builder = ImmutableList.builder();
    for (Log.LogLevel logLevel : Log.LogLevel.values()) {
      builder.add(new AndroidLogLevelFilter(logLevel));
    }
    myLogLevelFilters = builder.build();
    myFormatter = formatter;
    myPreferences = preferences;
  }

  // Implemented because it is abstract in the parent, but the functionality is no longer used.
  @Override
  public String getCustomFilter() {
    return "";
  }

  /**
   * This is called to enable regular expression filtering of log messages.
   * Replaces the customFilter mechanism.
   */
  public void updateCustomPattern(@Nullable Pattern pattern) {
    myCustomPattern = pattern;
    fireTextFilterChange();
  }

  public final void updateLogcatFilter(@Nullable AndroidLogcatFilter filter) {
    myPreferences.TOOL_WINDOW_CONFIGURED_FILTER = filter != null ? filter.getName() : "";
    myConfiguredFilter = filter;
    fireTextFilterChange();
  }

  @Override
  public final void addFilterListener(LogFilterListener listener) {
    myListeners.add(listener);
  }

  @Override
  public final void removeFilterListener(LogFilterListener listener) {
    myListeners.remove(listener);
  }

  /**
   * Once called, any logcat messages processed with a timestamp older than our most recent one
   * will be filtered out from now on.
   * <p>
   * This is useful as a way to mark a time where you don't care about older messages. For example,
   * if you change your active filter and replay all logcat messages from the beginning, we will
   * skip over any that were originally reported before we called this method.
   * <p>
   * This can also act as a lightweight clear, in case clearing the logcat buffer on the device
   * fails for some reason (which does happen). If you call this method, clear the console text
   * even without clearing the device's logcat buffer, and reprocess all messages, old messages
   * will be skipped.
   */
  public void beginRejectingOldMessages() {
    if (myPrevHeader == null) {
      return; // Haven't received any messages yet, so nothing to filter
    }

    myRejectBeforeHeader = myPrevHeader;
  }


  private void fireTextFilterChange() {
    for (LogFilterListener listener : myListeners) {
      listener.onTextFilterChange();
    }
  }

  private void fireFilterChange(@NotNull LogFilter filter) {
    for (LogFilterListener listener : myListeners) {
      listener.onFilterStateChange(filter);
    }
  }

  @Override
  public final boolean isApplicable(String line) {
    // Not calling the super class version, it does not do what we want with regular expression matching
    if (myCustomPattern != null && !myCustomPattern.matcher(line).find()) return false;
    final LogFilter selectedLogLevelFilter = getSelectedLogLevelFilter();
    return selectedLogLevelFilter == null || selectedLogLevelFilter.isAcceptable(line);
  }


  // Checks if the log message (with header stripped) matches the active filter, if set. Note that
  // this should ONLY be called if myPrevHeader was already set (which is how the filter will test
  // against header information).
  private boolean isApplicableByConfiguredFilter(@NotNull String message) {
    if (myConfiguredFilter == null) {
      return true;
    }

    assert myPrevHeader != null; // We never call this method unless we already parsed a header
    return myConfiguredFilter
      .isApplicable(message, myPrevHeader.getTag(), myPrevHeader.getAppName(), myPrevHeader.getPid(), myPrevHeader.getLogLevel());
  }

  @Override
  public final List<? extends LogFilter> getLogFilters() {
    return myLogLevelFilters;
  }

  private final class AndroidLogLevelFilter extends LogFilter {
    final Log.LogLevel myLogLevel;

    private AndroidLogLevelFilter(Log.LogLevel logLevel) {
      super(StringUtil.capitalize(logLevel.getStringValue()));
      myLogLevel = logLevel;
    }

    @Override
    public boolean isAcceptable(String line) {
      return myPrevHeader != null && myPrevHeader.getLogLevel().getPriority() >= myLogLevel.getPriority();
    }
  }

  @Nullable
  private LogFilter getSelectedLogLevelFilter() {
    final String filterName = myPreferences.TOOL_WINDOW_LOG_LEVEL;
    if (filterName != null) {
      for (AndroidLogLevelFilter logFilter : myLogLevelFilters) {
        if (filterName.equals(logFilter.myLogLevel.getStringValue())) {
          return logFilter;
        }
      }
    }
    return null;
  }

  @Override
  public boolean isFilterSelected(LogFilter filter) {
    return filter == getSelectedLogLevelFilter();
  }

  @Override
  public void selectFilter(LogFilter filter) {
    if (!(filter instanceof AndroidLogLevelFilter)) {
      return;
    }
    String newFilterName = ((AndroidLogLevelFilter)filter).myLogLevel.getStringValue();
    if (!Objects.equals(newFilterName, myPreferences.TOOL_WINDOW_LOG_LEVEL)) {
      myPreferences.TOOL_WINDOW_LOG_LEVEL = newFilterName;
      fireFilterChange(filter);
    }
  }

  @Override
  public void processingStarted() {
    myPrevHeader = null;
    myRejectBeforeHeader = null;
    myCustomApplicable = false;
    myConfiguredApplicable = false;
    myMessageSoFar.setLength(0);
  }

  @Override
  @NotNull
  public final MyProcessingResult processLine(String line) {
    LogCatMessage message = myFormatter.tryParseMessage(line);
    String continuation = (message == null) ? AndroidLogcatFormatter.tryParseContinuation(line) : null;

    boolean validContinuation = continuation != null && myPrevHeader != null;
    if (message == null && !validContinuation) {
      return new MyProcessingResult(ProcessOutputTypes.STDOUT, false, null);
    }

    if (message != null) {
      myPrevHeader = message.getHeader();
      myCustomApplicable = isApplicable(line);
      myConfiguredApplicable = isApplicableByConfiguredFilter(message.getMessage());
      myMessageSoFar.setLength(0);
    }
    else {
      myCustomApplicable = myCustomApplicable || isApplicable(continuation);
      myConfiguredApplicable = myConfiguredApplicable || isApplicableByConfiguredFilter(continuation);
    }

    boolean isApplicable = myCustomApplicable && myConfiguredApplicable;
    if (isApplicable && myRejectBeforeHeader != null) {
      isApplicable = !myPrevHeader.getTimestamp().isBefore(myRejectBeforeHeader.getTimestamp());
    }

    if (!isApplicable) {
      // Even if this message isn't applicable right now, store it in case it becomes so later
      myMessageSoFar.append(line);
      myMessageSoFar.append('\n');
    }

    Key key = AndroidLogcatUtils.getProcessOutputType(myPrevHeader.getLogLevel());
    MyProcessingResult result = new MyProcessingResult(key, isApplicable, myMessageSoFar.toString());

    if (isApplicable) {
      myMessageSoFar.setLength(0); // Don't need anymore, already added as a prefix at this point
    }

    return result;
  }
}
