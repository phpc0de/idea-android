/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.rendering;

import com.android.ide.common.rendering.api.IImageFactory;
import com.intellij.reference.SoftReference;

import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;

/**
 * {@link IImageFactory} that caches the image so it is not re-created on every call.
 */
class CachingImageFactory implements IImageFactory {
  private SoftReference<BufferedImage> myCachedImageReference;

  private final IImageFactory myDelegate;
  private int myCachedWidth;
  private int myCachedHeight;

  CachingImageFactory(IImageFactory delegate) {
    myDelegate = delegate;
  }

  @NotNull
  @Override
  public BufferedImage getImage(int width, int height) {
    BufferedImage cached = myCachedImageReference != null ? myCachedImageReference.get() : null;

    // This can cause flicker; see steps listed in http://b.android.com/208984
    if (cached == null || myCachedWidth != width || myCachedHeight != height) {
      cached = myDelegate.getImage(width, height);
      myCachedWidth = width;
      myCachedHeight = height;
      myCachedImageReference = new SoftReference<>(cached);
    }

    return cached;
  }
}
