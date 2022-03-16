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
package com.android.tools.adtui.treegrid;

import static com.google.common.truth.Truth.assertThat;
import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static java.awt.event.KeyEvent.KEY_TYPED;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UNDEFINED;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.ImageUtil;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import com.intellij.util.ui.ImageUtil;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TreeGridTest extends LightPlatformTestCase {
  private TreeGrid<String> myGrid;
  private JList<String> myGroup1;
  private JList<String> myGroup2;
  private JList<String> myGroup3;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    myGrid = new TreeGrid<>();
    myGrid.setSize(140, 800);
    myGrid.setModel(createTree());
    myGrid.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    myGrid.setFixedCellWidth(40);
    myGrid.setFixedCellHeight(40);
    myGrid.doLayout();
    BufferedImage image = ImageUtil.createImage(1000, 1000, TYPE_INT_ARGB);
    myGrid.paint(image.getGraphics());

    List<JList<String>> lists = myGrid.getLists();
    myGroup1 = lists.get(0);
    myGroup2 = lists.get(1);
    myGroup3 = lists.get(2);
    removeToolTipListener(lists);
  }

  public void testIsFiltered() {
    assertThat(myGrid.isFiltered()).isFalse();
    myGrid.setFilter(s -> s.equals("b2"));
    assertThat(myGrid.isFiltered()).isTrue();
  }

  public void testSelectIfUnique() {
    myGrid.selectIfUnique();
    assertThat(myGrid.getSelectedElement()).isNull();
  }

  public void testSelectIfUniqueWithUniqueFilter() {
    myGrid.setFilter(s -> s.equals("b2"));
    myGrid.selectIfUnique();
    assertThat(myGrid.getSelectedElement()).isEqualTo("b2");
  }

  public void testSelectIfUniqueWithNonUniqueFilter() {
    myGrid.setFilter(s -> s.startsWith("b"));
    myGrid.selectIfUnique();
    assertThat(myGrid.getSelectedElement()).isNull();
  }

  public void testFilterMatchCountWithoutFilterSet() {
    assertThat(myGrid.getFilterMatchCount()).isEqualTo(-1);
  }

  public void testFilterMatchCountWithNoMatches() {
    myGrid.setFilter(s -> s.startsWith("no-matches-expected"));
    assertThat(myGrid.getFilterMatchCount()).isEqualTo(0);
  }

  public void testFilterMatchCountWithSingleMatch() {
    myGrid.setFilter(s -> s.startsWith("b2"));
    assertThat(myGrid.getFilterMatchCount()).isEqualTo(1);
  }

  public void testFilterMatchCountWithMultipleMatches() {
    myGrid.setFilter(s -> s.startsWith("b"));
    assertThat(myGrid.getFilterMatchCount()).isEqualTo(3);
  }

  public void testSetVisibleSection() {
    myGrid.setVisibleSection("g2");
    assertThat(myGroup1.isVisible()).isFalse();
    assertThat(myGroup2.isVisible()).isTrue();
    assertThat(myGroup3.isVisible()).isFalse();

    myGrid.setVisibleSection(null);
    assertThat(myGroup1.isVisible()).isTrue();
    assertThat(myGroup2.isVisible()).isTrue();
    assertThat(myGroup3.isVisible()).isTrue();
  }

  public void testSelectFirst() {
    myGrid.selectFirst();
    assertThat(myGrid.getSelectedElement()).isEqualTo("a1");
  }

  public void testSelectNext() {
    myGrid.selectFirst();
    key(VK_RIGHT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b1");
  }

  public void testSelectNextGoesToNextGroup() {
    myGrid.setSelectedElement("d1");
    key(VK_RIGHT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("a2");
  }

  public void testSelectNextStopsWithLastElement() {
    myGrid.setSelectedElement("d3");
    key(VK_RIGHT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d3");
  }

  public void testSelectPrev() {
    myGrid.setSelectedElement("c1");
    key(VK_LEFT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b1");
  }

  public void testSelectPrevGoesToPrevGroup() {
    myGrid.setSelectedElement("a3");
    key(VK_LEFT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b2");
  }

  public void testSelectPrevStopsWithFirstElement() {
    myGrid.setSelectedElement("a1");
    key(VK_LEFT);
    assertThat(myGrid.getSelectedElement()).isEqualTo("a1");
  }

  public void testSelectDown() {
    myGrid.setSelectedElement("a1");
    key(VK_DOWN);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d1");
  }

  public void testSelectDownRemembersColumn() {
    myGrid.setSelectedElement("b1");
    key(VK_DOWN);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d1");
    key(VK_DOWN);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b2");
  }

  public void testSelectDownStopsAtLastRow() {
    myGrid.setSelectedElement("a3");
    key(VK_DOWN);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d3");
    key(VK_DOWN);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d3");
  }

  public void testSelectUp() {
    myGrid.setSelectedElement("a2");
    key(VK_UP);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d1");
  }

  public void testSelectUpRemembersColumn() {
    myGrid.setSelectedElement("b2");
    key(VK_UP);
    assertThat(myGrid.getSelectedElement()).isEqualTo("d1");
    key(VK_UP);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b1");
    key(VK_UP);
    assertThat(myGrid.getSelectedElement()).isEqualTo("b1");
  }

  public void testNoNavigationWithoutSelection() {
    myGrid.selectFirst();
    JComponent component = myGrid.getSelectedList();
    myGrid.setSelectedElement(null);

    key(VK_UP, component);
    key(VK_DOWN, component);
    key(VK_LEFT, component);
    key(VK_RIGHT, component);
    assertThat(myGrid.getSelectedElement()).isNull();
  }

  public void testKeyEventsAreSentToCustomKeyListener() {
    List<KeyEvent> received = new ArrayList<>();
    myGrid.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(@NotNull KeyEvent event) {
        received.add(event);
      }

      @Override
      public void keyReleased(@NotNull KeyEvent event) {
        received.add(event);
      }

      @Override
      public void keyTyped(@NotNull KeyEvent event) {
        received.add(event);
      }
    });

    myGrid.selectFirst();
    key(VK_ENTER, KEY_PRESSED, '\0', 0);
    assertThat(received.size()).isEqualTo(1);

    key(VK_UNDEFINED, KEY_TYPED, 'g', InputEvent.ALT_DOWN_MASK);
    assertThat(received.size()).isEqualTo(2);

    key(VK_ENTER, KEY_RELEASED, '\0', 0);
    assertThat(received.size()).isEqualTo(3);
  }

  public void testKeyEventsGoesToCustomKeyListenerFirst() {
    myGrid.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(@NotNull KeyEvent event) {
        event.consume();
      }
    });
    myGrid.setSelectedElement("a2");
    key(VK_UP);
    // Test that we did not go anywhere.
    // Would have been "d1" without a custom listener. See testSelectUp.
    assertThat(myGrid.getSelectedElement()).isEqualTo("a2");
  }

  public void testFocusEventsAreSentToCustomFocusListener() {
    List<FocusEvent> received = new ArrayList<>();
    myGrid.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(@NotNull FocusEvent event) {
        received.add(event);
      }

      @Override
      public void focusLost(@NotNull FocusEvent event) {
        received.add(event);
      }
    });
    JComponent component1 = myGroup1;
    JComponent component2 = new JPanel();

    focusLoss(component1, component2);
    assertThat(received.size()).isEqualTo(1);

    focusGain(component1, component2);
    assertThat(received.size()).isEqualTo(2);
  }

  public void testNoFocusEventIsTriggeredWhenFocusChangesBetweenLists() {
    List<FocusEvent> received = new ArrayList<>();
    myGrid.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(@NotNull FocusEvent event) {
        received.add(event);
      }

      @Override
      public void focusLost(@NotNull FocusEvent event) {
        received.add(event);
      }
    });

    JComponent component1 = myGroup1;
    JComponent component2 = myGroup2;

    focusLoss(component1, component2);
    focusGain(component2, component1);

    assertThat(received).isEmpty();
  }

  public void testMouseEventsAreSentToCustomMouseListener() {
    List<MouseEvent> received = new ArrayList<>();
    myGrid.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(@NotNull MouseEvent event) {
        received.add(event);
      }

      @Override
      public void mousePressed(@NotNull MouseEvent event) {
        received.add(event);
      }

      @Override
      public void mouseReleased(@NotNull MouseEvent event) {
        received.add(event);
      }

      @Override
      public void mouseEntered(@NotNull MouseEvent event) {
        received.add(event);
      }

      @Override
      public void mouseExited(@NotNull MouseEvent event) {
        received.add(event);
      }
    });
    mouse(MouseEvent.MOUSE_CLICKED);
    assertThat(received.size()).isEqualTo(1);

    mouse(MouseEvent.MOUSE_PRESSED);
    assertThat(received.size()).isEqualTo(2);

    mouse(MouseEvent.MOUSE_RELEASED);
    assertThat(received.size()).isEqualTo(3);

    mouse(MouseEvent.MOUSE_ENTERED);
    assertThat(received.size()).isEqualTo(4);

    if (!GraphicsEnvironment.isHeadless()) {
      // Fails on headless setup
      mouse(MouseEvent.MOUSE_EXITED);
      assertThat(received.size()).isEqualTo(5);
    }
  }

  public void testListSelectionListenerAreSentToCustomListSelectionListener() {
    List<ListSelectionEvent> received = new ArrayList<>();
    myGrid.addListSelectionListener(received::add);

    JList<String> list = myGroup1;
    ListSelectionEvent event = new ListSelectionEvent(list, 0, 4, false);
    for (ListSelectionListener listener : list.getListSelectionListeners()) {
      listener.valueChanged(event);
    }
    assertThat(received.size()).isEqualTo(1);
  }

  private void key(@MagicConstant(flagsFromClass = KeyEvent.class) int keyCode) {
    JComponent component = myGrid.getSelectedList();
    assertThat(component).isNotNull();
    key(keyCode, KEY_PRESSED, '\0', 0, component, component);
  }

  private void key(@MagicConstant(flagsFromClass = KeyEvent.class) int keyCode, int id, char keyChar, int modifiers) {
    JComponent component = myGrid.getSelectedList();
    assertThat(component).isNotNull();
    key(keyCode, id, keyChar, modifiers, component, component);
  }

  private static void key(@MagicConstant(flagsFromClass = KeyEvent.class) int keyCode, @NotNull JComponent component) {
    key(keyCode, KEY_PRESSED, '\0', 0, component, component);
  }

  private static void key(@MagicConstant(flagsFromClass = KeyEvent.class) int keyCode, int id, char keyChar, int modifiers,
                          @NotNull JComponent source,
                          @NotNull JComponent target) {
    KeyEvent event = new KeyEvent(source, id, System.currentTimeMillis(), modifiers, keyCode, keyChar);
    for (KeyListener listener : target.getKeyListeners()) {
      switch (id) {
        case KEY_PRESSED:
          listener.keyPressed(event);
          break;
        case KEY_TYPED:
          listener.keyTyped(event);
          break;
        case KEY_RELEASED:
          listener.keyReleased(event);
          break;
      }
    }
  }

  private static void focusGain(@NotNull JComponent source, @NotNull JComponent opposite) {
    FocusEvent event = new FocusEvent(source, FocusEvent.FOCUS_GAINED, false, opposite);
    for (FocusListener listener : source.getFocusListeners()) {
      listener.focusGained(event);
    }
  }

  private static void focusLoss(@NotNull JComponent source, @NotNull JComponent opposite) {
    FocusEvent event = new FocusEvent(source, FocusEvent.FOCUS_LOST, false, opposite);
    for (FocusListener listener : source.getFocusListeners()) {
      listener.focusLost(event);
    }
  }

  private void mouse(int id) {
    JComponent component = myGroup1;
    MouseEvent event = new MouseEvent(component, id, 0, 0, 10, 15, 1, false);
    component.dispatchEvent(event);
  }

  // There is a cleanup issue with the timers in ToolTipManager.
  // Remove the registrations such that this doesn't happen.
  private static void removeToolTipListener(List<JList<String>> components) {
    ToolTipManager manager = ToolTipManager.sharedInstance();
    components.forEach(component -> manager.unregisterComponent(component));
  }

  private static AbstractTreeStructure createTree() {
    return new AbstractTreeStructure() {
      @NotNull
      @Override
      public Object getRootElement() {
        return "root";
      }

      @NotNull
      @Override
      public Object[] getChildElements(@NotNull Object element) {
        switch ((String)element) {
          case "root":
            return new Object[]{"g1", "g2", "g3"};
          case "g1":
            return new Object[]{"a1", "b1", "c1", "d1"};
          case "g2":
            return new Object[]{"a2", "b2"};
          case "g3":
            return new Object[]{"a3", "b3", "c3", "d3"};
          default:
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }
      }

      @Nullable
      @Override
      public Object getParentElement(@NotNull Object element) {
        switch ((String)element) {
          case "c":
            return "a";
          case "a":
          case "b":
            return "root";
          default:
            return null;
        }
      }

      @NotNull
      @Override
      public NodeDescriptor createDescriptor(@NotNull Object element, NodeDescriptor parentDescriptor) {
        return new NodeDescriptor(null, parentDescriptor) {

          @Override
          public boolean update() {
            return false;
          }

          @Override
          public Object getElement() {
            return element;
          }
        };
      }

      @Override
      public void commit() {
      }

      @Override
      public boolean hasSomethingToCommit() {
        return false;
      }
    };
  }
}