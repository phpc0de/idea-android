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
package com.android.tools.idea.appinspection.inspectors.network.view

import com.android.tools.adtui.common.AdtUiUtils.DEFAULT_BOTTOM_BORDER
import com.android.tools.adtui.flat.FlatSeparator
import com.android.tools.adtui.model.AspectObserver
import com.android.tools.adtui.model.FpsTimer
import com.android.tools.adtui.model.StopwatchTimer
import com.android.tools.adtui.model.StreamingTimeline
import com.android.tools.adtui.model.Timeline
import com.android.tools.adtui.stdui.CommonButton
import com.android.tools.adtui.stdui.CommonToggleButton
import com.android.tools.adtui.stdui.DefaultContextMenuItem
import com.android.tools.adtui.stdui.TooltipLayeredPane
import com.android.tools.idea.appinspection.inspectors.network.model.CodeNavigationProvider
import com.android.tools.idea.appinspection.inspectors.network.model.NetworkInspectorClient
import com.android.tools.idea.appinspection.inspectors.network.model.NetworkInspectorDataSource
import com.android.tools.idea.appinspection.inspectors.network.model.NetworkInspectorModel
import com.android.tools.idea.appinspection.inspectors.network.model.NetworkInspectorServices
import com.android.tools.idea.appinspection.inspectors.network.view.constants.DEFAULT_BACKGROUND
import com.android.tools.idea.appinspection.inspectors.network.view.constants.H4_FONT
import com.android.tools.idea.appinspection.inspectors.network.view.constants.TOOLBAR_HEIGHT
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ThreeComponentsSplitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.JBEmptyBorder
import icons.StudioIcons
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.META_DOWN_MASK
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingConstants


private const val ZOOM_IN = "Zoom in"
private const val ZOOM_OUT = "Zoom out"
private const val ATTACH_LIVE = "Attach to live"
private const val DETACH_LIVE = "Detach live"
private val SHORTCUT_MODIFIER_MASK_NUMBER = if (SystemInfo.isMac) META_DOWN_MASK else CTRL_DOWN_MASK

/**
 * The number of updates per second our simulated object models receive.
 */
private const val UPDATE_RATE = 60

