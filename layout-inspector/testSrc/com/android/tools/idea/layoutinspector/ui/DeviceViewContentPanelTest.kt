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
package com.android.tools.idea.layoutinspector.ui

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.resources.ResourceType
import com.android.testutils.ImageDiffUtil
import com.android.testutils.MockitoKt.mock
import com.android.testutils.TestUtils.resolveWorkspacePath
import com.android.tools.adtui.actions.DropDownAction
import com.android.tools.adtui.imagediff.ImageDiffTestUtil
import com.android.tools.adtui.swing.FakeMouse
import com.android.tools.adtui.swing.FakeUi
import com.android.tools.adtui.swing.setPortableUiFont
import com.android.tools.adtui.workbench.PropertiesComponentMock
import com.android.tools.idea.appinspection.ide.ui.SelectProcessAction
import com.android.tools.idea.layoutinspector.LAYOUT_INSPECTOR_DATA_KEY
import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.common.SelectViewAction
import com.android.tools.idea.layoutinspector.metrics.statistics.SessionStatistics
import com.android.tools.idea.layoutinspector.model
import com.android.tools.idea.layoutinspector.model.ROOT
import com.android.tools.idea.layoutinspector.model.SelectionOrigin
import com.android.tools.idea.layoutinspector.model.VIEW1
import com.android.tools.idea.layoutinspector.model.VIEW2
import com.android.tools.idea.layoutinspector.model.VIEW3
import com.android.tools.idea.layoutinspector.model.VIEW4
import com.android.tools.idea.layoutinspector.model.WINDOW_MANAGER_FLAG_DIM_BEHIND
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient
import com.android.tools.idea.layoutinspector.pipeline.InspectorClientLauncher
import com.android.tools.idea.layoutinspector.util.FakeTreeSettings
import com.android.tools.idea.layoutinspector.window
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.impl.HeadlessDataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPopupMenu
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.replaceService
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import java.awt.Color
import java.awt.Cursor
import java.awt.Cursor.HAND_CURSOR
import java.awt.Dimension
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Polygon
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPopupMenu

private const val TEST_DATA_PATH = "tools/adt/idea/layout-inspector/testData"
private const val DIFF_THRESHOLD = 0.02
private val activityMain = ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.LAYOUT, "activity_main")

class DeviceViewContentPanelTest {
  private val projectRule = ProjectRule()

  @get:Rule
  val disposable = DisposableRule()

  @get:Rule
  val chain = RuleChain.outerRule(projectRule).around(DeviceViewSettingsRule()).around(EdtRule())!!

  @Before
  fun before() {
    ApplicationManager.getApplication().replaceService(PropertiesComponent::class.java, PropertiesComponentMock(), disposable.disposable)
  }

