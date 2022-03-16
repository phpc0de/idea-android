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
package com.android.tools.adtui.stdui;

import com.intellij.util.ui.JBUI;
import java.awt.Insets;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

public class BorderlessTableCellRenderer extends DefaultTableCellRenderer {
  @Override
  public void setBorder(Border border) {
    Insets insets = JBUI.insets(3, 10, 3, 0);
    if (getHorizontalAlignment() == RIGHT) {
      insets = JBUI.insets(3, 0, 3, 10);
    }
    super.setBorder(new EmptyBorder(insets));
  }
}

