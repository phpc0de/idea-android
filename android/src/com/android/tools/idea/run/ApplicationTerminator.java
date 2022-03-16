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
package com.android.tools.idea.run;

import com.android.annotations.Trace;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.util.LaunchStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Kill an app processes and use ADB listener to wait until it actually happened.
 * <p>
 * When the remote debugger is attached or when the app is run from the device directly, Running/Debugging an unchanged app may be fast
 * enough that the delta install "am force-stop" command's effect does not get reflected in ddmlib before the same IDevice is added to
 * AndroidProcessHandler. This is caused by the fact that a no-change Run does not wait until the stale Client is actually terminated
 * before proceeding to attempt to connect the AndroidProcessHandler to the desired device. In such circumstances, the handler will
 * connect to the stale Client, and almost immediately have the same Client's killed state get reflected by ddmlib, and removed from the
 * process handler.
 */
public class ApplicationTerminator implements AndroidDebugBridge.IDeviceChangeListener {
  @NotNull private final IDevice myIDevice;
  @NotNull private final String myApplicationId;
  @NotNull private final List<Client> myClientsToWaitFor = Collections.synchronizedList(new ArrayList<>());
  @NotNull private final CountDownLatch myProcessKilledLatch = new CountDownLatch(1);

  public ApplicationTerminator(@NotNull IDevice iDevice, @NotNull String applicationId) {
    myIDevice = iDevice;
    myApplicationId = applicationId;
  }

  /**
   * @return true if upon return no processes related to an app are running.
   */
  @Trace
  public boolean killApp(@NotNull LaunchStatus launchStatus) {
    myIDevice.forceStop(myApplicationId);
    myClientsToWaitFor.addAll(DeploymentApplicationService.getInstance().findClient(myIDevice, myApplicationId));
    if (!myIDevice.isOnline() || myClientsToWaitFor.isEmpty()) {
      myProcessKilledLatch.countDown();
    }
    else {
      AndroidDebugBridge.addDeviceChangeListener(this);
      checkDone();
    }

    try {
      // Ensure all Clients are killed prior to handing off to the AndroidProcessHandler.
      if (!myProcessKilledLatch.await(10, TimeUnit.SECONDS)) {
        launchStatus.terminateLaunch(String.format("Couldn't terminate the existing process for %s.", myApplicationId), true);
        return false;
      }
    }
    catch (InterruptedException ignored) {
      launchStatus.terminateLaunch(String.format("Termination of the existing process for %s was cancelled.", myApplicationId), true);
      return false;
    }

    return true;
  }

  @Override
  public void deviceConnected(@NotNull IDevice device) {}

  @Override
  public void deviceDisconnected(@NotNull IDevice device) {
    if (device == myIDevice) {
      myProcessKilledLatch.countDown();
      AndroidDebugBridge.removeDeviceChangeListener(this);
    }
  }

  @Override
  public void deviceChanged(@NotNull IDevice changedDevice, int changeMask) {
    if (changedDevice != myIDevice || (changeMask & IDevice.CHANGE_CLIENT_LIST) == 0) {
      checkDone();
      return;
    }

    myClientsToWaitFor.retainAll(Arrays.asList(changedDevice.getClients()));
    checkDone();
  }

  private void checkDone() {
    if (myClientsToWaitFor.isEmpty()) {
      myProcessKilledLatch.countDown();
      AndroidDebugBridge.removeDeviceChangeListener(this);
    }
  }
}
