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
package com.android.tools.idea.emulator

import com.android.emulator.control.DisplayConfiguration
import com.android.emulator.control.DisplayConfigurations
import com.android.emulator.control.ExtendedControlsStatus
import com.android.tools.adtui.ZOOMABLE_KEY
import com.android.tools.adtui.common.primaryPanelBackground
import com.android.tools.adtui.util.ActionToolbarUtil.makeToolbarNavigable
import com.android.tools.idea.emulator.EmulatorController.ConnectionState
import com.android.tools.idea.emulator.EmulatorController.ConnectionStateListener
import com.android.tools.idea.emulator.actions.findManageSnapshotDialog
import com.android.tools.idea.emulator.actions.showExtendedControls
import com.android.tools.idea.emulator.actions.showManageSnapshotsDialog
import com.android.utils.HashCodes
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Property
import icons.StudioIcons
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener
import java.nio.file.Path
import java.util.function.IntFunction
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


/**
 * Represents contents of the Emulator tool window for a single Emulator instance.
 */
class EmulatorToolWindowPanel(
  private val project: Project,
  val emulator: EmulatorController
) : BorderLayoutPanel(), DataProvider, ConnectionStateListener {

  private val mainToolbar: ActionToolbar
  private val centerPanel = BorderLayoutPanel()
  private val displayPanels = Int2ObjectRBTreeMap<EmulatorDisplayPanel>()
  private val displayConfigurator = DisplayConfigurator()
  private var clipboardSynchronizer: EmulatorClipboardSynchronizer? = null
  private var contentDisposable: Disposable? = null

  private var primaryEmulatorView: EmulatorView? = null
  private val multiDisplayStateStorage = MultiDisplayStateStorage.getInstance(project)
  private val multiDisplayStateUpdater = Runnable {
    multiDisplayStateStorage.setMultiDisplayState(id.avdFolder, displayConfigurator.getMultiDisplayState())
  }

  private val focusOwnerListener = PropertyChangeListener { event ->
    val newFocus = event.newValue
    val gained = newFocus is EmulatorView && newFocus.emulator == emulator
    val oldFocus = event.oldValue
    val lost = oldFocus is EmulatorView && oldFocus.emulator == emulator
    if (gained != lost) {
      if (gained) {
        if (connected) {
          clipboardSynchronizer?.setDeviceClipboardAndKeepHostClipboardInSync()
        }
      }
      else if (lost) {
        clipboardSynchronizer?.stopKeepingHostClipboardInSync()
      }
    }
  }

  val id
    get() = emulator.emulatorId

  val title
    get() = emulator.emulatorId.avdName

  val icon
    get() = ICON

  val component: JComponent
    get() = this

  var zoomToolbarVisible = false
    set(value) {
      field = value
      for (panel in displayPanels.values) {
        panel.zoomToolbarVisible = value
      }
    }

  private val connected
    get() = emulator.connectionState == ConnectionState.CONNECTED

  @TestOnly
  var lastUiState: UiState? = null

  init {
    background = primaryPanelBackground

    mainToolbar = createToolbar(EMULATOR_MAIN_TOOLBAR_ID, isToolbarHorizontal)

    addToCenter(centerPanel)
    addToolbar()
  }

  private fun addToolbar() {
    if (isToolbarHorizontal) {
      mainToolbar.setOrientation(SwingConstants.HORIZONTAL)
      centerPanel.border = IdeBorderFactory.createBorder(JBColor.border(), SideBorder.TOP)
      addToTop(mainToolbar.component)
    }
    else {
      mainToolbar.setOrientation(SwingConstants.VERTICAL)
      centerPanel.border = IdeBorderFactory.createBorder(JBColor.border(), SideBorder.LEFT)
      addToLeft(mainToolbar.component)
    }
  }

  fun getPreferredFocusableComponent(): JComponent {
    return primaryEmulatorView ?: this
  }

  fun setDeviceFrameVisible(visible: Boolean) {
    primaryEmulatorView?.deviceFrameVisible = visible
  }

  override fun connectionStateChanged(emulator: EmulatorController, connectionState: ConnectionState) {
    if (connectionState == ConnectionState.CONNECTED) {
      displayConfigurator.refreshDisplayConfiguration()
      EventQueue.invokeLater {
        if (isFocusOwner) {
          clipboardSynchronizer?.setDeviceClipboardAndKeepHostClipboardInSync()
        }
      }
    }
  }

  /**
   * Populates the emulator panel with content.
   */
  fun createContent(deviceFrameVisible: Boolean, savedUiState: UiState? = null) {
    try {
      lastUiState = null
      val disposable = Disposer.newDisposable()
      contentDisposable = disposable

      clipboardSynchronizer = EmulatorClipboardSynchronizer(emulator)

      val primaryDisplayPanel =
          EmulatorDisplayPanel(disposable, emulator, PRIMARY_DISPLAY_ID, null, zoomToolbarVisible, deviceFrameVisible)
      displayPanels[primaryDisplayPanel.displayId] = primaryDisplayPanel
      val emulatorView = primaryDisplayPanel.emulatorView
      primaryEmulatorView = emulatorView
      mainToolbar.setTargetComponent(emulatorView)
      installFileDropHandler(this, emulatorView, project)
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusOwnerListener)
      emulatorView.addDisplayConfigurationListener(displayConfigurator)
      emulator.addConnectionStateListener(this)

      val multiDisplayState = multiDisplayStateStorage.getMultiDisplayState(id.avdFolder)
      if (multiDisplayState == null) {
        centerPanel.addToCenter(primaryDisplayPanel)
      }
      else {
        try {
          displayConfigurator.buildLayout(multiDisplayState)
        }
        catch (e: RuntimeException) {
          thisLogger().error("Corrupted multi-display state", e)
          // Corrupted multi-display state. Start with a single display.
          centerPanel.addToCenter(primaryDisplayPanel)
        }
      }

      val uiState = savedUiState ?: UiState()
      val zoomScrollState = uiState.zoomScrollState
      for (panel in displayPanels.values) {
        zoomScrollState[panel.displayId]?.let { panel.zoomScrollState = it }
      }

      multiDisplayStateStorage.addUpdater(multiDisplayStateUpdater)

      if (connected) {
        displayConfigurator.refreshDisplayConfiguration()

        if (uiState.manageSnapshotsDialogShown) {
          showManageSnapshotsDialog(emulatorView, project)
        }
        if (uiState.extendedControlsShown) {
          showExtendedControls(emulator, project)
        }
      }
    }
    catch (e: Exception) {
      val label = "Unable to create emulator view: $e"
      add(JLabel(label), BorderLayout.CENTER)
    }
  }

  /**
   * Destroys content of the emulator panel and returns its state for later recreation.
   */
  fun destroyContent(): UiState {
    multiDisplayStateUpdater.run()
    multiDisplayStateStorage.removeUpdater(multiDisplayStateUpdater)

    val uiState = UiState()
    for (panel in displayPanels.values) {
      uiState.zoomScrollState[panel.displayId] = panel.zoomScrollState
    }

    val manageSnapshotsDialog = primaryEmulatorView?.let { findManageSnapshotDialog(it) }
    uiState.manageSnapshotsDialogShown = manageSnapshotsDialog != null
    manageSnapshotsDialog?.close(DialogWrapper.CLOSE_EXIT_CODE)

    if (connected) {
      emulator.closeExtendedControls(object: EmptyStreamObserver<ExtendedControlsStatus>() {
        override fun onNext(response: ExtendedControlsStatus) {
          EventQueue.invokeLater {
            uiState.extendedControlsShown = response.visibilityChanged
          }
        }
      })
    }

    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusOwnerListener)
    emulator.removeConnectionStateListener(this)
    contentDisposable?.let { Disposer.dispose(it) }
    contentDisposable = null

    centerPanel.removeAll()
    displayPanels.clear()
    primaryEmulatorView = null
    mainToolbar.setTargetComponent(null)
    clipboardSynchronizer = null
    lastUiState = uiState
    return uiState
  }

  override fun getData(dataId: String): Any? {
    return when (dataId) {
      EMULATOR_CONTROLLER_KEY.name -> emulator
      EMULATOR_VIEW_KEY.name, ZOOMABLE_KEY.name -> primaryEmulatorView
      NUMBER_OF_DISPLAYS.name -> displayPanels.size
      else -> null
    }
  }

  @Suppress("SameParameterValue")
  private fun createToolbar(toolbarId: String, horizontal: Boolean): ActionToolbar {
    val actions = listOf(CustomActionsSchema.getInstance().getCorrectedAction(toolbarId)!!)
    val toolbar = ActionManager.getInstance().createActionToolbar(toolbarId, DefaultActionGroup(actions), horizontal)
    toolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
    makeToolbarNavigable(toolbar)
    return toolbar
  }

  private inner class DisplayConfigurator : DisplayConfigurationListener {

    var displayDescriptors = emptyList<DisplayDescriptor>()

    override fun displayConfigurationChanged() {
      refreshDisplayConfiguration()
    }

    fun refreshDisplayConfiguration() {
      emulator.getDisplayConfigurations(object : EmptyStreamObserver<DisplayConfigurations>() {
        override fun onNext(response: DisplayConfigurations) {
          EventQueue.invokeLater {
            displayConfigurationReceived(response.displaysList)
          }
        }
      })
    }

    private fun displayConfigurationReceived(displayConfigs: List<DisplayConfiguration>) {
      val primaryDisplayView = primaryEmulatorView ?: return
      val newDisplays = getDisplayDescriptors(primaryDisplayView, displayConfigs)
      if (newDisplays.size == 1 && displayDescriptors.size <= 1 || newDisplays == displayDescriptors) {
        return
      }

      displayPanels.int2ObjectEntrySet().removeIf { (displayId, displayPanel) ->
        if (!newDisplays.any { it.displayId == displayId }) {
          Disposer.dispose(displayPanel)
          true
        }
        else {
          false
        }
      }
      val layoutRoot = computeBestLayout(centerPanel.sizeWithoutInsets, newDisplays.map { it.size })
      val rootPanel = buildLayout(layoutRoot, newDisplays)
      displayDescriptors = newDisplays
      setRootPanel(rootPanel)
    }

    fun buildLayout(multiDisplayState: MultiDisplayState) {
      val newDisplays = multiDisplayState.displayDescriptors
      val rootPanel = buildLayout(multiDisplayState.emulatorPanelState, newDisplays)
      displayDescriptors = newDisplays
      setRootPanel(rootPanel)
    }

    private fun buildLayout(layoutNode: LayoutNode, displayDescriptors: List<DisplayDescriptor>): JPanel {
      return when (layoutNode) {
        is LeafNode -> {
          val display = displayDescriptors[layoutNode.rectangleIndex]
          val displayId = display.displayId
          displayPanels.computeIfAbsent(displayId, IntFunction {
            assert(displayId != PRIMARY_DISPLAY_ID)
            EmulatorDisplayPanel(contentDisposable!!, emulator, displayId, display.size, zoomToolbarVisible)
          })
        }
        is SplitNode -> {
          EmulatorSplitPanel(layoutNode).apply {
            firstComponent = buildLayout(layoutNode.firstChild, displayDescriptors)
            secondComponent = buildLayout(layoutNode.secondChild, displayDescriptors)
          }
        }
      }
    }

    private fun buildLayout(state: EmulatorPanelState, displayDescriptors: List<DisplayDescriptor>): JPanel {
      val splitPanelState = state.splitPanel
      return if (splitPanelState != null) {
        EmulatorSplitPanel(splitPanelState.splitType, splitPanelState.proportion).apply {
          firstComponent = buildLayout(splitPanelState.firstComponent, displayDescriptors)
          secondComponent = buildLayout(splitPanelState.secondComponent, displayDescriptors)
        }
      }
      else {
        val displayId = state.displayId ?: throw IllegalArgumentException()
        val display = displayDescriptors.find { it.displayId == displayId } ?: throw IllegalArgumentException()
        displayPanels.computeIfAbsent(displayId, IntFunction {
          assert(displayId != PRIMARY_DISPLAY_ID)
          EmulatorDisplayPanel(contentDisposable!!, emulator, displayId, display.size, zoomToolbarVisible)
        })
      }
    }

    private fun setRootPanel(rootPanel: JPanel) {
      mainToolbar.updateActionsImmediately() // Rotation buttons are hidden in multi-display mode.
      centerPanel.removeAll()
      centerPanel.addToCenter(rootPanel)
      centerPanel.validate()
    }

    private fun getDisplayDescriptors(emulatorView: EmulatorView, displays: List<DisplayConfiguration>): List<DisplayDescriptor> {
      return displays
        .map {
          if (it.display == PRIMARY_DISPLAY_ID) {
            DisplayDescriptor(PRIMARY_DISPLAY_ID, emulatorView.displaySizeWithFrame)
          }
          else {
            DisplayDescriptor(it)
          }
        }
        .sorted()
    }

    fun getMultiDisplayState(): MultiDisplayState? {
      if (centerPanel.componentCount > 0) {
        val panel = centerPanel.getComponent(0)
        if (panel is EmulatorSplitPanel) {
          return MultiDisplayState(displayDescriptors.toMutableList(), panel.getState())
        }
      }
      return null
    }
  }

  data class DisplayDescriptor(var displayId: Int, var width: Int, var height: Int) : Comparable<DisplayDescriptor> {

    constructor(displayConfig: DisplayConfiguration) : this(displayConfig.display, displayConfig.width, displayConfig.height)

    constructor(displayId: Int, size: Dimension) : this(displayId, size.width, size.height)

    @Suppress("unused") // Used by XML deserializer.
    constructor() : this(0, 0, 0)

    val size
      get() = Dimension(width, height)

    override fun compareTo(other: DisplayDescriptor): Int {
      return displayId - other.displayId
    }
  }

  class UiState {
    var manageSnapshotsDialogShown = false
    var extendedControlsShown = false
    var multiDisplayState: MultiDisplayState? = null
    val zoomScrollState = Int2ObjectRBTreeMap<EmulatorDisplayPanel.ZoomScrollState>()
  }

  /**
   * Persistent multi-display state corresponding to a single AVD.
   * The no-argument constructor is used by the XML deserializer.
   */
  class MultiDisplayState() {

    constructor(displayDescriptors: MutableList<DisplayDescriptor>, emulatorPanelState: EmulatorPanelState) : this() {
      this.displayDescriptors = displayDescriptors
      this.emulatorPanelState = emulatorPanelState
    }

    lateinit var displayDescriptors: MutableList<DisplayDescriptor>
    lateinit var emulatorPanelState: EmulatorPanelState

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as MultiDisplayState
      return displayDescriptors == other.displayDescriptors && emulatorPanelState == other.emulatorPanelState
    }

    override fun hashCode(): Int {
      return HashCodes.mix(displayDescriptors.hashCode(), emulatorPanelState.hashCode())
    }
  }

  @State(name = "EmulatorDisplays", storages = [Storage("emulatorDisplays.xml")])
  class MultiDisplayStateStorage : PersistentStateComponent<MultiDisplayStateStorage> {

    @get:Property(surroundWithTag = true)
    var displayStateByAvdFolder = mutableMapOf<String, MultiDisplayState>()

    private val updaters = mutableListOf<Runnable>()

    override fun getState(): MultiDisplayStateStorage {
      for (updater in updaters) {
        updater.run()
      }
      return this
    }

    override fun loadState(state: MultiDisplayStateStorage) {
      XmlSerializerUtil.copyBean(state, this)
    }

    fun addUpdater(updater: Runnable) {
      updaters.add(updater)
    }

    fun removeUpdater(updater: Runnable) {
      updaters.remove(updater)
    }

    fun getMultiDisplayState(avdFolder: Path): MultiDisplayState? = displayStateByAvdFolder[avdFolder.toString()]

    fun setMultiDisplayState(avdFolder: Path, state: MultiDisplayState?) {
      if (state == null) {
        displayStateByAvdFolder.remove(avdFolder.toString())
      }
      else {
        displayStateByAvdFolder[avdFolder.toString()] = state
      }
    }

    companion object {
      @JvmStatic
      fun getInstance(project: Project): MultiDisplayStateStorage {
        return ServiceManager.getService(project, MultiDisplayStateStorage::class.java)
      }
    }
  }
}

private val ICON = ExecutionUtil.getLiveIndicator(StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE)
private const val isToolbarHorizontal = true