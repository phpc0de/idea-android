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
package com.android.tools.property.ptable.simple;

import com.android.tools.property.ptable.PTableGroupItem;
import com.android.tools.property.ptable.PTableItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimpleGroupItem extends PTableGroupItem {
  private String myName;

  public SimpleGroupItem(@NotNull String name, @NotNull List<PTableItem> children) {
    myName = name;
    setChildren(children);
  }

  @NotNull
  @Override
  public String getChildLabel(@NotNull PTableItem item) {
    return myName + "." + item.getName();
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }
}
