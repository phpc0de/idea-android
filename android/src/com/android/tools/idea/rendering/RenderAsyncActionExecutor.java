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
package com.android.tools.idea.rendering;

import com.intellij.openapi.application.ApplicationManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Interface to be implemented by executors of rendered async actions.
 */
public interface RenderAsyncActionExecutor {
  /**
   * Number of ms that we will wait for the rendering thread to return before timing out
   */
  long DEFAULT_RENDER_THREAD_TIMEOUT_MS = Long.getLong("layoutlib.thread.timeout",
                                                       TimeUnit.SECONDS.toMillis(10));

  long DEFAULT_RENDER_THREAD_QUEUE_TIMEOUT_MS = Long.getLong("layoutlib.thread.queue.timeout",
                                                       TimeUnit.SECONDS.toMillis(
                                                         (ApplicationManager.getApplication() == null ||
                                                          ApplicationManager.getApplication().isUnitTestMode()) ? 50 : 60)
  );

  /**
   * Runs an action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   * <p/>
   * This method will run the passed action asynchronously and return a {@link CompletableFuture}
   *
   * @param queueingTimeout maximum timeout for this action to wait to be executed.
   * @param queueingTimeoutUnit {@link TimeUnit} for queueingTimeout.
   * @param actionTimeout maximum timeout for this action to executed once it has started running.
   * @param actionTimeoutUnit {@link TimeUnit} for actionTimeout.
   * @param callable {@link Callable} to be executed with the render action.
   * @param <T> return type of the given callable.
   */
  @NotNull <T> CompletableFuture<T> runAsyncActionWithTimeout(
    long queueingTimeout,@NotNull TimeUnit queueingTimeoutUnit,
    long actionTimeout, @NotNull TimeUnit actionTimeoutUnit,
    @NotNull Callable<T> callable);

  /**
   * Runs an action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   * <p/>
   * This method will run the passed action asynchronously and return a {@link CompletableFuture}
   *
   * @param actionTimeout maximum timeout for this action to executed once it has started running.
   * @param actionTimeoutUnit {@link TimeUnit} for actionTimeout.
   * @param callable {@link Callable} to be executed with the render action.
   * @param <T> return type of the given callable.
   */
  default @NotNull <T> CompletableFuture<T> runAsyncActionWithTimeout(
    long actionTimeout, @NotNull TimeUnit actionTimeoutUnit,
    @NotNull Callable<T> callable) {
    return runAsyncActionWithTimeout(
      DEFAULT_RENDER_THREAD_QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS,
      actionTimeout, actionTimeoutUnit, callable);
  }

  /**
   * Runs an action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   * <p/>
   * This method will run the passed action asynchronously and return a {@link CompletableFuture}
   */
  default @NotNull <T> CompletableFuture<T> runAsyncAction(@NotNull Callable<T> callable) {
    return runAsyncActionWithTimeout(
      DEFAULT_RENDER_THREAD_QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS,
      DEFAULT_RENDER_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS, callable);
  }

  /**
   * Runs an action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   * <p/>
   * This method will run the passed action asynchronously
   */
  @NotNull
  default CompletableFuture<Void> runAsyncAction(@NotNull Runnable runnable) {
    return runAsyncAction(() -> {
      runnable.run();
      return null;
    });
  }
}
