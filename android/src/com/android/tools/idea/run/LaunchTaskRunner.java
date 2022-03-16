/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run;

import com.android.ddmlib.IDevice;
import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.run.tasks.DebugConnectorTask;
import com.android.tools.idea.run.tasks.LaunchContext;
import com.android.tools.idea.run.tasks.LaunchResult;
import com.android.tools.idea.run.tasks.LaunchTask;
import com.android.tools.idea.run.tasks.LaunchTasksProvider;
import com.android.tools.idea.run.util.LaunchStatus;
import com.android.tools.idea.run.util.ProcessHandlerLaunchStatus;
import com.android.tools.idea.run.util.SwapInfo;
import com.android.tools.idea.stats.RunStats;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.wireless.android.sdk.stats.LaunchTaskDetail;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LaunchTaskRunner extends Task.Backgroundable {
  @NotNull private final String myConfigName;
  @NotNull private final String myApplicationId;
  @Nullable private final String myExecutionTargetName; // Change to NotNull once everything is moved over to DeviceAndSnapshot
  @NotNull private final LaunchInfo myLaunchInfo;
  @NotNull private final ProcessHandler myProcessHandler;
  @NotNull private final DeviceFutures myDeviceFutures;
  @NotNull private final LaunchTasksProvider myLaunchTasksProvider;
  @NotNull private final RunStats myStats;
  @NotNull private final BiConsumer<String, HyperlinkInfo> myConsoleConsumer;
  @NotNull private final List<Runnable> myOnFinished;

  @Nullable private String myError;
  @Nullable private NotificationListener myErrorNotificationListener;

  public LaunchTaskRunner(@NotNull Project project,
                          @NotNull String configName,
                          @NotNull String applicationId,
                          @Nullable String executionTargetName,
                          @NotNull LaunchInfo launchInfo,
                          @NotNull ProcessHandler processHandler,
                          @NotNull DeviceFutures deviceFutures,
                          @NotNull LaunchTasksProvider launchTasksProvider,
                          @NotNull RunStats stats,
                          @NotNull BiConsumer<String, HyperlinkInfo> consoleConsumer) {
    super(project, "Launching " + configName);

    myConfigName = configName;
    myApplicationId = applicationId;
    myExecutionTargetName = executionTargetName;
    myLaunchInfo = launchInfo;
    myProcessHandler = processHandler;
    myDeviceFutures = deviceFutures;
    myLaunchTasksProvider = launchTasksProvider;
    myStats = stats;
    myConsoleConsumer = consoleConsumer;
    myOnFinished = new ArrayList<>();
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    myStats.beginLaunchTasks();

    try {
      final boolean destroyProcessOnCancellation = !isSwap();
      indicator.setText(getTitle());
      indicator.setIndeterminate(false);

      ProcessHandlerLaunchStatus launchStatus = new ProcessHandlerLaunchStatus(myProcessHandler);
      ProcessHandlerConsolePrinter consolePrinter = new ProcessHandlerConsolePrinter(myProcessHandler);
      List<ListenableFuture<IDevice>> listenableDeviceFutures = myDeviceFutures.get();
      AndroidVersion androidVersion = myDeviceFutures.getDevices().size() == 1
                                      ? myDeviceFutures.getDevices().get(0).getVersion()
                                      : null;
      DebugConnectorTask debugSessionTask = isSwap() ? null : myLaunchTasksProvider.getConnectDebuggerTask(launchStatus, androidVersion);

      if (debugSessionTask != null) {
        if (listenableDeviceFutures.size() != 1) {
          launchStatus.terminateLaunch("Cannot launch a debug session on more than 1 device.", true);
          return;
        }
        // Copy over console output from the original console to the debug console once it is established.
        AndroidProcessText.attach(myProcessHandler);
      }

      printLaunchTaskStartedMessage(consolePrinter);

      indicator.setText("Waiting for all target devices to come online");
      List<IDevice> devices = listenableDeviceFutures.stream()
        .map(deviceFuture -> waitForDevice(deviceFuture, indicator, launchStatus, destroyProcessOnCancellation))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
      if (devices.size() != listenableDeviceFutures.size()) {
        // Halt execution if any of target devices are unavailable.
        return;
      }

      // Wait for the previous android process with the same application ID to be terminated before we start the new process.
      // This step is necessary only for the standard launch (non-swap, android process handler). Ignore this step for
      // hot-swapping or debug runs.
      if (!isSwap() && myProcessHandler instanceof AndroidProcessHandler) {
        ListenableFuture<?> waitApplicationTerminationTask = Futures.whenAllSucceed(
          ContainerUtil.map(devices, device -> MoreExecutors.listeningDecorator(AppExecutorUtil.getAppExecutorService()).submit(() -> {
            ApplicationTerminator terminator = new ApplicationTerminator(device, myApplicationId);
            if (!terminator.killApp(launchStatus)) {
              throw new CancellationException("Could not terminate running app " + myApplicationId);
            }
            if (device.isOnline()) {
              ((AndroidProcessHandler)myProcessHandler).addTargetDevice(device);
            }
          }))).run(() -> {}, AppExecutorUtil.getAppExecutorService());

        ProgressIndicatorUtils.awaitWithCheckCanceled(waitApplicationTerminationTask, indicator);

        if (waitApplicationTerminationTask.isCancelled()) {
          return;
        }
      }

      myLaunchTasksProvider.fillStats(myStats);

      // Perform launch tasks for each device in parallel.
      Map<IDevice, List<LaunchTask>> launchTaskMap = new HashMap<>(devices.size());
      for (IDevice device : devices) {
        try {
          List<LaunchTask> launchTasks = myLaunchTasksProvider.getTasks(device, launchStatus, consolePrinter);
          launchTaskMap.put(device, launchTasks);
        }
        catch (com.intellij.execution.ExecutionException e) {
          launchStatus.terminateLaunch(e.getMessage(), !isSwap());
          return;
        }
        catch (IllegalStateException e) {
          launchStatus.terminateLaunch(e.getMessage(), !isSwap());
          Logger.getInstance(LaunchTaskRunner.class).error(e);
          return;
        }
      }

      Ref<Integer> completedStepsCount = new Ref<>(0);
      final int totalScheduledStepsCount = launchTaskMap
        .values()
        .stream()
        .mapToInt(launchTasks -> getTotalDuration(launchTasks, debugSessionTask))
        .sum();

      for (Map.Entry<IDevice, List<LaunchTask>> entry : launchTaskMap.entrySet()) {
        try {
          boolean isSucceeded = runLaunchTasks(
            entry.getValue(),
            indicator,
            new LaunchContext(myProject, myLaunchInfo.executor, entry.getKey(), launchStatus, consolePrinter, myProcessHandler),
            destroyProcessOnCancellation,
            completedStepsCount,
            totalScheduledStepsCount);
          if (!isSucceeded) {
            return;
          }
        } catch (CancellationException e) {
          launchStatus.terminateLaunch(e.getMessage(), !isSwap());
          return;
        }
      }

      // A debug session task should be performed sequentially at the end.
      for (IDevice device : devices) {
        if (debugSessionTask != null) {
          indicator.setText(debugSessionTask.getDescription());
          debugSessionTask.perform(myLaunchInfo, device, launchStatus, consolePrinter);
          // Update the indicator progress bar.
          completedStepsCount.set(completedStepsCount.get() + debugSessionTask.getDuration());
          indicator.setFraction(completedStepsCount.get().floatValue() / totalScheduledStepsCount);
        }
      }
    } finally {
      myStats.endLaunchTasks();
    }
  }

  private boolean runLaunchTasks(@NotNull List<LaunchTask> launchTasks,
                                 @NotNull ProgressIndicator indicator,
                                 @NotNull LaunchContext launchContext,
                                 boolean destroyProcessOnCancellation,
                                 @NotNull Ref<Integer> completedStepsCount,
                                 int totalScheduledStepsCount) {
    // Update the indicator progress.
    indicator.setFraction(completedStepsCount.get().floatValue() / totalScheduledStepsCount);
    IDevice device = launchContext.getDevice();
    LaunchStatus launchStatus = launchContext.getLaunchStatus();

    String groupId = "LaunchTaskRunner for " + launchContext.getExecutor().getId();
    NotificationGroup notificationGroup = NotificationGroup.findRegisteredGroup(groupId);
    if (notificationGroup == null) {
      notificationGroup = NotificationGroup.toolWindowGroup(groupId, launchContext.getExecutor().getId());
    }
    for (LaunchTask task : launchTasks) {
      if (!checkIfLaunchIsAliveAndTerminateIfCancelIsRequested(indicator, launchStatus, destroyProcessOnCancellation)) {
        return false;
      }

      if (task.shouldRun(launchContext)) {
        LaunchTaskDetail.Builder details = myStats.beginLaunchTask(task);
        indicator.setText(task.getDescription());
        LaunchResult result = task.run(launchContext);
        myOnFinished.addAll(result.onFinishedCallbacks());
        boolean success = result.getSuccess();
        myStats.endLaunchTask(task, details, success);
        if (!success) {
          myErrorNotificationListener = result.getNotificationListener();
          myError = result.getError();
          launchStatus.terminateLaunch(result.getConsoleError(), !isSwap());

          // Append a footer hyperlink, if one was provided.
          if (result.getConsoleHyperlinkInfo() != null) {
            myConsoleConsumer.accept(result.getConsoleHyperlinkText() + "\n",
                                     result.getConsoleHyperlinkInfo());
          }

          notificationGroup.createNotification("Error", result.getError(), NotificationType.ERROR)
            .setListener(myErrorNotificationListener)
            .setImportant(true).notify(myProject);

          // Show the tool window when we have an error.
          RunContentManager.getInstance(myProject).toFrontRunContent(myLaunchInfo.executor, myProcessHandler);

          myStats.setErrorId(result.getErrorId());
          return false;
        }

        // Notify listeners of the deployment.
        myProject.getMessageBus().syncPublisher(AppDeploymentListener.TOPIC).appDeployedToDevice(device, myProject);
      }

      // Update the indicator progress.
      completedStepsCount.set(completedStepsCount.get() + task.getDuration());
      indicator.setFraction(completedStepsCount.get().floatValue() / totalScheduledStepsCount);
    }

    String launchType = myLaunchTasksProvider.getLaunchTypeDisplayName();
    notificationGroup.createNotification("", launchType + " succeeded", NotificationType.INFORMATION)
      .setImportant(false).notify(myProject);

    return true;
  }

  private void printLaunchTaskStartedMessage(ConsolePrinter consolePrinter) {
    StringBuilder launchString = new StringBuilder("\n");
    DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
    launchString.append(dateFormat.format(new Date())).append(": ");
    launchString.append(getLaunchVerb()).append(" ");
    launchString.append("'").append(myConfigName).append("'");
    if (!StringUtil.isEmpty(myExecutionTargetName)) {
      launchString.append(" on ");
      launchString.append(myExecutionTargetName);
    }
    launchString.append(".");
    consolePrinter.stdout(launchString.toString());
  }

  @Override
  public void onSuccess() {
    if (myError == null) {
      myStats.success();
    }
  }

  @Override
  public void onFinished() {
    super.onFinished();
    if (myError != null) {
      myStats.fail();
    }
    for (Runnable runnable : myOnFinished) {
      ApplicationManager.getApplication().invokeLater(runnable);
    }
  }

  @Nullable
  private IDevice waitForDevice(@NotNull ListenableFuture<IDevice> deviceFuture,
                                @NotNull ProgressIndicator indicator,
                                @NotNull LaunchStatus launchStatus,
                                boolean destroyProcess) {
    myStats.beginWaitForDevice();
    IDevice device = null;
    while (checkIfLaunchIsAliveAndTerminateIfCancelIsRequested(indicator, launchStatus, destroyProcess)) {
      try {
        device = deviceFuture.get(1, TimeUnit.SECONDS);
        break;
      }
      catch (TimeoutException ignored) {
        // Let's check the cancellation request then continue to wait for a device again.
      }
      catch (InterruptedException e) {
        launchStatus.terminateLaunch("Interrupted while waiting for device", destroyProcess);
        break;
      }
      catch (ExecutionException e) {
        launchStatus.terminateLaunch("Error while waiting for device: " + e.getCause().getMessage(), destroyProcess);
        break;
      }
    }
    myStats.endWaitForDevice(device);
    return device;
  }

  /**
   * Checks if the launch is still alive and good to continue. Upon cancellation request, it updates a given {@code launchStatus} to
   * be terminated state. The associated process will be forcefully destroyed if {@code destroyProcess} is true.
   *
   * @param indicator      an progress indicator to check the user cancellation request
   * @param launchStatus   a launch status to be checked and updated upon the cancellation request
   * @param destroyProcess true to destroy the associated process upon cancellation, false to detach the process instead
   * @return true if the launch is still good to go, false otherwise.
   */
  private static boolean checkIfLaunchIsAliveAndTerminateIfCancelIsRequested(
    @NotNull ProgressIndicator indicator, @NotNull LaunchStatus launchStatus, boolean destroyProcess) {
    // Check for cancellation via stop button or unexpected failures in launch tasks.
    if (launchStatus.isLaunchTerminated()) {
      return false;
    }

    // Check for cancellation via progress bar.
    if (indicator.isCanceled()) {
      launchStatus.terminateLaunch("User cancelled launch", destroyProcess);
      return false;
    }

    return true;
  }

  private static int getTotalDuration(@NotNull List<LaunchTask> launchTasks, @Nullable DebugConnectorTask debugSessionTask) {
    int total = 0;

    for (LaunchTask task : launchTasks) {
      total += task.getDuration();
    }

    if (debugSessionTask != null) {
      total += debugSessionTask.getDuration();
    }

    return total;
  }

  private boolean isSwap() {
    return myLaunchInfo.env.getUserData(SwapInfo.SWAP_INFO_KEY) != null;
  }

  @NotNull
  private String getLaunchVerb() {
    SwapInfo swapInfo = myLaunchInfo.env.getUserData(SwapInfo.SWAP_INFO_KEY);
    if (swapInfo != null) {
      if (swapInfo.getType() == SwapInfo.SwapType.APPLY_CHANGES) {
        return "Applying changes to";
      }
      else if (swapInfo.getType() == SwapInfo.SwapType.APPLY_CODE_CHANGES) {
        return "Applying code changes to";
      }
    }
    return "Launching";
  }
}
