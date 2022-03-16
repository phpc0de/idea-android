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
package com.android.tools.idea.ui.resourcechooser.colorpicker2

import com.google.common.annotations.VisibleForTesting
import com.intellij.ui.JBColor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.math.max

private const val PADDING = 5

private val KNOB_COLOR = Color(255, 255, 255)
private val KNOB_BORDER_COLOR = JBColor(Color(100, 100, 100), Color(64, 64, 64))
private val KNOB_BORDER_STROKE = BasicStroke(1.5f)

private const val ACTION_SLIDE_LEFT = "actionSlideLeft"
private const val ACTION_SLIDE_LEFT_STEP = "actionSlideLeftStep"
private const val ACTION_SLIDE_RIGHT = "actionSlideRight"
private const val ACTION_SLIDE_RIGHT_STEP = "actionSlideRightStep"

abstract class SliderComponent<T: Number>(initialValue: T) : JComponent() {

  private val knobWidth = JBUI.scale(5)
  private val knobCornerArc = JBUI.scale(5)
  private val focusBorderWidth = JBUI.scale(3)
  private val focusBorderCornerArc = JBUI.scale(5)

  protected val leftPadding = JBUI.scale(PADDING)
  protected val rightPadding = JBUI.scale(PADDING)
  protected val topPadding = JBUI.scale(PADDING)
  protected val bottomPadding = JBUI.scale(PADDING)

  private var _knobPosition: Int = 0
  var knobPosition: Int
    get() = _knobPosition
    @VisibleForTesting
    set(newPointerValue) {
      _knobPosition = newPointerValue
      _value = knobPositionToValue(newPointerValue)
    }

  private var _value: T = initialValue
  var value: T
    get() = _value
    set(newValue) {
      _value = newValue
      _knobPosition = valueToKnobPosition(newValue)
    }

  private val polygonToDraw = Polygon()

  private val listeners = ContainerUtil.createLockFreeCopyOnWriteList<(T, Boolean) -> Unit>()

  /**
   * @return size of slider, must be positive value or zero.
   */
  @VisibleForTesting
  val sliderWidth get() = max(0, width - leftPadding - rightPadding)

  init {
    this.addMouseMotionListener(object : MouseAdapter() {
      override fun mouseDragged(e: MouseEvent) {
        processMouse(e, false)
        e.consume()
      }
    })

    this.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        processMouse(e, false)
        e.consume()
      }

      override fun mouseReleased(e: MouseEvent) {
        processMouse(e, true)
        e.consume()
      }
    })

    addMouseWheelListener { e ->
      runAndUpdateIfNeeded({
        val amount = when {
          e.scrollType == MouseWheelEvent.WHEEL_UNIT_SCROLL -> e.unitsToScroll * e.scrollAmount
          e.wheelRotation < 0 -> -e.scrollAmount
          else -> e.scrollAmount
        }
        val newKnobPosition = Math.max(0, Math.min(_knobPosition + amount, sliderWidth))
        knobPosition = newKnobPosition
      }, true)
      e.consume()
    }

    this.addFocusListener(object : FocusAdapter() {
      override fun focusGained(e: FocusEvent?) {
        repaint()
      }
      override fun focusLost(e: FocusEvent?) {
        repaint()
      }
    })

    this.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        repaint()
      }
    })

    with (actionMap) {
      put(ACTION_SLIDE_LEFT, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) = runAndUpdateIfNeeded({ doSlide(-1) }, true)
      })
      put(ACTION_SLIDE_LEFT_STEP, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) = runAndUpdateIfNeeded({ doSlide(-10) }, true)
      })
      put(ACTION_SLIDE_RIGHT, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) = runAndUpdateIfNeeded({ doSlide(1) }, true)
      })
      put(ACTION_SLIDE_RIGHT_STEP, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) = runAndUpdateIfNeeded({ doSlide(10) }, true)
      })
    }

    with (getInputMap(WHEN_FOCUSED)) {
      put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ACTION_SLIDE_LEFT)
      put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), ACTION_SLIDE_LEFT_STEP)
      put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ACTION_SLIDE_RIGHT)
      put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), ACTION_SLIDE_RIGHT_STEP)
    }
  }

  /**
   * Helper function to execute the code and check if needs to update the color
   */
  private fun runAndUpdateIfNeeded(task: () -> Unit, commit: Boolean = false) {
    val oldValue = value
    task()
    repaint()
    if (oldValue != value || commit) {
      listeners.forEach { it.invoke(value, commit) }
    }
  }

  private fun processMouse(e: MouseEvent, commit: Boolean) = runAndUpdateIfNeeded({
    val newKnobPosition = Math.max(0, Math.min(e.x - leftPadding, sliderWidth))
    knobPosition = newKnobPosition
  }, commit)

  fun addListener(listener: (T, Boolean) -> Unit) {
    listeners.add(listener)
  }

  protected abstract fun knobPositionToValue(knobPosition: Int): T

  protected abstract fun valueToKnobPosition(value: T): Int

  private fun doSlide(shift: Int) {
    value = slide(shift)
  }

  /**
   * return the new value after sliding. The [shift] is the amount of sliding.
   */
  protected abstract fun slide(shift: Int): T

  override fun getPreferredSize(): Dimension = JBUI.size(100, 22)

  override fun getMinimumSize(): Dimension = JBUI.size(50, 22)

  override fun getMaximumSize(): Dimension = Dimension(Integer.MAX_VALUE, preferredSize.height)

  override fun isFocusable() = true

  override fun setToolTipText(text: String) = Unit

  override fun paintComponent(g: Graphics) {
    val g2d = g as Graphics2D
    if (isFocusOwner) {
      g2d.color = UIUtil.getFocusedFillColor()
      val left = leftPadding - focusBorderWidth
      val top = topPadding - focusBorderWidth
      val width = width - left - rightPadding + focusBorderWidth
      val height = height - top - bottomPadding + focusBorderWidth
      g2d.fillRoundRect(left, top, width, height, focusBorderCornerArc, focusBorderCornerArc)
    }
    paintSlider(g2d)
    drawKnob(g2d, leftPadding + valueToKnobPosition(value))
  }

  protected abstract fun paintSlider(g2d: Graphics2D)

  private fun drawKnob(g2d: Graphics2D, x: Int) {
    val originalAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
    val originalStroke = g2d.stroke

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val knobLeft = x - knobWidth / 2
    val knobTop = topPadding / 2
    val knobWidth = knobWidth
    val knobHeight = height - (topPadding + bottomPadding) / 2

    g2d.color = KNOB_COLOR
    g2d.fillRoundRect(knobLeft, knobTop, knobWidth, knobHeight, knobCornerArc, knobCornerArc)
    g2d.color = KNOB_BORDER_COLOR
    g2d.stroke = KNOB_BORDER_STROKE
    g2d.drawRoundRect(knobLeft, knobTop, knobWidth, knobHeight, knobCornerArc, knobCornerArc)

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialiasing)
    g2d.stroke = originalStroke
  }
}
