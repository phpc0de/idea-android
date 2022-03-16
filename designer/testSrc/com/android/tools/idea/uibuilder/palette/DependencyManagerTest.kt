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
package com.android.tools.idea.uibuilder.palette

import com.android.SdkConstants.CARD_VIEW
import com.android.SdkConstants.FLOATING_ACTION_BUTTON
import com.android.SdkConstants.FN_GRADLE_PROPERTIES
import com.android.SdkConstants.RECYCLER_VIEW
import com.android.SdkConstants.TEXT_VIEW
import com.android.tools.idea.model.AndroidModuleInfo
import com.android.tools.idea.projectsystem.PLATFORM_SUPPORT_LIBS
import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager.SyncResult
import com.android.tools.idea.projectsystem.TestProjectSystem
import com.android.tools.idea.uibuilder.type.LayoutFileType
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.createTestOpenProjectOptions
import com.intellij.util.ui.UIUtil
import org.jetbrains.android.AndroidTestCase
import org.mockito.Mockito.mock
import java.io.File
import java.util.*
import java.util.concurrent.Future

class DependencyManagerTest : AndroidTestCase() {
  private var panel: PalettePanel? = null
  private var palette: Palette? = null
  private var disposable: Disposable? = null
  private var dependencyManager: DependencyManager? = null
  private var dependencyUpdateCount = 0
  private val syncListeners = ArrayDeque<Future<*>>()
  private val dialogMessages = mutableListOf<String>()

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    val testProjectSystem = TestProjectSystem(project, availableDependencies = PLATFORM_SUPPORT_LIBS)
    testProjectSystem.useInTests()
    panel = mock(PalettePanel::class.java)
    palette = NlPaletteModel.get(myFacet).getPalette(LayoutFileType)
    disposable = Disposer.newDisposable()
    Disposer.register(testRootDisposable, disposable!!)

