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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.tools.adtui.model.FakeTimer;
import com.android.tools.adtui.model.StreamingTimeline;
import com.android.tools.adtui.model.updater.Updater;
import com.android.tools.adtui.swing.FakeKeyboard;
import com.android.tools.adtui.swing.FakeUi;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StreamingScrollbarTest {

  public static final float EPSILON = 0.0001f;
  private StreamingTimeline myTimeline;
  private StreamingScrollbar myScrollbar;
  private JPanel myPanel;
  private FakeUi myUi;
  private FakeTimer myTimer;

  @Before
  public void setUp() {
    myTimer = new FakeTimer();
    Updater updater = new Updater(myTimer);
    myTimeline = new StreamingTimeline(updater);
    myPanel = new JPanel();
    myPanel.setSize(100, 100);
    myScrollbar = new StreamingScrollbar(myTimeline, myPanel);
    myScrollbar.setSize(100, 10);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myPanel, BorderLayout.CENTER);
    panel.add(myScrollbar, BorderLayout.SOUTH);
    panel.setSize(100, 110);
    myUi = new FakeUi(panel);
    myTimeline.getViewRange().set(0, 5000);
    myTimer.tick(TimeUnit.MICROSECONDS.toNanos(10000));
    myTimeline.setIsPaused(true); // Set paused so we don't update data time in future updates.
  }

  @Test
  public void testInitialization() {
    StreamingScrollbar scrollbar = new StreamingScrollbar(myTimeline, myPanel);

    // Scrollbar's model should be correct without an explicit update.
    // Note: model units are kept in 1000th of a range unit
    assertEquals(0, scrollbar.getModel().getMinimum());
    assertEquals(10, scrollbar.getModel().getMaximum());
    assertEquals(0, scrollbar.getModel().getValue());
    assertEquals(5, scrollbar.getModel().getExtent());
  }

  @Test
  public void testModelChanged() {
    // Note: model units are kept in 1000th of a range unit
    assertEquals(0, myScrollbar.getModel().getMinimum());
    assertEquals(10, myScrollbar.getModel().getMaximum());
    assertEquals(0, myScrollbar.getModel().getValue());
    assertEquals(5, myScrollbar.getModel().getExtent());

    // Update view range.
    myTimeline.getViewRange().set(1000, 4000);
    assertEquals(0, myScrollbar.getModel().getMinimum());
    assertEquals(10, myScrollbar.getModel().getMaximum());
    assertEquals(1, myScrollbar.getModel().getValue());
    assertEquals(3, myScrollbar.getModel().getExtent());

    // Update data range.
    myTimeline.getDataRange().set(0, 20000);
    assertEquals(0, myScrollbar.getModel().getMinimum());
    assertEquals(20, myScrollbar.getModel().getMaximum());
    assertEquals(1, myScrollbar.getModel().getValue());
    assertEquals(3, myScrollbar.getModel().getExtent());
  }

  @Test
  public void testZoom() {
    // Zoom in
    double delta = myScrollbar.getZoomWheelDelta();
    myUi.keyboard.press(FakeKeyboard.MENU_KEY_CODE); // Menu+wheel == zoom

    myUi.mouse.wheel(50, 50, -1);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + delta * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000 - delta * 0.5, myTimeline.getViewRange().getMax(), EPSILON);

    // Zoom in twice
    double delta2 = myScrollbar.getZoomWheelDelta() * 2;
    myUi.mouse.wheel(50, 50, -2);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + (delta + delta2) * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000 - (delta + delta2) * 0.5, myTimeline.getViewRange().getMax(), EPSILON);

    // Zoom out
    double delta3 = myScrollbar.getZoomWheelDelta();
    myUi.mouse.wheel(50, 50, 1);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + (delta + delta2 - delta3) * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000 - (delta + delta2 - delta3) * 0.5, myTimeline.getViewRange().getMax(), EPSILON);
  }

  @Test
  public void testCanZoomWhenNotScrollable() {
    int initialMax = 10000;
    myTimeline.getViewRange().set(0, initialMax);
    // Zoom in
    double delta = myScrollbar.getZoomWheelDelta();
    myUi.keyboard.press(FakeKeyboard.MENU_KEY_CODE); // Menu+wheel == zoom

    assertTrue(myScrollbar.isScrollable());
    myUi.mouse.wheel(50, 50, -1);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + delta * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(initialMax - delta * 0.5, myTimeline.getViewRange().getMax(), EPSILON);

    // Set our data min large enough that when we zoom in it moves our view min.
    myTimeline.getDataRange().setMin(myTimeline.getViewRange().getMin() + 2000);
    assertFalse(myScrollbar.isScrollable());
    // Zoom in again
    double delta2 = myScrollbar.getZoomWheelDelta() * 2;
    myUi.mouse.wheel(50, 50, -2);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + (delta + delta2) * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(initialMax, myTimeline.getViewRange().getMax(), EPSILON);
  }

  @Test
  public void testPan() {
    double delta = myScrollbar.getPanWheelDelta();
    myUi.mouse.wheel(50, 50, 1);
    assertEquals(0 + delta, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000 + delta, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.wheel(50, 50, 2);
    assertEquals(0 + delta * 3, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000 + delta * 3, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.wheel(50, 50, -3);
    assertEquals(0, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000, myTimeline.getViewRange().getMax(), EPSILON);
  }

  @Test
  public void testAdjust() {
    // Disable paused so we get proper streaming states.
    myTimeline.setIsPaused(false);
    assertEquals(0, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.press(5, 100);

    assertEquals(0, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.dragTo(10, 100);

    assertEquals(1000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(6000, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.release();

    assertEquals(1000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(6000, myTimeline.getViewRange().getMax(), EPSILON);

    // Scroll to the end, and check streaming
    assertFalse(myTimeline.isStreaming());
    myUi.mouse.press(10, 100);
    assertFalse(myTimeline.isStreaming());
    myUi.mouse.dragTo(40, 100);
    assertFalse(myTimeline.isStreaming());
    myUi.mouse.release();

    assertEquals(4000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(9000, myTimeline.getViewRange().getMax(), EPSILON);

    BufferedImage image = new BufferedImage(100, 10, BufferedImage.TYPE_INT_ARGB);
    Graphics graphics = image.getGraphics();
    graphics.setClip(0, 0, 100, 10);
    myScrollbar.paintComponent(graphics);
    assertTrue(myTimeline.isStreaming());
  }

  @Test
  public void testBlockIncrement() {
    assertEquals(5, myScrollbar.getBlockIncrement(), EPSILON);

    // Click on the non-thumb region on the scrollbar should advance the time by a block.
    myUi.mouse.click(75, 105);
    assertEquals(5000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(10000, myTimeline.getViewRange().getMax(), EPSILON);

    // Updating the view range should update the block increment
    myTimeline.getViewRange().set(5000, 6000);
    assertEquals(1, myScrollbar.getBlockIncrement(), EPSILON);

    myUi.mouse.click(5, 105);
    assertEquals(4000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(5000, myTimeline.getViewRange().getMax(), EPSILON);

    myUi.mouse.click(95, 105);
    assertEquals(5000, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(6000, myTimeline.getViewRange().getMax(), EPSILON);
  }

  @Test
  public void testScrollableAtMaxRange() {
    myTimeline.getViewRange().set(0, 10000);

    double delta = myScrollbar.getZoomWheelDelta();
    myUi.keyboard.press(FakeKeyboard.MENU_KEY_CODE); // Menu+wheel == zoom

    // Zoom out should work but does nothing.
    myUi.mouse.wheel(50, 50, 1);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(10000, myTimeline.getViewRange().getMax(), EPSILON);

    // Zoom in should still work
    myUi.mouse.wheel(50, 50, -1);
    myTimer.tick(TimeUnit.SECONDS.toNanos(5));
    assertEquals(0 + delta * 0.5, myTimeline.getViewRange().getMin(), EPSILON);
    assertEquals(10000 - delta * 0.5, myTimeline.getViewRange().getMax(), EPSILON);
  }

  @Test
  public void testNotScrollableIfViewRangeGreaterThatDataRange() {
    myTimeline.getDataRange().set(0, 100);
    myTimeline.getViewRange().set(-100, 200);

    assertFalse(myScrollbar.isScrollable());

    // We should not be able to pan if we are not scrollable.
    myUi.mouse.wheel(50, 50, 1);
    assertEquals(-100, myTimeline.getViewRange().getMin(), EPSILON);
    // View range should never be greater than data range.
    assertEquals(200, myTimeline.getViewRange().getMax(), EPSILON);
  }
}
