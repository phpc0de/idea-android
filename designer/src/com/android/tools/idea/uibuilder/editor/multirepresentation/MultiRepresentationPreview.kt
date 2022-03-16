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
package com.android.tools.idea.uibuilder.editor.multirepresentation

import com.android.tools.adtui.actions.DropDownAction
import com.android.tools.adtui.common.AdtPrimaryPanel
import com.android.tools.adtui.util.ActionToolbarUtil
import com.android.tools.idea.common.editor.DesignFileEditor
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.ui.UIUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Tag
import icons.StudioIcons
import org.jetbrains.android.util.AndroidSlowOperations
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BorderFactory
import javax.swing.JComponent

/**
 * Tag name used to persist the multi preview state.
 */
internal const val MULTI_PREVIEW_STATE_TAG = "multi-preview-state"

/**
 * Type for [PreviewRepresentation]s to store their settings as key/value pairs.
 */
typealias PreviewRepresentationState = Map<String, String>

/**
 * Bean for saving representation states.
 */
@Tag("representation")
data class Representation(
  @Attribute("name") var key: RepresentationName = "",
  @Tag("settings")
  @MapAnnotation(entryTagName = "setting", keyAttributeName = "name", valueAttributeName = "value", surroundWithTag = false)
  var settings: PreviewRepresentationState = mutableMapOf())

/**
 * [FileEditorState] for [MultiRepresentationPreview]. It saves the state of the individual [PreviewRepresentation]s and restore the state
 * of each one.
 */
@Tag(MULTI_PREVIEW_STATE_TAG)
data class MultiRepresentationPreviewFileEditorState(
  @Attribute("selected")
  var selectedRepresentationName: RepresentationName = "",
  @Tag("representations")
  var representations: Collection<Representation> = mutableListOf()) : FileEditorState {
  override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean =
    otherState is MultiRepresentationPreviewFileEditorState && this == otherState

  companion object {
    val INSTANCE = MultiRepresentationPreviewFileEditorState("", listOf())
  }
}

/**
 * A generic preview [com.intellij.openapi.fileEditor.FileEditor] that allows you to switch between different [PreviewRepresentation]s.
 *
 * @param psiFile the file being edited by this editor.
 * @param editor the text [Editor] for the file.
 * @param providers list of [PreviewRepresentationProvider] for this file type.
 */
