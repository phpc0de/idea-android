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
package com.android.tools.idea.mlkit.viewer;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.mlkit.viewer.TfliteModelFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Registers machine learning model file types.
 */
public class MlModelFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(@NotNull FileTypeConsumer consumer) {
    if (StudioFlags.ML_MODEL_BINDING.get() || ApplicationManager.getApplication().isUnitTestMode()) {
      consumer.consume(TfliteModelFileType.INSTANCE, TfliteModelFileType.INSTANCE.getDefaultExtension());
    }
  }
}
