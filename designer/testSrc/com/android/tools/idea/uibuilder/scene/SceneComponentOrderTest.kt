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
package com.android.tools.idea.uibuilder.scene

import com.android.SdkConstants
import com.android.tools.idea.common.api.InsertType
import com.android.tools.idea.common.fixtures.ModelBuilder
import com.intellij.psi.XmlElementFactory
import com.intellij.testFramework.PlatformTestUtil

/**
 * Basic tests for creating and updating a Scene out of a NlModel
 */
class SceneComponentOrderTest : SceneTest() {

  fun testInsertComponentToHead() {
    val constraintLayout = myModel.find("root")!!
    val textView = myModel.find("textView")!!

    val editTextTag = XmlElementFactory.getInstance(project).createTagFromText("<" + SdkConstants.EDIT_TEXT + "/>")
    val editText = myModel.createComponent(myModel.surface, editTextTag, null, null, InsertType.CREATE)
    myModel.addComponents(listOf(editText), constraintLayout, textView, InsertType.CREATE, myModel.surface)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals(editText, root.children[0].nlComponent)
    assertEquals("textView", root.children[1].id)
    assertEquals("button", root.children[2].id)
  }

  fun testInsertComponentToMiddle() {
    val constraintLayout = myModel.find("root")!!
    val button = myModel.find("button")!!

    val editTextTag = XmlElementFactory.getInstance(project).createTagFromText("<" + SdkConstants.EDIT_TEXT + "/>")
    val editText = myModel.createComponent(myModel.surface, editTextTag, null, null, InsertType.CREATE)
    myModel.addComponents(listOf(editText), constraintLayout, button, InsertType.CREATE, myModel.surface)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals("textView", root.children[0].id)
    assertEquals(editText, root.children[1].nlComponent)
    assertEquals("button", root.children[2].id)
  }

  fun testAppendComponent() {
    val constraintLayout = myModel.find("root")!!

    val editTextTag = XmlElementFactory.getInstance(project).createTagFromText("<" + SdkConstants.EDIT_TEXT + "/>")
    val editText = myModel.createComponent(myModel.surface, editTextTag, null, null, InsertType.CREATE)
    myModel.addComponents(listOf(editText), constraintLayout, null, InsertType.CREATE, myModel.surface)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals("textView", root.children[0].id)
    assertEquals("button", root.children[1].id)
    assertEquals(editText, root.children[2].nlComponent)
  }

  fun testMoveItemDown() {
    val constraintLayout = myModel.find("root")!!
    val textView = myModel.find("textView")!!
    val button = myModel.find("button")!!

    myModel.addComponents(listOf(textView), constraintLayout, null, InsertType.MOVE, myModel.surface)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals(button, root.children[0].nlComponent)
    assertEquals(textView, root.children[1].nlComponent)
  }

  fun testMoveItemUp() {
    val constraintLayout = myModel.find("root")!!
    val textView = myModel.find("textView")!!
    val button = myModel.find("button")!!

    myModel.addComponents(listOf(button), constraintLayout, textView, InsertType.MOVE, myModel.surface)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals(button, root.children[0].nlComponent)
    assertEquals(textView, root.children[1].nlComponent)
  }

  fun testRemoveComponent() {
    val constraintLayout = myModel.find("root")!!
    val textView = myModel.find("textView")!!
    val button = myModel.find("button")!!

    constraintLayout.removeChild(textView)

    mySceneManager.update()

    val root = myScene.root!!
    assertEquals(button, root.children[0].nlComponent)
    assertEquals(1, root.childCount)
  }

  override fun createModel(): ModelBuilder {
    return model("constraint.xml",
                 component(SdkConstants.CONSTRAINT_LAYOUT.defaultName())
                   .id("@+id/root")
                   .withBounds(0, 0, 2000, 2000)
                   .width("1000dp")
                   .height("1000dp")
                   .children(
                     component(SdkConstants.TEXT_VIEW)
                       .id("@+id/textView")
                       .withBounds(200, 400, 200, 40)
                       .width("100dp")
                       .height("20dp")
                       .withAttribute("tools:layout_editor_absoluteX", "100dp")
                       .withAttribute("tools:layout_editor_absoluteY", "200dp"),
                     component(SdkConstants.BUTTON)
                       .id("@+id/button")
                       .withBounds(400, 800, 200, 40)
                       .width("100dp")
                       .height("20dp")
                       .withAttribute("tools:layout_editor_absoluteX", "200dp")
                       .withAttribute("tools:layout_editor_absoluteY", "400dp")
                   )
    )
  }
}
