/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.folding;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "AndroidFoldingSettings", storages = {
  @Storage("editor.xml"),
  @Storage(value = "editor.codeinsight.xml", deprecated = true),
})
public class AndroidFoldingSettings implements PersistentStateComponent<AndroidFoldingSettings> {
  public static AndroidFoldingSettings getInstance() {
    return ApplicationManager.getApplication().getService(AndroidFoldingSettings.class);
  }

  public boolean isCollapseAndroidStrings() {
    return COLLAPSE_ANDROID_TEXT;
  }

  public void setCollapseAndroidStrings(boolean value) {
    COLLAPSE_ANDROID_TEXT = value;
  }

  @SuppressWarnings("WeakerAccess") public boolean COLLAPSE_ANDROID_TEXT = true;

  @Override
  public AndroidFoldingSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final AndroidFoldingSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}