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

import com.android.tools.idea.explorer.fs.DeviceFileSystem;
import com.android.tools.idea.explorer.fs.DeviceFileSystemRenderer;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MockDeviceFileSystemRenderer implements DeviceFileSystemRenderer {
  private final ListCellRenderer<DeviceFileSystem> myRenderer;

  public MockDeviceFileSystemRenderer() {
    myRenderer = SimpleListCellRenderer.create("<No device>", DeviceFileSystem::getName);
  }

  @NotNull
  @Override
  public ListCellRenderer<DeviceFileSystem> getDeviceNameListRenderer() {
    return myRenderer;
  }
}
