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
package com.android.tools.adtui.stdui

import com.android.tools.adtui.common.secondaryPanelBackground
import com.android.tools.adtui.model.stdui.CommonAction
import com.android.tools.adtui.model.stdui.CommonTextFieldModel
import com.android.tools.adtui.model.stdui.DefaultCommonComboBoxModel
import com.android.tools.adtui.model.stdui.DefaultCommonTextFieldModel
import com.android.tools.adtui.model.stdui.EDITOR_NO_ERROR
import com.android.tools.adtui.model.stdui.EditingErrorCategory
import com.android.tools.adtui.model.stdui.EditingSupport
import com.android.tools.adtui.model.stdui.EditorCompletion
import com.android.tools.adtui.stdui.menu.CommonDropDownButton
import com.intellij.icons.AllIcons
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.ui.laf.darcula.DarculaLaf
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.LookAndFeel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

/**
 * Tester for misc common controls.
 */
object CommonControlPortfolio {
  private val ourFont = UIUtil.getFontWithFallback("Ariel", 0, 12)

  @JvmStatic
  fun main(args: Array<String>) {
    IconLoader.activate()
    SwingUtilities.invokeLater { createAndShowGUI() }
  }

  private fun createAndShowGUI() {
    setLAF(IntelliJLaf())

    //Create and set up the window.
    val frame = JFrame("Common Controls")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

    //Set up the content pane.
    addComponentsToPane(frame.contentPane)
    updateFonts(frame)

    //Display the window.
    frame.pack()
    frame.setSize(500, frame.height)
    frame.isVisible = true
  }

  private fun addComponentsToPane(contentPane: Container) {
    var grid = JPanel(VerticalFlowLayout())
    grid.add(makeTextField("Normal"))
    grid.add(makeTextField("Disabled"))
    grid.add(makeTextField("Error"))
    grid.add(makeTextField(""))

    val topPanel = CommonTabbedPane()
    topPanel.add(grid, "TextField")

    grid = JPanel(VerticalFlowLayout())
    grid.add(makeComboBox("zero", enabled = true, editable = true))
    grid.add(makeComboBox("zero", enabled = true, editable = false))
    topPanel.add(grid, "ComboBox")

    val menuPanel = JPanel(VerticalFlowLayout())
    val label = JLabel()
    val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    toolBarPanel.add(makeDropDownMenu("MenuWithIconArrow", AllIcons.General.Add, true, 100, 2, label))
    toolBarPanel.add(makeDropDownMenu("MenuNoIcon", null, false, 100, 2, label))
    toolBarPanel.add(makeDropDownMenu("", AllIcons.ToolbarDecorator.Export, true, 2, 2, label))
    toolBarPanel.add(makeDropDownMenu("", AllIcons.General.Filter, false, 2, 2, label))
    menuPanel.add(toolBarPanel, BorderLayout.PAGE_START)
    menuPanel.add(label, BorderLayout.CENTER)
    topPanel.add(menuPanel, "Menus")

    grid = JPanel()
    grid.layout = GridLayout(2, 2, 5, 5)
    grid.border = JBUI.Borders.empty(20, 20, 20, 20)

    listOf(SwingConstants.TOP, SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.BOTTOM).forEach {
      val tab = CommonTabbedPane()
      tab.border = BorderFactory.createLineBorder(StandardColors.TAB_BORDER_COLOR, 1)
      tab.tabPlacement = it
      listOf("One", "Two", "Three").forEach { tab.add(JLabel("Label $it"), it) }
      grid.add(tab)
    }
    topPanel.add(grid, "Tabs")

    contentPane.layout = BorderLayout()
    contentPane.add(topPanel, BorderLayout.CENTER)
    contentPane.add(makeLAFControl(), BorderLayout.SOUTH)
    contentPane.background = secondaryPanelBackground
  }

