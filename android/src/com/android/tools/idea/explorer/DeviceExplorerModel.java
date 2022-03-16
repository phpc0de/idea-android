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
package com.android.tools.idea.explorer;

import com.android.tools.idea.explorer.fs.DeviceFileSystem;
import com.android.tools.idea.explorer.fs.DeviceState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Device Explorer model class: encapsulates the list of devices,
 * their file system and also associated state changes to via the
 * {@link DeviceExplorerModelListener} listener class.
 */
public class DeviceExplorerModel {
  @NotNull private final List<DeviceExplorerModelListener> myListeners = new ArrayList<>();
  @NotNull private final List<DeviceFileSystem> myDevices = new ArrayList<>();
  @Nullable private DeviceFileSystem myActiveDevice;
  @Nullable private DeviceState myActiveDeviceLastKnownState;
  @Nullable private DefaultTreeModel myTreeModel;
  @Nullable private DefaultTreeSelectionModel myTreeSelectionModel;

  @NotNull
  public List<DeviceFileSystem> getDevices() {
    return myDevices;
  }

  @Nullable
  public DeviceFileSystem getActiveDevice() {
    return myActiveDevice;
  }

  public void setActiveDevice(@Nullable DeviceFileSystem activeDevice) {
    myActiveDevice = activeDevice;
    myActiveDeviceLastKnownState = (activeDevice == null ? null : activeDevice.getDeviceState());
    myListeners.forEach(x -> x.activeDeviceChanged(myActiveDevice));
    setActiveDeviceTreeModel(activeDevice, null, null);
  }

  @Nullable
  public DeviceState getActiveDeviceLastKnownState(@NotNull DeviceFileSystem device) {
    if (!Objects.equals(device, myActiveDevice)) {
      return null;
    }
    return myActiveDeviceLastKnownState;
  }

  public void setActiveDeviceLastKnownState(@NotNull DeviceFileSystem device) {
    if (!Objects.equals(device, myActiveDevice)) {
      return;
    }
    myActiveDeviceLastKnownState = device.getDeviceState();
  }

  @Nullable
  public DefaultTreeModel getTreeModel() {
    return myTreeModel;
  }

  @Nullable
  public DefaultTreeSelectionModel getTreeSelectionModel() {
    return myTreeSelectionModel;
  }

  public void addListener(@NotNull DeviceExplorerModelListener listener) {
    myListeners.add(listener);
  }

  public void removeListener(@NotNull DeviceExplorerModelListener listener) {
    myListeners.remove(listener);
  }

  public void setActiveDeviceTreeModel(@Nullable DeviceFileSystem device,
                                       @Nullable DefaultTreeModel treeModel,
                                       @Nullable DefaultTreeSelectionModel treeSelectionModel) {
    // Ignore if active device has changed
    if (!Objects.equals(myActiveDevice, device)) {
      return;
    }

    // Ignore if tree model is not changing
    if (Objects.equals(myTreeModel, treeModel)) {
      return;
    }

    myTreeModel = treeModel;
    myTreeSelectionModel = treeSelectionModel;
    myListeners.forEach(x -> x.treeModelChanged(treeModel, treeSelectionModel));
  }

  public void addDevice(@NotNull DeviceFileSystem device) {
    if (myDevices.contains(device))
      return;
    myDevices.add(device);
    myListeners.forEach(l -> l.deviceAdded(device));
  }

  public void removeDevice(@NotNull DeviceFileSystem device) {
    if (!myDevices.contains(device))
      return;
    myListeners.forEach(l -> l.deviceRemoved(device));
    myDevices.remove(device);
  }

  public void removeAllDevices() {
    myDevices.clear();
    setActiveDevice(null);
    setActiveDeviceTreeModel(null, null, null);
    myListeners.forEach(DeviceExplorerModelListener::allDevicesRemoved);
  }

  public void updateDevice(@NotNull DeviceFileSystem device) {
    if (!myDevices.contains(device))
      return;
    myListeners.forEach(l -> l.deviceUpdated(device));
  }
}