  @Test
  fun testSize() {
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 0, 0, 50, 50) {
          view(VIEW3, 30, 30, 10, 10)
        }
        view(VIEW2, 60, 160, 10, 20)
      }
    }
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val settings = DeviceViewSettings(scalePercent = 30)
    settings.drawLabel = false
    val stats = SessionStatistics(model, treeSettings)
    val panel = DeviceViewContentPanel(model, stats, treeSettings, settings, { mock() }, disposable.disposable)
    assertEquals(Dimension(376, 394), panel.preferredSize)

    settings.scalePercent = 100
    assertEquals(Dimension(1020, 1084), panel.preferredSize)

    model.update(
      window(ROOT, ROOT, 0, 0, 100, 200) {
        view(VIEW1, 0, 0, 50, 50)
      }, listOf(ROOT), 0)
    // This is usually handled by a listener registered in DeviceViewPanel
    panel.model.refresh()
    assertEquals(Dimension(732, 820), panel.preferredSize)
  }

  @Test
  fun testPaint() {
    val model = model {
      view(ROOT, 0, 0, 500, 1000) {
        view(VIEW1, 125, 150, 250, 250) {
          image()
        }
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(1000, 1500, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(1000, 1500)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint.png"), generatedImage, DIFF_THRESHOLD)

    settings.scalePercent = 50
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_scaled.png"), generatedImage, DIFF_THRESHOLD)

    settings.scalePercent = 100
    panel.model.rotate(0.3, 0.2)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_rotated.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.layerSpacing = 10
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_spacing1.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.layerSpacing = 200
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_spacing2.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.layerSpacing = INITIAL_LAYER_SPACING
    val windowRoot = model[ROOT]!!
    model.setSelection(windowRoot, SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_selected.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawLabel = true
    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    // Set the font so it will be the same across platforms
    graphics.font = Font("Droid Sans", Font.PLAIN, 12)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_label.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawBorders = false
    graphics = generatedImage.createGraphics()
    graphics.font = Font("Droid Sans", Font.PLAIN, 12)
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_noborders.png"), generatedImage, DIFF_THRESHOLD)

    model.hoveredNode = windowRoot
    graphics = generatedImage.createGraphics()
    graphics.font = Font("Droid Sans", Font.PLAIN, 12)
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_hovered.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  fun testPaintWithHiddenSystemViews() {
    val model = model {
      view(ROOT, 0, 0, 500, 1000, layout = null) {
        view(VIEW1, 125, 150, 250, 250, layout = activityMain) {
          image()
        }
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(1000, 1500, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(1000, 1500)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithHiddenSystemViews.png"),
                                     generatedImage, DIFF_THRESHOLD)

    settings.scalePercent = 100
    panel.model.rotate(0.3, 0.2)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithHiddenSystemView_rotated.png"),
                                     generatedImage, DIFF_THRESHOLD)

    panel.model.layerSpacing = INITIAL_LAYER_SPACING
    val windowRoot = model[ROOT]!!
    model.setSelection(windowRoot, SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithHiddenSystemView_selected.png"),
                                     generatedImage, DIFF_THRESHOLD)
  }

  @Test
  fun testRotationDoesntThrow() {
    val model = model {
      view(ROOT, 0, 0, 500, 1000) {
        // Use a RTL name to force TextLayout to be used
        view(VIEW1, 125, 150, 250, 250, qualifiedName = "שמי העברי") {
          image()
        }
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(10, 15, TYPE_INT_ARGB)
    val graphics = generatedImage.createGraphics()

    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, DeviceViewSettings(), { mock() },
                                       disposable.disposable)
    panel.setSize(10, 15)
    panel.model.rotate(-1.0, -1.0)

    for (i in 0..20) {
      panel.model.rotate(-2.0, 0.1)
      for (j in 0..20) {
        panel.model.rotate(0.1, 0.0)
        panel.paint(graphics)
      }
    }
  }

  @Test
  fun testOverlay() {
    val model = model {
      view(ROOT, 0, 0, 600, 600) {
        view(VIEW1, 125, 150, 250, 250)
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(1000, 1500, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(1000, 1500)

    panel.model.overlay = ImageIO.read(resolveWorkspacePath("$TEST_DATA_PATH/overlay.png").toFile())

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_overlay-60.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.overlayAlpha = 0.2f
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_overlay-20.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.overlayAlpha = 0.9f
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaint_overlay-90.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  fun testDrag() {
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 25, 30, 50, 50)
      }
    }

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val client: InspectorClient = mock()
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { client },
                                       disposable.disposable)
    `when`(client.capabilities).thenReturn(setOf(InspectorClient.Capability.SUPPORTS_SKP))
    val layoutInspector: LayoutInspector = mock()
    `when`(layoutInspector.currentClient).thenReturn(client)
    (DataManager.getInstance() as HeadlessDataManager).setTestDataProvider { id ->
      when (id) {
        LAYOUT_INSPECTOR_DATA_KEY.name -> layoutInspector
        TOGGLE_3D_ACTION_BUTTON_KEY.name -> mock<ActionButton>()
        else -> null
      }
    }
    panel.setSize(200, 300)
    val fakeUi = FakeUi(panel)

    fakeUi.mouse.drag(10, 10, 50, 10)
    // We're not in rotated mode, so nothing should have happened yet.
    assertEquals(0.0, panel.model.xOff)
    assertEquals(0.0, panel.model.yOff)

    // Now modify the model to be rotated and verify that dragging changes the rotation
    panel.model.xOff = 0.1
    fakeUi.mouse.drag(10, 10, 10, 10)
    assertEquals(0.11, panel.model.xOff)
    assertEquals(0.01, panel.model.yOff)

    panel.model.resetRotation()
    assertEquals(0.0, panel.model.xOff)
    assertEquals(0.0, panel.model.yOff)
  }

  @Test
  fun testClick() {
    val layoutMain = ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.LAYOUT, "activity_main")
    val layoutAppcompat = ResourceReference(ResourceNamespace.APPCOMPAT, ResourceType.LAYOUT, "abc_screen_simple")
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 25, 30, 50, 50, layout = layoutMain) {
          view(VIEW2, 30, 35, 40, 40, layout = layoutAppcompat) {
            view(VIEW3, 35, 40, 30, 30, layout = layoutAppcompat)
          }
        }
      }
    }
    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(100, 200)
    val fakeUi = FakeUi(panel)
    assertThat(model.selection).isNull()

    // Click on VIEW3 when system views are showing:
    treeSettings.hideSystemNodes = false
    fakeUi.mouse.click(40, 50)
    assertThat(model.selection).isSameAs(model[VIEW3]!!)

    // Click on VIEW3 when system views are hidden:
    treeSettings.hideSystemNodes = true
    fakeUi.mouse.click(40, 50)
    assertThat(model.selection).isSameAs(model[VIEW1]!!)
  }

  @Test
  fun testMouseMove() {
    val layoutMain = ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.LAYOUT, "activity_main")
    val layoutAppcompat = ResourceReference(ResourceNamespace.APPCOMPAT, ResourceType.LAYOUT, "abc_screen_simple")
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 25, 30, 50, 50, layout = layoutMain) {
          view(VIEW2, 30, 35, 40, 40, layout = layoutAppcompat) {
            view(VIEW3, 35, 40, 30, 30, layout = layoutAppcompat)
          }
        }
      }
    }
    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(100, 200)
    val fakeUi = FakeUi(panel)
    assertThat(model.hoveredNode).isNull()

    // Move to VIEW3 when system views are showing:
    fakeUi.mouse.moveTo(40, 50)
    assertThat(model.hoveredNode).isSameAs(model[VIEW3]!!)

    // Move to VIEW3 when system views are hidden:
    treeSettings.hideSystemNodes = true
    fakeUi.mouse.moveTo(40, 50)
    assertThat(model.hoveredNode).isSameAs(model[VIEW1]!!)
  }

  @Test
  fun testContextMenu() {
    val layoutMain = ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.LAYOUT, "activity_main")
    val layoutAppcompat = ResourceReference(ResourceNamespace.APPCOMPAT, ResourceType.LAYOUT, "abc_screen_simple")
    val model = model {
      view(ROOT, 0, 0, 100, 200, layout = null) {
        view(VIEW1, 25, 30, 50, 50, layout = layoutMain) {
          view(VIEW2, 30, 35, 40, 40, layout = layoutAppcompat) {
            view(VIEW3, 35, 40, 30, 30, layout = layoutAppcompat) {
              view(VIEW4, 36, 41, 25, 25, layout = layoutMain)
            }
          }
        }
      }
    }
    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(100, 200)
    val fakeUi = FakeUi(panel)
    assertThat(model.selection).isNull()

    var latestPopup: FakeActionPopupMenu? = null
    ApplicationManager.getApplication().replaceService(ActionManager::class.java, mock(), disposable.disposable)
    doAnswer { invocation ->
      latestPopup = FakeActionPopupMenu(invocation.getArgument(1))
      latestPopup
    }.`when`(ActionManager.getInstance()).createActionPopupMenu(anyString(), any(ActionGroup::class.java))

    // Right click on VIEW4 when system views are showing:
    fakeUi.mouse.click(40, 50, FakeMouse.Button.RIGHT)
    latestPopup!!.assertSelectViewAction(5, 4, 3, 2, 1)

    // Right click on VIEW4 when system views are hidden:
    treeSettings.hideSystemNodes = true
    fakeUi.mouse.click(40, 50, FakeMouse.Button.RIGHT)
    latestPopup!!.assertSelectViewAction(5, 2)
  }

  @Test
  fun testEmptyTextVisibility() {
    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val model = model {}
    val launcher: InspectorClientLauncher = mock()
    val client = mock<InspectorClient>()
    `when`(launcher.activeClient).thenReturn(client)
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { client },
                                       disposable.disposable)
    val selectProcessAction = mock<SelectProcessAction>()
    panel.selectProcessAction = selectProcessAction
    `when`(selectProcessAction.templatePresentation).thenReturn(mock())
    panel.setSize(200, 200)
    val fakeUi = FakeUi(panel)
    val hand = Cursor.getPredefinedCursor(HAND_CURSOR)
    mockStatic(BrowserUtil::class.java).use { browserUtil ->
      for (x in 0..200) {
        for (y in 0..200) {
          fakeUi.mouse.moveTo(x, y)
          if (panel.cursor == hand) {
            fakeUi.mouse.click(x, y)
          }
        }
      }
      browserUtil.verify(atLeastOnce()) { BrowserUtil.browse("https://developer.android.com/studio/debug/layout-inspector") }
      verify(selectProcessAction, atLeastOnce()).actionPerformed(any())
    }

    model.update(window(ROOT, ROOT) { view(VIEW1) }, listOf(ROOT), 1)
    for (x in 0..200) {
      for (y in 0..200) {
        fakeUi.mouse.moveTo(x, y)
        assertThat(panel.cursor).isNotEqualTo(hand)
      }
    }
  }

  @Test
  fun testPaintMultiWindow() {
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 0, 0, 50, 50) {
          image()
        }
      }
    }

    // Second window. Root doesn't overlap with top of first window--verify they're on separate levels in the drawing.
    val window2 = window(VIEW2, VIEW2, 60, 60, 30, 30) {
      view(VIEW3, 70, 70, 10, 10)
    }

    model.update(window2, listOf(ROOT, VIEW2), 0)

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(200, 300, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(200, 300)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintMultiWindow.png"), generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[VIEW3], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintMultiWindow_selected.png"), generatedImage,
                                     DIFF_THRESHOLD)

    panel.model.rotate(0.3, 0.2)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintMultiWindow_rotated.png"), generatedImage,
                                     DIFF_THRESHOLD)
  }

  @Test
  fun testPaintMultiWindowDimBehind() {
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 0, 0, 50, 50) {
          image()
        }
      }
    }

    // Second window. Root doesn't overlap with top of first window--verify they're on separate levels in the drawing.
    val window2 = window(VIEW2, VIEW2, 60, 60, 30, 30, layoutFlags = WINDOW_MANAGER_FLAG_DIM_BEHIND) {
      view(VIEW3, 70, 70, 10, 10)
    }

    model.update(window2, listOf(ROOT, VIEW2), 0)

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(200, 300, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 100)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(200, 300)
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintMultiWindowDimBehind.png"), generatedImage, DIFF_THRESHOLD)

    panel.model.rotate(0.3, 0.2)
    settings.scalePercent = 50
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintMultiWindowDimBehind_rotated.png"), generatedImage,
                                     DIFF_THRESHOLD)
  }

  @Test
  fun testPaintWithImages() {
    val image1 = ImageIO.read(resolveWorkspacePath("$TEST_DATA_PATH/image1.png").toFile())
    val image2 = ImageIO.read(resolveWorkspacePath("$TEST_DATA_PATH/image2.png").toFile())
    val image3 = ImageIO.read(resolveWorkspacePath("$TEST_DATA_PATH/image3.png").toFile())

    val model = model {
      view(ROOT, 0, 0, 585, 804) {
        image(image1)
        view(VIEW1, 0, 100, 585, 585) {
          image(image2)
        }
        view(VIEW2, 100, 400, 293, 402) {
          image(image3)
        }
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(350, 450, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 50)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(350, 450)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages.png"), generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[ROOT], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_root.png"), generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_view1.png"), generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[VIEW2], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_view2.png"), generatedImage, DIFF_THRESHOLD)

    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_view2.png"), generatedImage, DIFF_THRESHOLD)

    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_view2.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawLabel = true
    graphics = generatedImage.createGraphics()
    // Set the font so it will be the same across platforms
    graphics.font = Font("Droid Sans", Font.PLAIN, 12)
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImages_label.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Suppress("UndesirableClassUsage")
  @Test
  fun testPaintWithImagesBetweenChildren() {
    val image1 = BufferedImage(100, 100, TYPE_INT_ARGB)
    image1.graphics.run {
      color = Color.RED
      fillRect(25, 25, 50, 50)
    }
    val image2 = BufferedImage(100, 100, TYPE_INT_ARGB)
    image2.graphics.run {
      color = Color.BLUE
      fillRect(0, 0, 60, 60)
    }
    val image3 = BufferedImage(50, 50, TYPE_INT_ARGB)
    image3.graphics.run {
      color = Color.GREEN
      fillRect(0, 0, 50, 50)
    }

    val model = model {
      view(ROOT, 0, 0, 100, 100) {
        view(VIEW1, 0, 0, 100, 100) {
          image(image2)
        }
        image(image1)
        view(VIEW2, 50, 50, 50, 50) {
          image(image3)
        }
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(1200, 1400, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val stats = SessionStatistics(model, treeSettings)
    val panel = DeviceViewContentPanel(model, stats, treeSettings, DeviceViewSettings(scalePercent = 400), { mock() },
                                       disposable.disposable)
    panel.setSize(1200, 1400)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImagesBetweenChildren.png"), generatedImage,
                                     DIFF_THRESHOLD)

    panel.model.rotate(0.3, 0.2)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImagesBetweenChildren_rotated.png"),
                                     generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[ROOT], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    graphics.font = Font("Droid Sans", Font.PLAIN, 12)
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithImagesBetweenChildren_root.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  fun testPaintWithRootImageOnly() {
    val image1 = ImageIO.read(resolveWorkspacePath("$TEST_DATA_PATH/image1.png").toFile())

    val model = model {
      view(ROOT, 0, 0, 585, 804) {
        image(image1)
        view(VIEW1, 0, 100, 585, 585)
        view(VIEW2, 100, 400, 293, 402)
      }
    }

    @Suppress("UndesirableClassUsage")
    val generatedImage = BufferedImage(350, 450, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()

    val settings = DeviceViewSettings(scalePercent = 50)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(350, 450)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithRootImageOnly.png"), generatedImage,
                                     DIFF_THRESHOLD)

    model.setSelection(model[ROOT], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithRootImageOnly_root.png"), generatedImage, DIFF_THRESHOLD)

    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintWithRootImageOnly_view1.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  @Suppress("UndesirableClassUsage")
  fun testPaintTransformed() {
    val image1 = BufferedImage(220, 220, TYPE_INT_ARGB)
    (image1.graphics as Graphics2D).run {
      paint = GradientPaint(0f, 40f, Color.RED, 220f, 180f, Color.BLUE)
      fill(Polygon(intArrayOf(0, 180, 220, 40), intArrayOf(40, 0, 180, 220), 4))
    }

    val model = model {
      view(ROOT, 0, 0, 400, 600) {
        view(VIEW1, 50, 100, 300, 300, bounds = Polygon(intArrayOf(90, 270, 310, 130), intArrayOf(180, 140, 320, 360), 4)) {
          image(image1)
        }
      }
    }

    val generatedImage = BufferedImage(400, 600, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()

    val settings = DeviceViewSettings(scalePercent = 50)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(400, 600)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformed.png"), generatedImage,
                                     DIFF_THRESHOLD)

    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformed_view1.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawLabel = true
    graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformed_label.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawUntransformedBounds = true
    graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformed_untransformed.png"), generatedImage, DIFF_THRESHOLD)

    settings.drawBorders = false
    graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformed_onlyUntransformed.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  @Suppress("UndesirableClassUsage")
  fun testPaintTransformedOutsideRoot() {
    val image1 = BufferedImage(80, 150, TYPE_INT_ARGB)
    (image1.graphics as Graphics2D).run {
      paint = GradientPaint(0f, 0f, Color.RED, 80f, 100f, Color.BLUE)
      fillRect(0, 0, 80, 100)
    }

    val model = model {
      view(ROOT, 0, 0, 100, 100) {
        view(VIEW1, 20, 20, 60, 60, bounds = Polygon(intArrayOf(-20, 80, 80, -20), intArrayOf(-50, -50, 150, 150), 4)) {
          image(image1)
        }
      }
    }

    val generatedImage = BufferedImage(200, 200, TYPE_INT_ARGB)
    var graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()

    val settings = DeviceViewSettings(scalePercent = 75)
    settings.drawLabel = false
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    panel.setSize(200, 200)

    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformedOutsideRoot.png"), generatedImage,
                                     DIFF_THRESHOLD)

    model.setSelection(model[VIEW1], SelectionOrigin.INTERNAL)
    graphics = generatedImage.createGraphics()
    graphics.font = ImageDiffTestUtil.getDefaultFont()
    panel.paint(graphics)
    ImageDiffUtil.assertImageSimilar(
      resolveWorkspacePath("$TEST_DATA_PATH/testPaintTransformedOutsideRoot_view1.png"), generatedImage, DIFF_THRESHOLD)
  }

  @Test
  @Suppress("UndesirableClassUsage")
  fun testPaintEmpty() {
    IconLoader.activate()
    setPortableUiFont(2.0f)
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val model = model {}
    val stats = SessionStatistics(model, treeSettings)
    val panel = DeviceViewContentPanel(model, stats, treeSettings, DeviceViewSettings(), { mock() },
                                       disposable.disposable)
    panel.setSize(800, 400)
    val generatedImage = BufferedImage(panel.width, panel.height, TYPE_INT_ARGB)
    val graphics = generatedImage.createGraphics()
    panel.paint(graphics)
    // We have to use a big threshold here, since the image is almost all text and despite using the portable font there's still
    // some differences between platforms.
    ImageDiffUtil.assertImageSimilar(resolveWorkspacePath("$TEST_DATA_PATH/testPaintEmpty.png"), generatedImage, 0.1)
  }

  @RunsInEdt
  @Test
  fun testAutoScroll() {
    val model = model {
      view(ROOT, 0, 0, 100, 200) {
        view(VIEW1, 0, 0, 50, 50) {
          view(VIEW3, 30, 30, 10, 10)
        }
        view(VIEW2, 60, 160, 10, 20)
      }
    }
    val view1 = model[VIEW1]
    val settings = DeviceViewSettings(scalePercent = 200)
    val treeSettings = FakeTreeSettings()
    treeSettings.hideSystemNodes = false
    val panel = DeviceViewContentPanel(model, SessionStatistics(model, treeSettings), treeSettings, settings, { mock() },
                                       disposable.disposable)
    val scrollPane = JBScrollPane(panel)
    panel.setBounds(0, 0, 1000, 1000)
    scrollPane.setBounds(0, 0, 400, 400)
    scrollPane.viewport.viewPosition = Point(600, 600)
    UIUtil.dispatchAllInvocationEvents()

    // No auto scrolling when selection is changed from the image:
    model.setSelection(view1, SelectionOrigin.INTERNAL)
    assertThat(scrollPane.viewport.viewPosition).isEqualTo(Point(600, 600))

    // Auto scrolling will be in effect when selecting from the component tree:
    model.setSelection(view1, SelectionOrigin.COMPONENT_TREE)
    assertThat(scrollPane.viewport.viewPosition).isEqualTo(Point(394, 194))
  }

  private class FakeActionPopupMenu(private val group: ActionGroup) : ActionPopupMenu {
    val popup: JPopupMenu = mock()

    override fun getComponent(): JPopupMenu = popup
    override fun getActionGroup(): ActionGroup = group
    override fun getPlace(): String = error("Not implemented")
    override fun setTargetComponent(component: JComponent) = error("Not implemented")

    fun assertSelectViewAction(vararg expected: Long) {
      val event: AnActionEvent = mock()
      `when`(event.actionManager).thenReturn(ActionManager.getInstance())
      val actions = group.getChildren(event)
      assertThat(actions.size).isEqualTo(1)
      assertThat(actions[0]).isInstanceOf(DropDownAction::class.java)
      val selectActions = (actions[0] as DropDownAction).getChildren(event)
      assertThat(selectActions.toList().filterIsInstance(SelectViewAction::class.java).map { it.view.drawId })
        .containsExactlyElementsIn(expected.toList())
    }
  }
}