    dependencyManager = DependencyManager(project)
    dependencyManager?.setSyncTopicListener { syncListeners.add(it) }
    if (getTestName(true) != "noNotificationOnProjectSyncBeforeSetPalette") {
      dependencyManager!!.setPalette(palette!!, myModule)
      waitAndDispatchAll()
    }
    Disposer.register(disposable!!, dependencyManager!!)
    dependencyManager!!.addDependencyChangeListener { dependencyUpdateCount++ }
    TestDialogManager.setTestDialog { message: String ->
      dialogMessages.add(message.trim()) // Remove line break in the end of the message.
      Messages.OK
    }
  }

  @Throws(Exception::class)
  override fun tearDown() {
    try {
      AndroidModuleInfo.setInstanceForTest(myFacet, null)
      TestDialogManager.setTestDialog(TestDialog.DEFAULT)
      // Null out all fields, since otherwise they're retained for the lifetime of the suite (which can be long if e.g. you're running many
      // tests through IJ)
      Disposer.dispose(disposable!!)
      palette = null
      panel = null
      dependencyManager = null
      disposable = null
    }
    finally {
      super.tearDown()
    }
  }

  fun testNeedsLibraryLoad() {
    assertThat(dependencyManager!!.needsLibraryLoad(findItem(TEXT_VIEW))).isFalse()
    assertThat(dependencyManager!!.needsLibraryLoad(findItem(FLOATING_ACTION_BUTTON.defaultName()))).isTrue()
  }

  fun testEnsureLibraryIsIncluded() {
    val (floatingActionButton, recyclerView, cardView) =
      listOf(FLOATING_ACTION_BUTTON.defaultName(), RECYCLER_VIEW.defaultName(), CARD_VIEW.defaultName()).map(this::findItem)

    assertThat(dependencyManager!!.needsLibraryLoad(floatingActionButton)).isTrue()
    assertThat(dependencyManager!!.needsLibraryLoad(recyclerView)).isTrue()
    assertThat(dependencyManager!!.needsLibraryLoad(cardView)).isTrue()

    dependencyManager!!.ensureLibraryIsIncluded(floatingActionButton)
    waitAndDispatchAll()
    dependencyManager!!.ensureLibraryIsIncluded(cardView)
    waitAndDispatchAll()

    assertThat(dependencyManager!!.needsLibraryLoad(floatingActionButton)).isFalse()
    assertThat(dependencyManager!!.needsLibraryLoad(recyclerView)).isTrue()
    assertThat(dependencyManager!!.needsLibraryLoad(cardView)).isFalse()
  }

  fun testRegisterDependencyUpdates() {
    simulateProjectSync()
    assertEquals(0, dependencyUpdateCount)

    dependencyManager!!.ensureLibraryIsIncluded(findItem(FLOATING_ACTION_BUTTON.defaultName()))
    waitAndDispatchAll()
    assertEquals(1, dependencyUpdateCount)
  }

  fun testDisposeStopsProjectSyncListening() {
    Disposer.dispose(disposable!!)

    dependencyManager!!.ensureLibraryIsIncluded(findItem(FLOATING_ACTION_BUTTON.defaultName()))
    waitAndDispatchAll()
    assertEquals(0, dependencyUpdateCount)
  }

  fun testAndroidxDependencies() {
    // The project has no dependencies and NELE_USE_ANDROIDX_DEFAULT is set to true
    assertTrue(dependencyManager!!.useAndroidXDependencies())

    val gradlePropertiesFile = ApplicationManager.getApplication().runWriteAction(Computable<VirtualFile> {
      val projectDir = VfsUtil.findFileByIoFile(File(project.basePath), true)!!
      projectDir.createChildData(null, FN_GRADLE_PROPERTIES)
    })

    val propertiesPsi = PsiManager.getInstance(project).findFile(gradlePropertiesFile)!!
    val propertiesDoc = PsiDocumentManager.getInstance(project).getDocument(propertiesPsi)!!

    // Check explicitly setting the variable
    ApplicationManager.getApplication().runWriteAction {
      propertiesDoc.setText("android.useAndroidX=false")
      PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
    simulateProjectSync()
    assertFalse(dependencyManager!!.useAndroidXDependencies())
    assertEquals(1, dependencyUpdateCount)

    ApplicationManager.getApplication().runWriteAction {
      propertiesDoc.setText("android.useAndroidX=true")
      PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
    simulateProjectSync()
    assertTrue(dependencyManager!!.useAndroidXDependencies())
    assertEquals(2, dependencyUpdateCount)
  }

  fun testNoNotificationOnProjectSyncBeforeSetPalette() {
    dependencyManager!!.setNotifyAlways(true)
    simulateProjectSync()
    assertEquals(0, dependencyUpdateCount)
  }

  fun testSetPaletteWithDisposedProject() {
    val foo = createTempDir("foo")
    val bar = createTempDir("bar")
    val tempProject = ProjectManagerEx.getInstanceEx().newProject(foo.toPath(), createTestOpenProjectOptions())!!
    val localDependencyManager: DependencyManager

    try {
      val tempModule = WriteCommandAction.runWriteCommandAction(tempProject, Computable {
        ModuleManager.getInstance(tempProject).newModule(bar.path, StdModuleTypes.JAVA.id)
      })

      localDependencyManager = DependencyManager(tempProject)
      Disposer.register(tempProject, localDependencyManager)
      localDependencyManager.setPalette(palette!!, tempModule)
    }
    finally {
      WriteCommandAction.runWriteCommandAction(tempProject) {
        Disposer.dispose(tempProject)
      }
    }
  }

  private fun simulateProjectSync() {
    project.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC).syncEnded(SyncResult.SUCCESS)
    waitAndDispatchAll()
  }

  private fun waitAndDispatchAll() {
    while (syncListeners.isNotEmpty()) {
      syncListeners.remove().get()
    }
    UIUtil.dispatchAllInvocationEvents()
  }

  private fun findItem(tagName: String): Palette.Item {
    val found = Ref<Palette.Item>()
    palette!!.accept { item ->
      if (item.tagName == tagName) {
        found.set(item)
      }
    }
    if (found.isNull) {
      throw RuntimeException("The item: $tagName was not found on the palette.")
    }
    return found.get()
  }
}
