/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.uibuilder;

import android.view.View;
import com.android.testutils.VirtualTimeScheduler;
import com.android.tools.adtui.common.SwingCoordinate;
import com.android.tools.analytics.*;
import com.android.tools.idea.common.SyncNlModel;
import com.android.tools.idea.common.fixtures.KeyEventBuilder;
import com.android.tools.idea.common.model.DnDTransferItem;
import com.android.tools.idea.common.model.ItemTransferable;
import com.android.tools.idea.common.model.DefaultSelectionModel;
import com.android.tools.idea.uibuilder.analytics.NlUsageTracker;
import com.android.tools.idea.common.fixtures.MouseEventBuilder;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.SelectionModel;
import com.android.tools.idea.common.scene.draw.DisplayList;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.idea.common.surface.DesignSurfaceListener;
import com.android.tools.idea.common.surface.InteractionManager;
import com.android.tools.idea.uibuilder.fixtures.DropTargetDragEventBuilder;
import com.android.tools.idea.uibuilder.fixtures.DropTargetDropEventBuilder;
import com.android.tools.idea.uibuilder.model.NlComponentMixin;
import com.android.tools.idea.uibuilder.scene.LayoutlibSceneManager;
import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.android.tools.idea.uibuilder.surface.NlScreenViewProvider;
import com.android.tools.idea.uibuilder.surface.ScreenView;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.android.tools.idea.uibuilder.surface.ScreenView.DEVICE_CONTENT_SIZE_POLICY;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class LayoutTestUtilities {
  public static void dragMouse(InteractionManager manager,
                               @SwingCoordinate int x1,
                               @SwingCoordinate int y1,
                               @SwingCoordinate int x2,
                               @SwingCoordinate int y2,
                               int modifiers) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof MouseMotionListener);
    MouseMotionListener mouseListener = (MouseMotionListener)listener;
    int frames = 5;
    double x = x1;
    double y = y1;
    double xSlope = (x2 - x) / frames;
    double ySlope = (y2 - y) / frames;

    JComponent layeredPane = manager.getSurface().getLayeredPane();
    for (int i = 0; i < frames + 1; i++) {
      MouseEvent event = new MouseEventBuilder((int)x, (int)y)
        .withSource(layeredPane)
        .withMask(modifiers | InputEvent.BUTTON1_DOWN_MASK)
        .withId(MouseEvent.MOUSE_DRAGGED)
        .build();
      mouseListener.mouseDragged(event);
      x += xSlope;
      y += ySlope;
    }
  }

  public static void moveMouse(InteractionManager manager,
                               @SwingCoordinate int x1,
                               @SwingCoordinate int y1,
                               @SwingCoordinate int x2,
                               @SwingCoordinate int y2) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof MouseMotionListener);
    MouseMotionListener mouseListener = (MouseMotionListener)listener;

    JComponent layeredPane = manager.getSurface().getLayeredPane();
    int frames = 5;
    double x = x1;
    double y = y1;
    double xSlope = (x2 - x) / frames;
    double ySlope = (y2 - y) / frames;
    for (int i = 0; i < frames + 1; i++) {
      MouseEvent event = new MouseEventBuilder((int)x, (int)y)
        .withSource(layeredPane)
        .withId(MouseEvent.MOUSE_MOVED)
        .build();
      mouseListener.mouseMoved(event);
      x += xSlope;
      y += ySlope;
    }
  }

  public static void pressMouse(InteractionManager manager, int button, @SwingCoordinate int x, @SwingCoordinate int y, int modifiers) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof MouseListener);
    MouseListener mouseListener = (MouseListener)listener;
    JComponent layeredPane = manager.getSurface().getLayeredPane();
    mouseListener.mousePressed(new MouseEventBuilder(x, y)
                                 .withSource(layeredPane)
                                 .withMask(modifiers)
                                 .withButton(button)
                                 .withId(MouseEvent.MOUSE_PRESSED)
                                 .build());
  }

  public static void releaseMouse(InteractionManager manager, int button, @SwingCoordinate int x, @SwingCoordinate int y, int modifiers) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof MouseListener);
    MouseListener mouseListener = (MouseListener)listener;
    JComponent layeredPane = manager.getSurface().getLayeredPane();
    mouseListener.mouseReleased(new MouseEventBuilder(x, y)
                                  .withSource(layeredPane)
                                  .withMask(modifiers)
                                  .withButton(button)
                                  .withId(MouseEvent.MOUSE_RELEASED).build());
  }

  public static void clickMouse(InteractionManager manager,
                                int button,
                                int count,
                                @SwingCoordinate int x,
                                @SwingCoordinate int y,
                                int modifiers) {
    JComponent layeredPane = manager.getSurface().getLayeredPane();
    for (int i = 0; i < count; i++) {
      pressMouse(manager, button, x, y, modifiers);
      releaseMouse(manager, button, x, y, modifiers);

      Object listener = manager.getListener();
      assertTrue(listener instanceof MouseListener);
      MouseListener mouseListener = (MouseListener)listener;
      MouseEvent event =
        new MouseEventBuilder(x, y)
          .withSource(layeredPane)
          .withButton(button)
          .withMask(modifiers)
          .withClickCount(i + 1)
          .withId(MouseEvent.MOUSE_CLICKED)
          .build();
      mouseListener.mouseClicked(event);
    }
  }

  public static void dragDrop(InteractionManager manager,
                              @SwingCoordinate int x1,
                              @SwingCoordinate int y1,
                              @SwingCoordinate int x2,
                              @SwingCoordinate int y2,
                              Transferable transferable) {
    dragDrop(manager, x1, y1, x2, y2, transferable, DnDConstants.ACTION_COPY);
  }

  public static void dragDrop(InteractionManager manager,
                              @SwingCoordinate int x1,
                              @SwingCoordinate int y1,
                              @SwingCoordinate int x2,
                              @SwingCoordinate int y2,
                              Transferable transferable,
                              int dropAction) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof DropTargetListener);
    DropTargetListener dropListener = (DropTargetListener)listener;
    int frames = 5;
    double x = x1;
    double y = y1;
    double xSlope = (x2 - x) / frames;
    double ySlope = (y2 - y) / frames;

    DropTargetContext context = createDropTargetContext();
    dropListener.dragEnter(new DropTargetDragEventBuilder(context, (int)x, (int)y, transferable).withDropAction(dropAction).build());
    for (int i = 0; i < frames + 1; i++) {
      dropListener.dragOver(new DropTargetDragEventBuilder(context, (int)x, (int)y, transferable).withDropAction(dropAction).build());
      x += xSlope;
      y += ySlope;
    }

    DropTargetDropEvent dropEvent =
      new DropTargetDropEventBuilder(context, (int)x, (int)y, transferable).withDropAction(dropAction).build();
    dropListener.drop(dropEvent);

    verify(dropEvent, times(1)).acceptDrop(anyInt());
    verify(dropEvent, times(1)).dropComplete(true);
  }

  public static void releaseKey(@NotNull InteractionManager manager, int keyCode) {
    Object listener = manager.getListener();
    assertTrue(listener instanceof KeyListener);
    KeyListener keyListener = (KeyListener)listener;
    JComponent layeredPane = manager.getSurface().getLayeredPane();
    keyListener.keyReleased(new KeyEventBuilder(keyCode, KeyEvent.CHAR_UNDEFINED)
                              .withSource(layeredPane)
                              .build());
  }

  public static ScreenView createScreen(SyncNlModel model) {
    return createScreen(model, 1, 0, 0);
  }

  public static ScreenView createScreen(SyncNlModel model, double scale,
                                        @SwingCoordinate int x, @SwingCoordinate int y) {
    NlDesignSurface surface = (NlDesignSurface) model.getSurface();
    LayoutlibSceneManager spy = spy(surface.getSceneManager());
    when(surface.getSceneManager()).thenReturn(spy);
    ScreenView screenView = new ScreenView(surface, spy, DEVICE_CONTENT_SIZE_POLICY) {
      @Override
      public double getScale() {
        return scale;
      }
    };
    screenView.setLocation(x, y);
    when(spy.getSceneView()).thenReturn(screenView);
    surface.getScene().buildDisplayList(new DisplayList(), 0);
    return screenView;
  }

  public static DesignSurface createSurface(Class<? extends DesignSurface> surfaceClass) {
    JComponent layeredPane = new JPanel();
    DesignSurface surface = mock(surfaceClass);
    SelectionModel selectionModel = new DefaultSelectionModel();
    List<DesignSurfaceListener> listeners = new ArrayList<>();
    when(surface.getLayeredPane()).thenReturn(layeredPane);
    when(surface.getSelectionModel()).thenReturn(selectionModel);
    when(surface.getSize()).thenReturn(new Dimension(1000, 1000));
    when(surface.getScale()).thenReturn(0.5);
    when(surface.getSelectionAsTransferable()).thenReturn(new ItemTransferable(new DnDTransferItem(0, ImmutableList.of())));
    doAnswer(inv -> listeners.add(inv.getArgument(0))).when(surface).addListener(any(DesignSurfaceListener.class));
    doAnswer(inv -> listeners.remove((DesignSurfaceListener)inv.getArgument(0))).when(surface).removeListener(any(DesignSurfaceListener.class));
    selectionModel.addListener((model, selection) -> listeners.forEach(listener -> listener.componentSelectionChanged(surface, selection)));
    if (NlDesignSurface.class.equals(surfaceClass)) {
      when(((NlDesignSurface)surface).getScreenViewProvider()).thenReturn(NlScreenViewProvider.BLUEPRINT);
    }
    return surface;
  }

  public static DropTargetContext createDropTargetContext() {
    return mock(DropTargetContext.class);
  }

  public static Transferable createTransferable(DataFlavor flavor, Object data) throws IOException, UnsupportedFlavorException {
    Transferable transferable = mock(Transferable.class);

    when(transferable.getTransferDataFlavors()).thenReturn(new DataFlavor[]{flavor});
    when(transferable.getTransferData(eq(flavor))).thenReturn(data);
    when(transferable.isDataFlavorSupported(eq(flavor))).thenReturn(true);

    return transferable;
  }

  public static View mockViewWithBaseline(int baseline) {
    View view = mock(View.class);
    when(view.getBaseline()).thenReturn(baseline);
    return view;
  }

  @Nullable
  public static AnAction findActionForKey(@NotNull JComponent component, int keyCode, int modifiers) {
    Shortcut shortcutToFind = new KeyboardShortcut(KeyStroke.getKeyStroke(keyCode, modifiers), null);
    java.util.List<AnAction> actions = ActionUtil.getActions(component);
    for (AnAction action : actions) {
      for (Shortcut shortcut : action.getShortcutSet().getShortcuts()) {
        if (shortcut.equals(shortcutToFind)) {
          return action;
        }
      }
    }
    return null;
  }

  public static NlUsageTracker mockNlUsageTracker(@NotNull DesignSurface surface) {
    AnalyticsSettingsData settings = new AnalyticsSettingsData();
    settings.setOptedIn(true);
    AnalyticsSettings.setInstanceForTest(settings);

    UsageTrackerWriter tracker = new TestUsageTracker(new VirtualTimeScheduler());
    UsageTracker.setWriterForTest(tracker);

    NlUsageTracker usageTracker = mock(NlUsageTracker.class);
    NlUsageTracker.MANAGER.setInstanceForTest(surface, usageTracker);
    return usageTracker;
  }

  public static void cleanUsageTrackerAfterTesting(@NotNull DesignSurface surface) {
    NlUsageTracker.MANAGER.cleanAfterTesting(surface);
    UsageTracker.cleanAfterTesting();
  }

  public static NlComponent createMockComponent() {
    NlComponent mock = mock(NlComponent.class);
    when(mock.getMixin()).thenReturn(new NlComponentMixin(mock));
    return mock;
  }
}
