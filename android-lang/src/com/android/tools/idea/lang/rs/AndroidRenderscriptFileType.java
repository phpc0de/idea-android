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
package com.android.tools.idea.lang.rs;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.StudioIcons;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AndroidRenderscriptFileType extends LanguageFileType {
  public static final AndroidRenderscriptFileType INSTANCE = new AndroidRenderscriptFileType();
  @NonNls public static final String CODE_EXTENSION = "rs";
  @NonNls public static final String FS_CODE_EXTENSION = "fs";
  @NonNls private static final String HEADER_EXTENSION = "rsh";

  private AndroidRenderscriptFileType() {
    super(RenderscriptLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "Android RenderScript";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Android RenderScript";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return CODE_EXTENSION;
  }

  @Override
  public Icon getIcon() {
    return StudioIcons.Shell.Filetree.RENDER_SCRIPT;
  }
}