class NetworkInspectorTab(
  private val client: NetworkInspectorClient,
  scope: CoroutineScope,
  private val componentsProvider: UiComponentsProvider,
  private val codeNavigationProvider: CodeNavigationProvider,
  private val dataSource: NetworkInspectorDataSource,
  private val uiDispatcher: CoroutineDispatcher,
  parentDisposable: Disposable,
  private val timer: StopwatchTimer = FpsTimer(UPDATE_RATE)
) : AspectObserver(), Disposable {
  private lateinit var inspectorServices: NetworkInspectorServices

  @VisibleForTesting
  lateinit var model: NetworkInspectorModel
  private lateinit var view: NetworkInspectorView
  val component: TooltipLayeredPane

  @VisibleForTesting
  lateinit var actionsToolBar: JPanel

  @VisibleForTesting
  val launchJob: Job

  init {
    Disposer.register(parentDisposable, this)
    val parentPanel = JPanel(BorderLayout())
    parentPanel.background = DEFAULT_BACKGROUND
    val splitter = ThreeComponentsSplitter(parentDisposable)
    splitter.focusTraversalPolicy = LayoutFocusTraversalPolicy()
    splitter.dividerWidth = 0
    splitter.setDividerMouseZoneSize(-1)
    splitter.setHonorComponentsMinimumSize(true)
    splitter.lastComponent = parentPanel

    component = TooltipLayeredPane(splitter)
    launchJob = scope.launch {
      val deviceTime = client.getStartTimeStampNs()
      withContext(uiDispatcher) {
        val stagePanel = JPanel(BorderLayout())
        val toolbar = JPanel(BorderLayout())
        toolbar.border = DEFAULT_BOTTOM_BORDER
        toolbar.preferredSize = Dimension(0, TOOLBAR_HEIGHT)

        parentPanel.add(toolbar, BorderLayout.NORTH)
        parentPanel.add(stagePanel, BorderLayout.CENTER)

        inspectorServices = NetworkInspectorServices(codeNavigationProvider, deviceTime, timer)
        model = NetworkInspectorModel(inspectorServices, dataSource)
        view = NetworkInspectorView(model, componentsProvider, component)
        stagePanel.add(view.component)

        actionsToolBar = JPanel(GridBagLayout())
        toolbar.add(actionsToolBar, BorderLayout.EAST)
        actionsToolBar.border = JBEmptyBorder(0, 0, 0, 2)

        val zoomOut = CommonButton(AllIcons.General.ZoomOut)
        zoomOut.disabledIcon = IconLoader.getDisabledIcon(AllIcons.General.ZoomOut)
        zoomOut.addActionListener {
          model.timeline.zoomOut()
        }
        val zoomOutAction = DefaultContextMenuItem.Builder(ZOOM_OUT)
          .setContainerComponent(splitter)
          .setActionRunnable { zoomOut.doClick(0) }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT_MODIFIER_MASK_NUMBER),
                         KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, SHORTCUT_MODIFIER_MASK_NUMBER))
          .build()

        zoomOut.toolTipText = zoomOutAction.defaultToolTipText
        actionsToolBar.add(zoomOut)

        val zoomIn = CommonButton(AllIcons.General.ZoomIn)
        zoomIn.disabledIcon = IconLoader.getDisabledIcon(AllIcons.General.ZoomIn)
        zoomIn.addActionListener {
          model.timeline.zoomIn()
        }
        val zoomInAction = DefaultContextMenuItem.Builder(ZOOM_IN)
          .setContainerComponent(splitter)
          .setActionRunnable { zoomIn.doClick(0) }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, SHORTCUT_MODIFIER_MASK_NUMBER),
                         KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT_MODIFIER_MASK_NUMBER),
                         KeyStroke.getKeyStroke(KeyEvent.VK_ADD, SHORTCUT_MODIFIER_MASK_NUMBER)).build()
        zoomIn.toolTipText = zoomInAction.defaultToolTipText
        actionsToolBar.add(zoomIn)

        val resetZoom = CommonButton(StudioIcons.Common.RESET_ZOOM)
        resetZoom.disabledIcon = IconLoader.getDisabledIcon(StudioIcons.Common.RESET_ZOOM)
        resetZoom.addActionListener {
          model.timeline.resetZoom()
        }
        val resetZoomAction = DefaultContextMenuItem.Builder("Reset zoom")
          .setContainerComponent(splitter)
          .setActionRunnable { resetZoom.doClick(0) }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0),
                         KeyStroke.getKeyStroke(KeyEvent.VK_0, 0)).build()
        resetZoom.toolTipText = resetZoomAction.defaultToolTipText
        actionsToolBar.add(resetZoom)

        val zoomToSelection = CommonButton(StudioIcons.Common.ZOOM_SELECT)
        zoomToSelection.disabledIcon = IconLoader.getDisabledIcon(StudioIcons.Common.ZOOM_SELECT)
        zoomToSelection.addActionListener {
          model.timeline.frameViewToRange(model.timeline.selectionRange)
        }
        val zoomToSelectionAction = DefaultContextMenuItem.Builder("Zoom to Selection")
          .setContainerComponent(splitter)
          .setActionRunnable { zoomToSelection.doClick(0) }
          .setEnableBooleanSupplier { model.timeline.selectionRange.isEmpty }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0))
          .build()
        zoomToSelection.toolTipText = zoomToSelectionAction.defaultToolTipText
        actionsToolBar.add(zoomToSelection)

        val goLiveToolbar = JPanel(GridBagLayout())
        goLiveToolbar.add(FlatSeparator())

        val goLive = CommonToggleButton("", StudioIcons.Profiler.Toolbar.GOTO_LIVE)
        goLive.disabledIcon = IconLoader.getDisabledIcon(StudioIcons.Profiler.Toolbar.GOTO_LIVE)
        goLive.font = H4_FONT
        goLive.horizontalTextPosition = SwingConstants.LEFT
        goLive.horizontalAlignment = SwingConstants.LEFT
        goLive.border = JBEmptyBorder(3, 7, 3, 7)
        val attachAction = DefaultContextMenuItem.Builder(ATTACH_LIVE)
          .setContainerComponent(splitter)
          .setActionRunnable { goLive.doClick(0) }
          .setEnableBooleanSupplier {
            goLive.isEnabled &&
            !goLive.isSelected
          }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, SHORTCUT_MODIFIER_MASK_NUMBER))
          .build()
        val detachAction = DefaultContextMenuItem.Builder(DETACH_LIVE)
          .setContainerComponent(splitter)
          .setActionRunnable { goLive.doClick(0) }
          .setEnableBooleanSupplier {
            goLive.isEnabled &&
            goLive.isSelected
          }
          .setKeyStrokes(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)).build()

        goLive.toolTipText = detachAction.defaultToolTipText
        goLive.addActionListener {
          val currentStageTimeline: Timeline = model.timeline
          assert(currentStageTimeline is StreamingTimeline)
          (currentStageTimeline as StreamingTimeline).toggleStreaming()
        }
        goLive.addChangeListener {
          val isSelected: Boolean = goLive.isSelected
          goLive.icon = if (isSelected) StudioIcons.Profiler.Toolbar.PAUSE_LIVE else StudioIcons.Profiler.Toolbar.GOTO_LIVE
          goLive.toolTipText = if (isSelected) detachAction.defaultToolTipText else attachAction.defaultToolTipText
        }
        inspectorServices.timeline.addDependency(this@NetworkInspectorTab).onChange(
          StreamingTimeline.Aspect.STREAMING) { goLive.isSelected = inspectorServices.timeline.isStreaming }
        goLiveToolbar.add(goLive)
        actionsToolBar.add(goLiveToolbar)


        zoomOut.isEnabled = true
        zoomIn.isEnabled = true
        resetZoom.isEnabled = true
        zoomToSelection.isEnabled = zoomToSelectionAction.isEnabled
        goLive.isEnabled = true
        goLive.isSelected = true
      }
    }
  }

  override fun dispose() {
    timer.stop()
  }
}