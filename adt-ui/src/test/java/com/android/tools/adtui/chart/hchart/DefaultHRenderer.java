/*
 * Copyright (C) 2016 The Android Open Source Project
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


package com.android.tools.adtui.chart.hchart;

import com.android.annotations.NonNull;
import com.android.tools.adtui.model.DefaultHNode;
import com.intellij.ui.JBColor;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer which renders a single node in an {@link HTreeChart}.
 *
 * @param <T> The type of the model wrapped by each node.
 */
public abstract class DefaultHRenderer<T> implements HRenderer<DefaultHNode<T>> {

  protected static final JBColor fillVendorColor = new JBColor(new Color(146, 215, 248), new Color(146, 215, 248));
  protected static final JBColor borderVendorColor = new JBColor(new Color(115, 190, 233), new Color(115, 190, 233));

  protected static final JBColor fillPlatformColor = new JBColor(new Color(190, 225, 154), new Color(190, 225, 154));
  protected static final JBColor borderPlatformColor = new JBColor(new Color(159, 208, 110), new Color(159, 208, 110));

  protected static final JBColor fillAppColor = new JBColor(new Color(245, 192, 118), new Color(245, 192, 118));
  protected static final JBColor borderAppColor = new JBColor(new Color(235, 163, 63), new Color(235, 163, 63));

  // To limit the number of object allocation we reuse the same Rectangle.
  @NonNull private Rectangle2D.Float mRect;

  /**
   * A function which can be used to tweak the color of a node that should be highlighted since it
   * is focused.
   */
  private Function<Color, Color> myAdjustFocusedColor;

  public DefaultHRenderer() {
    this(color -> color);
  }

  public DefaultHRenderer(@NotNull Function<Color, Color> adjustFocusedColor) {
    myAdjustFocusedColor = adjustFocusedColor;
    mRect = new Rectangle2D.Float();
  }

  // This method is not thread-safe. In order to limit object allocation, mRect is being re-used.
  @Override
  public final void render(@NotNull Graphics2D g,
                           @NotNull DefaultHNode<T> node,
                           @NotNull Rectangle2D fullDrawingArea,
                           @NotNull Rectangle2D drawingArea,
                           boolean isFocused,
                           boolean isDeselected) {
    mRect.x = (float)drawingArea.getX();
    mRect.y = (float)drawingArea.getY();
    mRect.width = (float)drawingArea.getWidth();
    mRect.height = (float)drawingArea.getHeight();

    // Draw rectangle background
    Color fillColor = getFillColor(node.getData());
    if (isFocused) {
      fillColor = myAdjustFocusedColor.apply(fillColor);
    }
    g.setPaint(fillColor);
    g.fill(mRect);

    // Draw text
    FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
    String text = generateFittingText(node.getData(), drawingArea, fontMetrics);
    g.setPaint(Color.BLACK);
    renderText(g, text, mRect, fontMetrics);
  }

  protected abstract String generateFittingText(@NotNull T nodeData, @NotNull Rectangle2D rect, @NotNull FontMetrics fontMetrics);
  protected abstract Color getFillColor(@NotNull T nodeData);

  /**
   * Renders a text inside a node rectangle according to the renderer constraints.
   */
  private void renderText(Graphics2D g, String text, Rectangle2D.Float rect, FontMetrics fontMetrics) {
    float textPositionY = (float)(rect.getY() + fontMetrics.getAscent());
    g.drawString(text, rect.x, textPositionY);
  }
}
