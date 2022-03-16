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
package com.android.tools.idea.common.surface

import com.android.SdkConstants.CONSTRAINT_LAYOUT
import com.android.SdkConstants.RELATIVE_LAYOUT
import com.android.tools.idea.common.SyncNlModel
import com.android.tools.idea.common.editor.ActionManager
import com.android.tools.idea.common.model.DnDTransferItem
import com.android.tools.idea.common.model.ItemTransferable
import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.common.model.NlModel
import com.android.tools.idea.uibuilder.LayoutTestCase
import com.android.tools.idea.uibuilder.scene.SyncLayoutlibSceneManager
import com.android.tools.idea.uibuilder.surface.layout.PositionableContent
import com.android.tools.idea.uibuilder.surface.layout.PositionableContentLayoutManager
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.ComponentEvent
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import javax.swing.JComponent

class DesignSurfaceTest : LayoutTestCase() {

  fun testAddAndRemoveModel() {
    val model1 = model("model1.xml", component(RELATIVE_LAYOUT)).build()
    val model2 = model("model2.xml", component(CONSTRAINT_LAYOUT.oldName())).build()
    val surface = TestDesignSurface(myModule.project, myModule.project)

    assertEquals(0, surface.models.size)

    surface.addModelWithoutRender(model1)
    assertEquals(1, surface.models.size)

    surface.addModelWithoutRender(model2)
    assertEquals(2, surface.models.size)

    surface.removeModel(model2)
    surface.zoomToFit()
    assertEquals(1, surface.models.size)

    surface.removeModel(model1)
    surface.zoomToFit()
    assertEquals(0, surface.models.size)
  }

  fun testAddDuplicatedModel() {
    val model = model("model.xml", component(RELATIVE_LAYOUT)).build()
    val surface = TestDesignSurface(myModule.project, myModule.project)

    assertEquals(0, surface.models.size)

    surface.addModelWithoutRender(model)
    assertEquals(1, surface.models.size)

    surface.addModelWithoutRender(model)
    // should not add model again and the callback should not be triggered.
    assertEquals(1, surface.models.size)
  }

  fun testRemoveIllegalModel() {
    val model1 = model("model1.xml", component(RELATIVE_LAYOUT)).build()
    val model2 = model("model2.xml", component(RELATIVE_LAYOUT)).build()
    val surface = TestDesignSurface(myModule.project, myModule.project)

    assertEquals(0, surface.models.size)

    surface.removeModel(model1)
    surface.zoomToFit()
    // do nothing and the callback should not be triggered.
    assertEquals(0, surface.models.size)

    surface.addModelWithoutRender(model1)
    assertEquals(1, surface.models.size)

    surface.removeModel(model2)
    surface.zoomToFit()
    assertEquals(1, surface.models.size)
  }

  fun testScale() {
    val surface = TestDesignSurface(myModule.project, myModule.project)
    surface.setScale(0.66, -1, -1)
    assertFalse(surface.setScale(0.663, -1, -1))
    assertFalse(surface.setScale(0.664, -1, -1))
    assertTrue(surface.setScale(0.665, -1, -1))

    surface.setScale(0.33, -1, -1)
    assertFalse(surface.setScale(0.332, -1, -1))
    assertTrue(surface.setScale(0.335, -1, -1))
  }

  fun testResizeSurfaceRebuildScene() {
    val builder = model("relative.xml",
                        component(RELATIVE_LAYOUT)
                          .withBounds(0, 0, 1000, 1000)
                          .matchParentWidth()
                          .matchParentHeight())
    val model1 = builder.build()
    val model2 = builder.build()

    val surface = TestDesignSurface(project, testRootDisposable)
    surface.addModelWithoutRender(model1)
    surface.addModelWithoutRender(model2)

    val scene1 = surface.getSceneManager(model1)!!.scene
    val scene2 = surface.getSceneManager(model2)!!.scene
    val oldVersion1 = scene1.displayListVersion
    val oldVersion2 = scene2.displayListVersion

    surface.dispatchEvent(ComponentEvent(surface, ComponentEvent.COMPONENT_RESIZED))

    assertFalse(scene1.displayListVersion == oldVersion1)
    assertFalse(scene2.displayListVersion == oldVersion2)
  }