  private fun makeLAFControl(): JComponent {
    val control = JCheckBox("Darcula")
    control.addItemListener { _ ->
      val laf = if (control.isSelected) DarculaLaf() else IntelliJLaf()
      if (laf !== UIManager.getLookAndFeel()) {
        setLAF(laf)
        updateFonts(SwingUtilities.getWindowAncestor(control))
        SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(control))
      }
    }
    return control
  }

  private fun setLAF(laf: LookAndFeel) {
    try {
      UIManager.setLookAndFeel(laf)
      JBColor.setDark(UIUtil.isUnderDarcula())
    }
    catch (ex: Exception) {
      ex.printStackTrace()
    }
  }

  private fun updateFonts(component: Component) {
    component.font = ourFont
    if (component is Container) {
      for (child in component.components) {
        updateFonts(child)
      }
    }
  }

  private fun makeTextField(text: String): JComponent {
    val model = TestCommonTextFieldModel(text)
    model.placeHolderValue = "@+id/name"
    val field = CommonTextField(model)
    field.isOpaque = false
    if (text == "Disabled") {
      field.isEnabled = false
    }
    return field
  }

  private fun makeComboBox(initialValue: String, enabled: Boolean, editable: Boolean): JComponent {
    val model = TestCommonComboBoxModel(initialValue, listOf("one", "two", "three", "four", "five", "six"))
    model.enabled = enabled
    model.editable = editable
    model.placeHolderValue = "@+id/name"

    val combo = CommonComboBox(model)
    combo.isOpaque = false
    combo.updateUI()
    return combo
  }

  private fun makeDropDownMenu(text: String, icon: Icon?, showArrow: Boolean, width: Int, depth: Int, label: JLabel): JComponent {
    val model = CommonAction(text, icon, null)
    model.showExpandArrow = showArrow

    for (i in 0 until width) {
      if (i % 2 == 0) {
        val action = CommonAction(text, icon)
        action.setAction {
          action.isSelected = !action.isSelected
          label.text = String.format("Clicked: %s", action)
          label.text = String.format("Clicked: %s", action)
        }
        model.addChildrenActions(action, CommonAction.SeparatorAction())
      }
      else {
        val action = CommonAction(text, icon)
        populateCommonActionRecursive(action, text, icon, width, depth - 1, label)
        model.addChildrenActions(action)
      }
    }

    return CommonDropDownButton(model)
  }

  private fun populateCommonActionRecursive(parent: CommonAction, text: String, icon: Icon?, width: Int, depth: Int, label: JLabel) {
    for (i in 0 until width) {
      if (i % 2 == 0 || depth - 1 == 0) {
        val action = CommonAction(text, icon)
        action.setAction {
          action.isSelected = !action.isSelected
          label.text = String.format("Clicked: %s", action)
        }
        parent.addChildrenActions(action, CommonAction.SeparatorAction())
      }
      else {
        val action = CommonAction(text, icon)
        populateCommonActionRecursive(action, text, icon, width, depth - 1, label)
        parent.addChildrenActions(action)
      }
    }
  }
}

class TestEditingSupport(val model: CommonTextFieldModel): EditingSupport {

  override val validation = fun(editedValue: String?): Pair<EditingErrorCategory, String> {
    return when (editedValue ?: model.value) {
      "Error" -> Pair(EditingErrorCategory.ERROR, "Error is not a valid value")
      "Warning" -> Pair(EditingErrorCategory.WARNING, "Be careful about warnings")
      else -> EDITOR_NO_ERROR
    }
  }

  override var allowCustomValues: Boolean = true

  override val completion: EditorCompletion = {
    listOf(
      "@string/almond",
      "@string/app_firstName",
      "@string/app_name",
      "@string/app_name1",
      "@string/app_name2",
      "@string/app_name3",
      "@string/app_name4",
      "@string/app_name5",
      "@string/app_name6",
      "@string/app_name7",
      "@string/app_name8",
      "@string/app_name9",
      "@string/app_name10",
      "@string/app_name11",
      "@string/app_name12",
      "@string/app_name13",
      "@string/app_name14",
      "@string/app_name15",
      "@string/app_name16",
      "@string/app_name17",
      "@string/app_name18",
      "@string/app_name19",
      "@string/app_name20",
      "@string/app_name21",
      "@string/app_name22",
      "@string/app_name23",
      "@string/app_name24",
      "@string/app_name25",
      "@string/app_name26",
      "@string/app_name27",
      "@string/app_name28",
      "@string/app_name29",
      "@string/app_name30",
      "@string/appelsin",
      "@string/apricot",
      "@android:string/paste_as_plain_text",
      "@android:string/hello")
  }
}

class TestCommonTextFieldModel(initialValue: String) : DefaultCommonTextFieldModel(initialValue) {
  override val editingSupport = TestEditingSupport(this)
}

class TestCommonComboBoxModel(initialValue: String, elements: List<String>)
  : DefaultCommonComboBoxModel<String>(initialValue, elements) {
  override val editingSupport = TestEditingSupport(this)
}
