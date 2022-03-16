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
package com.android.tools.idea.uibuilder.handlers;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.FQCN_TABLE_ROW;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;

import com.android.tools.idea.common.api.InsertType;
import com.android.tools.idea.common.command.NlWriteCommandActionUtil;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.tools.idea.uibuilder.handlers.linear.LinearLayoutHandler;
import com.android.tools.idea.uibuilder.model.NlComponentHelperKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for the {@code <TableLayout>} widget
 */
public class TableLayoutHandler extends LinearLayoutHandler {
  @Override
  public boolean isVertical(@NotNull NlComponent component) {
    // Tables are always vertical
    return true;
  }

  @Override
  public boolean acceptsChild(@NotNull NlComponent layout, @NotNull NlComponent newChild) {
    // Only table rows are allowed as direct children of the table
    return TABLE_ROW.equals(newChild.getTagName());
  }

  @Override
  public void onChildInserted(@NotNull ViewEditor editor,
                              @NotNull NlComponent parent,
                              @NotNull NlComponent child,
                              @NotNull InsertType insertType) {
    // Overridden to inhibit the setting of layout_width/layout_height since
    // it should always be match_parent
    child.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, VALUE_MATCH_PARENT);
    child.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, VALUE_MATCH_PARENT);
  }

  @Override
  public boolean onCreate(@NotNull ViewEditor editor,
                          @Nullable NlComponent parent,
                          @NotNull NlComponent node,
                          @NotNull InsertType insertType) {
    if (insertType.isCreate()) {
      // Start the table with 4 rows
      NlWriteCommandActionUtil.run(node, "", () -> {
        for (int i = 0; i < 4; i++) {
          NlComponentHelperKt.createChild(node, editor, FQCN_TABLE_ROW, null, InsertType.PROGRAMMATIC);
        }
      });
    }

    return true;
  }
}
