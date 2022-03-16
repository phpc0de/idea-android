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
package com.android.tools.adtui;

import com.android.tools.adtui.common.StudioColorsKt;
import com.android.tools.adtui.stdui.CommonButton;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import icons.StudioIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class builds a tool window bar that looks like the intellij {@link com.intellij.openapi.wm.ToolWindow} however has the ability to
 * add custom behaviors when things are clicked, and provides the ability to remove tabs.
 */
public class TabbedToolbar extends JPanel {
  private final JPanel myActionPanel = new JPanel();
  private final JPanel myTabsPanel = new JPanel();
  private int myActiveTab;
  private int myMouseOverTab;
  // Caching value as it can change each time we add a new child.
  // Note: We cannot reference preferred height from TabLabel as it causes a circular dependency.
  // Note: If we do not set the preferred height of the child the label is not framed properly.
  // Note: If we do not use the updated preferred height when more than 1 item is added we end up with a pixel space on the first item.
  private int myPreferredHeight;

  /**
   * Creates a toolbar where the label can be replaces by a custom component.
   */
  public TabbedToolbar(@NotNull JComponent title) {
    // Set layout explicitly to remove default hgap/vgap.
    myTabsPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

    //    Fit    px                *                      Fit
    // [Title][Padding][(Tab)(Tab)(Tab)              ][(Action)(Action)]
    setLayout(new TabularLayout("Fit,5px,*,Fit", "*"));
    add(title, new TabularLayout.Constraint(0, 0));
    add(myTabsPanel, new TabularLayout.Constraint(0, 2));
    add(myActionPanel, new TabularLayout.Constraint(0, 3));
  }

  /**
   * Adds an {@link Icon} as a button in the toolbar. When the button is clicked the event is forwarded to the action listener.
   */
  public void addAction(@NotNull Icon actionIcon, @NotNull ActionListener actionListener) {
    CommonButton action = new CommonButton(actionIcon);
    action.addActionListener(actionListener);
    myActionPanel.add(action);
  }

  /**
   * Adds a tab to the toolbar. The tab
   */
  public void addTab(String name, TabListener selectedListener) {
    addTab(name, selectedListener, null);
  }

  /**
   * Adds a tab to the toolbar.
   *
   * @param name             of the tab to show.
   * @param selectedListener event to trigger when the tab is selected.
   * @param closedListener   event to trigger when the tab is closed. Presence of event indicates closing is possible.
   */
  public void addTab(@NotNull String name, @NotNull TabListener selectedListener, @Nullable TabListener closedListener) {
    TabLabel tab = new TabLabel(name, closedListener);
    tab.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        selectTab(tab, selectedListener);
        // Need to repaint to adjust blue label for selected tab.
        repaint();
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        myMouseOverTab = tab.hashCode();
        // Need to repaint to show proper background color.
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        myMouseOverTab = 0;
        // Need to repaint to reset background color.
        repaint();
      }
    });
    tab.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        myMouseOverTab = tab.hashCode();
      }

      @Override
      public void focusLost(FocusEvent e) {
        myMouseOverTab = 0;
      }
    });
    tab.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          selectTab(tab, selectedListener);
        }
      }
    });
    myTabsPanel.add(tab);
    // Cache preferred height of control so we can properly adjust child elements sizes.
    myPreferredHeight = getPreferredSize().height;
    selectTab(tab, selectedListener);
  }

  /**
   * Remove all tabs from toolbar.
   */
  public void clearTabs() {
    myTabsPanel.removeAll();
    myPreferredHeight = 0; // Clear preferred height to prevent growing forever.
  }

  private void selectTab(@NotNull TabLabel tab, @NotNull TabListener listener) {
    myActiveTab = tab.hashCode();
    listener.doAction();
  }

  @NotNull
  @VisibleForTesting
  public JComponent getTabsPanel() {
    return myTabsPanel;
  }


  /**
   * Innerclass that handles decoration / drawing of Tabs.
   */
  private class TabLabel extends JPanel {
    private Color myFocusedColor = StudioColorsKt.getTabbedPaneFocus();
    private Color myHighlightColor = StudioColorsKt.getTabbedPaneHoverHighlight();

    /**
     * Builds a tab that contains a label and optionally a close button.
     *
     * @param name     to display on label.
     * @param onClosed if provided a close button will be added and when triggered this will be called. If not provided no button will show.
     */
    TabLabel(String name, @Nullable TabListener onClosed) {
      setLayout(new BorderLayout());
      // Add spacing to match mocks.
      setBorder(JBUI.Borders.empty(5, 10));
      setFocusable(true);
      add(new JLabel(name), BorderLayout.CENTER);
      if (onClosed != null) {
        CommonButton closeButton = new CommonButton(StudioIcons.Common.CLOSE);
        closeButton.addActionListener((e) -> onClosed.doAction());
        add(closeButton, BorderLayout.EAST);
      }
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(super.getPreferredSize().width, myPreferredHeight);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      GraphicsUtil.setAntialiasingType(this, AntialiasingType.getAAHintForSwingComponent());

      // 1) Adjust our background color before painting for hover effect.
      if (myMouseOverTab == hashCode()) {
        setBackground(myHighlightColor);
      }
      else {
        setBackground(getParent().getBackground());
      }

      // 2) Paint self + children.
      super.paintComponent(g);

      // 3) Draw focused state for active tab.
      if (myActiveTab == hashCode()) {
        Rectangle bounds = this.getBounds();
        int selectedHeight = JBUIScale.scale(2);
        g.setColor(myFocusedColor);
        g.fillRect(0, bounds.height - selectedHeight, bounds.width, selectedHeight);
      }
    }
  }

  /**
   * Callback for events when interacting with tabs.
   */
  public interface TabListener {
    void doAction();
  }
}
