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
package com.android.tools.idea.ui.resourcemanager.plugin

import com.android.tools.idea.ui.resourcemanager.model.DesignAsset


private val supportedFileTypes = setOf("xml")

/**
 * Importer for VectorDrawable
 */
class VectorDrawableImporter : ResourceImporter {
  override val presentableName = "Vector Drawable Importer"

  override val userCanEditQualifiers get() = true

  override fun getSupportedFileTypes() = supportedFileTypes // TODO reuse DesignAssetRenderer.isFileSupported

  override fun getSourcePreview(asset: DesignAsset): DesignAssetRenderer? =
    DesignAssetRendererManager.getInstance().getViewer(DrawableAssetRenderer::class.java)
}