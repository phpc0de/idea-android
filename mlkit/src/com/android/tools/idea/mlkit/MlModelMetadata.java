/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.mlkit;

import com.android.tools.mlkit.ModelInfo;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Stores metadata of model file used by the light model class generator.
 */
public class MlModelMetadata {
  public final String myModelFileUrl;
  public final ModelInfo myModelInfo;

  public MlModelMetadata(@NotNull String modelFileUrl, @NotNull ModelInfo modelInfo) {
    myModelFileUrl = modelFileUrl;
    myModelInfo = modelInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MlModelMetadata that = (MlModelMetadata)o;
    return myModelFileUrl.equals(that.myModelFileUrl) && myModelInfo.equals(that.myModelInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myModelFileUrl, myModelInfo);
  }
}
