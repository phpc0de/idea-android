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
package com.android.tools.idea.gradle.structure.dependencies

import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.model.PsModule
import com.android.tools.idea.gradle.structure.model.meta.ParsedValue
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.util.text.nullize
import java.net.UnknownHostException
import javax.swing.JComponent

const val ADD_LIBRARY_DEPENDENCY_DIALOG_TITLE = "Add Library Dependency"

class AddLibraryDependencyDialog(private val context: PsContext, module: PsModule) : AbstractAddDependenciesDialog(module) {

  private var libraryDependenciesForm: LibraryDependenciesForm? = null

  init {
    title = ADD_LIBRARY_DEPENDENCY_DIALOG_TITLE
    init()
  }

  override fun postponeValidation(): Boolean = true

  override fun addNewDependencies() {
    val library = libraryDependenciesForm!!.selectedLibrary
    val scopeName = scopesPanel.selectedScopeName

    when (library) {
      is ParsedValue.Set.Parsed -> module.addLibraryDependency(library, scopeName)
      else -> throw IllegalStateException()
    }
  }

  override fun getSplitterProportionKey(): String = "psd.add.library.dependency.main.horizontal.splitter.proportion"

  override fun getDependencySelectionView(): JComponent {
    if (libraryDependenciesForm == null) {
      libraryDependenciesForm = LibraryDependenciesForm(context, module)
    }
    return libraryDependenciesForm!!.panel
  }

  override fun getInstructions(): String =
    "Use the form below to find the library to add. This form uses the repositories specified in the project's build files " +
    "(${libraryDependenciesForm?.repositories?.joinToString(", ") { it.name }})"

  override fun getDimensionServiceKey(): String = "psd.add.library.dependency.panel.dimension"

  override fun getPreferredFocusedComponent(): JComponent? =
    if (libraryDependenciesForm != null) libraryDependenciesForm!!.preferredFocusedComponent else null

  override fun dispose() {
    super.dispose()
    if (libraryDependenciesForm != null) {
      Disposer.dispose(libraryDependenciesForm!!)
    }
  }

  override fun doValidate(): ValidationInfo? {
    val selectedLibrary = libraryDependenciesForm!!.selectedLibrary
    // Do not validate search query if a specifi library has been already selected.
    if (selectedLibrary != ParsedValue.NotSet) return scopesPanel.validateInput()

    val searchErrors = libraryDependenciesForm!!.searchErrors
    if (!searchErrors.isEmpty()) {
      return ValidationInfo(buildString {
        searchErrors.forEach {
          append(getErrorMessage(it))
          append("\n")
        }
      }, libraryDependenciesForm!!.preferredFocusedComponent)
    }

    return ValidationInfo("Please specify the library to add as dependency", libraryDependenciesForm!!.preferredFocusedComponent)
  }

  override fun createDependencyScopesPanel(module: PsModule): AbstractDependencyScopesPanel =
    DependencyScopePanel(module, PsModule.ImportantFor.LIBRARY)
}

private fun getErrorMessage(error: Exception): String =
  when (error) {
    is UnknownHostException -> "Failed to connect to host '" + error.message + "'. Please check your Internet connection."
    else -> error.message.nullize() ?: error.javaClass.name
  }