  fun testResizeSurfaceDoesNotChangeScale() {
    // This also checks if the zoom level is same after resizing, because the screen factor of TestDesignSurface is always 1.
    val builder = model("relative.xml",
                        component(RELATIVE_LAYOUT)
                          .withBounds(0, 0, 1000, 1000)
                          .matchParentWidth()
                          .matchParentHeight())
    val model1 = builder.build()
    val model2 = builder.build()

    val surface = TestDesignSurface(project, testRootDisposable)
    surface.addModelWithoutRender(model1)
    surface.addModelWithoutRender(model2)

    surface.setSize(1000, 1000)
    surface.dispatchEvent(ComponentEvent(surface, ComponentEvent.COMPONENT_RESIZED))
    val oldScale = surface.scale

    surface.setSize(500, 500)
    surface.dispatchEvent(ComponentEvent(surface, ComponentEvent.COMPONENT_RESIZED))

    assertTrue(oldScale == surface.scale)
  }

  fun testDesignSurfaceModelOrdering() {
    val builder = model("relative.xml",
                        component(RELATIVE_LAYOUT)
                          .withBounds(0, 0, 1000, 1000)
                          .matchParentWidth()
                          .matchParentHeight())
    val model1 = builder.build()
    val model2 = builder.build()
    val model3 = builder.build()


    val surface = TestDesignSurface(project, testRootDisposable)
    surface.addModelWithoutRender(model1)
    surface.addModelWithoutRender(model2)
    surface.addModelWithoutRender(model3)

    assertThat(surface.models).containsExactly(model1, model2, model3).inOrder()
    surface.addModelWithoutRender(model3)
    assertThat(surface.models).containsExactly(model1, model2, model3).inOrder()
    surface.addModelWithoutRender(model1)
    assertThat(surface.models).containsExactly(model2, model3, model1).inOrder()
    surface.addModelWithoutRender(model3)
    assertThat(surface.models).containsExactly(model2, model1, model3).inOrder()
  }
}

class TestActionManager(surface: DesignSurface) : ActionManager<DesignSurface>(surface) {
  override fun registerActionsShortcuts(component: JComponent) = Unit

  override fun getPopupMenuActions(leafComponent: NlComponent?) = DefaultActionGroup()

  override fun getToolbarActions(selection: MutableList<NlComponent>) = DefaultActionGroup()
}

class TestInteractionHandler(surface: DesignSurface) : InteractionHandlerBase(surface) {
  override fun createInteractionOnPressed(mouseX: Int, mouseY: Int, modifiersEx: Int): Interaction? = null

  override fun createInteractionOnDrag(mouseX: Int, mouseY: Int, modifiersEx: Int): Interaction? = null
}

class TestLayoutManager(private val surface: DesignSurface) : PositionableContentLayoutManager() {
  override fun layoutContainer(content: Collection<PositionableContent>, availableSize: Dimension) {}

  override fun preferredLayoutSize(content: Collection<PositionableContent>, availableSize: Dimension): Dimension =
    surface.sceneViews.map { it.getContentSize(null) }.firstOrNull() ?: Dimension(0, 0)
}

class TestActionHandler(surface: DesignSurface) : DesignSurfaceActionHandler(surface) {
  override fun getPasteTarget(): NlComponent? = null
  override fun canHandleChildren(component: NlComponent, pasted: MutableList<NlComponent>): Boolean = false
  override fun getFlavor(): DataFlavor = ItemTransferable.DESIGNER_FLAVOR
  override fun canDeleteElement(dataContext: DataContext): Boolean = false
  override fun isPasteEnabled(dataContext: DataContext): Boolean = false
  override fun isCopyEnabled(dataContext: DataContext): Boolean = false
  override fun isCopyVisible(dataContext: DataContext): Boolean = false
  override fun isCutVisible(dataContext: DataContext): Boolean = false
  override fun isPastePossible(dataContext: DataContext): Boolean = false
}

class TestDesignSurface(project: Project, disposible: Disposable)
  : DesignSurface(project,
                  disposible,
                  java.util.function.Function { TestActionManager(it) },
                  java.util.function.Function { TestInteractionHandler(it) },
                  true,
                  java.util.function.Function { TestLayoutManager(it) },
                  java.util.function.Function { TestActionHandler(it) },
                  ZoomControlsPolicy.VISIBLE) {
  override fun getSelectionAsTransferable(): ItemTransferable {
    return ItemTransferable(DnDTransferItem(0, ImmutableList.of()))
  }

  override fun getComponentRegistrar() = Consumer<NlComponent> {}

  override fun createSceneManager(model: NlModel) = SyncLayoutlibSceneManager(model as SyncNlModel)

  override fun scrollToCenter(list: MutableList<NlComponent>) {}

  override fun canZoomToFit() = true

  override fun getDefaultOffset() = Dimension()

  override fun getPreferredContentSize(availableWidth: Int, availableHeight: Int) = Dimension()

  override fun isLayoutDisabled() = true

  override fun forceUserRequestedRefresh(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

  override fun getSelectableComponents(): List<NlComponent> = emptyList()
}
