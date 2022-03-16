/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.devicemanager;

import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Device {
  protected final @NotNull String myName;
  protected final boolean myOnline;
  protected final @NotNull String myTarget;

  protected static abstract class Builder {
    protected @Nullable String myName;
    protected boolean myOnline;
    protected @Nullable String myTarget;

    protected abstract @NotNull Device build();
  }

  protected Device(@NotNull Builder builder) {
    assert builder.myName != null;
    myName = builder.myName;

    myOnline = builder.myOnline;

    assert builder.myTarget != null;
    myTarget = builder.myTarget;
  }

  protected abstract @NotNull Icon getIcon();

  public final @NotNull String getName() {
    return myName;
  }

  public final boolean isOnline() {
    return myOnline;
  }

  public final @NotNull String getTarget() {
    return myTarget;
  }
}
