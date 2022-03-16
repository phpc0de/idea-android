/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.explorer.mocks;

import com.android.tools.idea.concurrency.FutureCallbackExecutor;
import com.android.tools.idea.concurrency.FutureUtils;
import com.android.tools.idea.explorer.fs.DeviceFileSystem;
import com.android.tools.idea.explorer.fs.DeviceFileSystemService;
import com.android.tools.idea.explorer.fs.DeviceFileSystemServiceListener;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockDeviceFileSystemService implements DeviceFileSystemService<DeviceFileSystem> {
  public static int OPERATION_TIMEOUT_MILLIS = 10;

  @NotNull private final Project myProject;
  @NotNull private final FutureCallbackExecutor myEdtExecutor;
  @NotNull private final FutureCallbackExecutor myTaskExecutor;
  @NotNull private final List<DeviceFileSystemServiceListener> myListeners = new ArrayList<>();
  @NotNull private final List<MockDeviceFileSystem> myDevices = new ArrayList<>();

  public MockDeviceFileSystemService(@NotNull Project project, @NotNull Executor edtExecutor, @NotNull Executor taskExecutor) {
    myProject = project;
    myEdtExecutor = new FutureCallbackExecutor(edtExecutor);
    myTaskExecutor = new FutureCallbackExecutor(taskExecutor);
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public FutureCallbackExecutor getEdtExecutor() {
    return myEdtExecutor;
  }

  @Override
  public void addListener(@NotNull DeviceFileSystemServiceListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeListener(@NotNull DeviceFileSystemServiceListener listener) {
    myListeners.remove(listener);
  }

  public DeviceFileSystemServiceListener[] getListeners() {
    return myListeners.toArray(new DeviceFileSystemServiceListener[myListeners.size()]);
  }

  @NotNull
  @Override
  public ListenableFuture<Void> start(@NotNull Supplier<File> adbSupplier) {
    return FutureUtils.delayedValue(null, OPERATION_TIMEOUT_MILLIS);
  }

  @NotNull
  @Override
  public ListenableFuture<Void> restart(@NotNull Supplier<File> adbSupplier) {
    ListenableFuture<Void> futureResult = FutureUtils.delayedValue(null, OPERATION_TIMEOUT_MILLIS);

    myEdtExecutor.addCallback(futureResult, new FutureCallback<Void>() {
      @Override
      public void onSuccess(@Nullable Void result) {
        myListeners.forEach(DeviceFileSystemServiceListener::serviceRestarted);
      }

      @Override
      public void onFailure(@NotNull Throwable t) {
      }
    });
    return futureResult;
  }

  @NotNull
  @Override
  public ListenableFuture<List<DeviceFileSystem>> getDevices() {
    return Futures.immediateFuture(new ArrayList<DeviceFileSystem>(myDevices));
  }

  public MockDeviceFileSystem addDevice(String deviceName) {
    MockDeviceFileSystem device = new MockDeviceFileSystem(this, deviceName, myTaskExecutor);
    myDevices.add(device);
    myListeners.forEach(l -> l.deviceAdded(device));
    return device;
  }

  public boolean removeDevice(MockDeviceFileSystem device) {
    int index = myDevices.indexOf(device);
    if (index < 0) {
      return false;
    }

    myDevices.remove(index);
    myListeners.forEach(l -> l.deviceRemoved(device));
    return true;
  }
}
