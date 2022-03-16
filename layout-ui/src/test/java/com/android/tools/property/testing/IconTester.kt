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
package com.android.tools.property.testing

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Icon

object IconTester {

  fun hasOnlyWhiteColors(icon: Icon): Boolean {
    var combined = 0xffffff
    val image = toImage(icon)
    for (x in 0 until image.width) {
      for (y in 0 until image.height) {
        val rgb = Color(image.getRGB(x, y), true)
        if (rgb.alpha != 0) {
          combined = combined.and(rgb.rgb)
        }
      }
    }
    return combined.and(0xffffff) == 0xffffff
  }

  private fun toImage(icon: Icon): BufferedImage {
    @Suppress("UndesirableClassUsage")
    val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    icon.paintIcon(null, g, 0, 0)
    return image
  }
}