open class MultiRepresentationPreview(psiFile: PsiFile,
                                      private val editor: Editor,
                                      private val providers: Collection<PreviewRepresentationProvider>) :
  PreviewRepresentationManager, DesignFileEditor(psiFile.virtualFile!!) {
  private val LOG = Logger.getInstance(MultiRepresentationPreview::class.java)
  /** Id identifying this MultiRepresentationPreview to be used in logging */
  private val instanceId = psiFile.virtualFile.presentableName

  private val project = psiFile.project
  private val psiFilePointer = SmartPointerManager.createPointer(psiFile)
  private var shortcutsApplicableComponent: JComponent? = null

  private var representationNeverShown = true

  /**
   * [MultiRepresentationPreviewFileEditorState] deserialized from disk, if any.
   */
  private var stateFromDisk: MultiRepresentationPreviewFileEditorState? = null

  private val representations: MutableMap<RepresentationName, PreviewRepresentation> = mutableMapOf()

  override val currentRepresentation: PreviewRepresentation?
    get() {
      return representations[currentRepresentationName]
    }

  /**
   * true if the [currentRepresentation] has been activated.
   */
  private val currentRepresentationIsActive = AtomicBoolean(false)

  override val representationNames: List<RepresentationName>
    get() {
      return representations.keys.sorted()
    }

  // It is a client's responsibility to set a correct (valid) value of the currentRepresentationName
  override var currentRepresentationName: RepresentationName = ""
    set(value) {
      if (field != value) {
        if (currentRepresentationIsActive.get()) {
          currentRepresentation?.onDeactivate()
        }
        field = value

        onRepresentationChanged()
        if (isActive.get()) {
          LOG.debug { "[$instanceId] Activating '$value'"}
          currentRepresentation?.onActivate()
          currentRepresentationIsActive.set(true)
        }
        else {
          // The preview is not active so mark the current representation as not-active
          currentRepresentationIsActive.set(false)
          LOG.debug { "[$instanceId] Did not activate '$value' since the MultiRepresentationPreview is not active."}
        }
      }
    }

  /**
   * [AtomicBoolean] to track activations.
   * Indicates whether the current preview is active. If false, the preview might be hidden or in the background.
   */
  private val isActive = AtomicBoolean(false)

  private val caretListener = object : CaretListener {
    /**
     * This tracks the last time the file was modified. This allows us to infer if the caret moved because the user
     * was typing or just moving around.
     */
    var lastEditorModificationStamp = -1L

    override fun caretPositionChanged(event: CaretEvent) {
      val newStamp = event.editor.document.modificationStamp

      val isModificationTriggered = if (newStamp != -1L && lastEditorModificationStamp != newStamp) {
        lastEditorModificationStamp = newStamp
        true
      }
      else false

      currentRepresentation?.onCaretPositionChanged(event, isModificationTriggered)
    }
  }

  private fun onRepresentationChanged() = UIUtil.invokeLaterIfNeeded {
    component.removeAll()

    component.add(representationSelectionToolbar, BorderLayout.NORTH)

    currentRepresentation?.let {
      component.add(it.component, BorderLayout.CENTER)
    }

    representationNeverShown = false
  }

  private fun validateCurrentRepresentationName() {
    if (representations.isEmpty()) {
      currentRepresentationName = ""
    }
    else if (!representations.containsKey(currentRepresentationName)) {
      currentRepresentationName = representationNames.first()
    }
    if (representationNeverShown) {
      onRepresentationChanged()
    }
  }

  protected fun updateRepresentations() = UIUtil.invokeLaterIfNeeded {
    if (Disposer.isDisposed(this)) {
      return@invokeLaterIfNeeded
    }

    val file = psiFilePointer.element
    if (file == null || !file.isValid) {
      return@invokeLaterIfNeeded
    }

    val providers = AndroidSlowOperations.allowSlowOperationsInIdea(ThrowableComputable {
      providers.filter { it.accept(project, file) }.toList()
    })
    val providerNames = providers.map { it.displayName }.toSet()

    // Remove unaccepted
    (representations.keys - providerNames).forEach { name ->
      representations.remove(name)?.let {
        Disposer.dispose(it)
      }
    }

    val hadAnyRepresentationsInitialized = representations.isNotEmpty()
    // Add new
    for (provider in providers.filter { it.displayName !in representations.keys }) {
      if (representations.containsKey(provider.displayName)) continue
      val representation = provider.createRepresentation(file)
      Disposer.register(this, representation)
      shortcutsApplicableComponent?.let {
        representation.registerShortcuts(it)
      }
      representations[provider.displayName] = representation

      // Restore the state of the representation
      stateFromDisk
        ?.representations
        ?.find { it.key == provider.displayName }
        ?.let {
          representation.setState(it.settings)
        }
    }

    if (!hadAnyRepresentationsInitialized) {
      // The first time we load one representation, we try to set it to the one we had saved on disk when saving the state.
      stateFromDisk?.selectedRepresentationName?.let { currentRepresentationName = it }
    }

    // update current if it was deleted
    validateCurrentRepresentationName()

    representationSelectionToolbar.isVisible = representations.size > 1

    onRepresentationsUpdated?.invoke()
  }

  var onRepresentationsUpdated: (() -> Unit)? = null

  fun updateNotifications() {
    representations.values.forEach {
      it.updateNotifications(this)
    }
  }

  fun registerShortcuts(appliedTo: JComponent) {
    shortcutsApplicableComponent = appliedTo
    representations.values.forEach { it.registerShortcuts(appliedTo) }
  }

  override fun setState(state: FileEditorState) {
    if (stateFromDisk != null) return
    if (state is MultiRepresentationPreviewFileEditorState) {
      stateFromDisk = state

      // For any already loaded representations, restore the state.
      representations.forEach { (representationName, representation) ->
        state.representations
          .find { it.key == representationName }
          ?.let { representation.setState(it.settings)  }
      }

      currentRepresentationName = state.selectedRepresentationName
      updateRepresentations()
    }
  }

  override fun getState(level: FileEditorStateLevel): MultiRepresentationPreviewFileEditorState {
    val representationStates = representations.mapNotNull { (name, representation) ->
      if (name.isEmpty()) return@mapNotNull null
      val settings = representation.getState() ?: return@mapNotNull null
      Representation(name, settings)
    }.toList()

    return MultiRepresentationPreviewFileEditorState(currentRepresentationName, representationStates)
  }

  /*
   * The code below is responsible for creating a toolbar for selecting current [PreviewRepresentation]. The toolbar appears only if there
   * are more than 1 [PreviewRepresentation] is available (accepted) for the current file.
   */
  private fun findAllPreviews() = FileEditorManager.getInstance(project).getAllEditors(file).asIterable()
    .filterIsInstance<TextEditorWithMultiRepresentationPreview<out MultiRepresentationPreview>>()
    .map { it.preview }
    .distinct()

  private fun createActionToolbar(group: ActionGroup): ActionToolbarImpl {
    val toolbar = ActionManager.getInstance().createActionToolbar("top", group, true)
    toolbar.layoutPolicy = ActionToolbar.WRAP_LAYOUT_POLICY
    if (group === ActionGroup.EMPTY_GROUP) {
      toolbar.component.isVisible = false
    }
    ActionToolbarUtil.makeToolbarNavigable(toolbar)
    return toolbar as ActionToolbarImpl
  }

  private class RepresentationOption(val representationName: String, val parent: MultiRepresentationPreview) :
    AnAction(representationName) {
    override fun actionPerformed(e: AnActionEvent) {
      // Here we iterate over all editors as change in selection (write) should trigger updates in all of them
      parent.findAllPreviews().forEach { it.currentRepresentationName = representationName }
    }
  }

  private class RepresentationsSelector(val parent: MultiRepresentationPreview) :
    DropDownAction(null, "Representations", StudioIcons.LayoutEditor.Palette.CUSTOM_VIEW) {
    override fun update(e: AnActionEvent) {
      super.update(e)
      removeAll()

      // We need just a single previewEditor here (any) to retrieve (read) the states and currently selected state
      parent.representations.keys.forEach {
        add(RepresentationOption(it, parent))
      }
      e.presentation.setText(parent.currentRepresentationName, false)
    }

    override fun displayTextInToolbar() = true
  }

  private fun createActionGroup(): ActionGroup {
    val actionGroup = DefaultActionGroup()
    val representationsSelector = RepresentationsSelector(this)
    actionGroup.add(representationsSelector)
    return actionGroup
  }

  private val representationSelectionToolbar: JComponent by lazy {
    AdtPrimaryPanel(BorderLayout()).apply {
      border = BorderFactory.createMatteBorder(0, 0, 1, 0, com.android.tools.adtui.common.border)

      add(createActionToolbar(createActionGroup()))
    }
  }

  /**
   * Method called before [onActivate] to initialize the representations. This method will only be called once while [onActivate] and
   * [onDeactivate] might be called multiple times.
   */
  fun onInit() {
    // If we initialize during non smart mode, it can be that the representations can not be calculated correctly just yet.
    // In that case, we will force a second refresh after we are in smart mode.
    if (DumbService.getInstance(project).isDumb) {
      DumbService.getInstance(project).runWhenSmart {
        updateRepresentations()
      }
    }
    updateRepresentations()
  }

  /**
   * Method called when this preview becomes active.
   */
  fun onActivate() {
    if (isActive.getAndSet(true)) return

    if (!currentRepresentationIsActive.getAndSet(true)) {
      LOG.debug { "[$instanceId] Activating '$currentRepresentationName'" }
      currentRepresentation?.onActivate()
    }

    caretListener.lastEditorModificationStamp = editor.document.modificationStamp
    editor.caretModel.addCaretListener(caretListener, this)
  }

  /**
   * Method called when this preview becomes deactivated. Updates will not be processed un the next [onActivate].
   */
  fun onDeactivate() {
    if (!isActive.getAndSet(false)) return
    currentRepresentationIsActive.set(false)
    LOG.debug { "[$instanceId] Deactivating '$currentRepresentationName'"}
    editor.caretModel.removeCaretListener(caretListener)
    currentRepresentation?.onDeactivate()
  }

  override fun dispose() {
    onDeactivate()
  }
}