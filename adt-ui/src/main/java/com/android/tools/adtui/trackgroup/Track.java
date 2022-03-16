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
package com.android.tools.adtui.trackgroup;

import com.android.tools.adtui.TabularLayout;
import com.android.tools.adtui.common.StudioColorsKt;
import com.android.tools.adtui.model.trackgroup.TrackModel;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import icons.StudioIcons;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import org.jetbrains.annotations.NotNull;

/**
 * Represents horizontal data visualization (e.g. time-based data series), used in a {@link TrackGroup}.
 */
public class Track {
  /**
   * The first column displays the title header. The second column displays the track content.
   */
  static final int DEFAULT_TITLE_COL_PX = 150;
  public static final String COL_SIZES = DEFAULT_TITLE_COL_PX + "px,*";

  /**
   * Number of pixels of the border when the track is selected.
   */
  private static final int SELECTION_BORDER_PX = 2;

  private static final Icon COLLAPSE_ICON = AllIcons.General.ArrowRight;
  private static final Icon EXPAND_ICON = AllIcons.General.ArrowDown;
  static final Icon REORDER_ICON = StudioIcons.Common.REORDER;
  private static final Border TITLE_BORDER_DEFAULT = JBUI.Borders.merge(
    JBUI.Borders.customLine(StudioColorsKt.getBorder(), 0, 0, 0, 1),
    JBUI.Borders.empty(SELECTION_BORDER_PX, SELECTION_BORDER_PX, SELECTION_BORDER_PX, 0),
    false);
  private static final Border TITLE_BORDER_SELECTED = JBUI.Borders.merge(
    JBUI.Borders.customLine(StudioColorsKt.getBorder(), 0, 0, 0, 1),
    JBUI.Borders.customLine(StudioColorsKt.getSelectionBackground(), SELECTION_BORDER_PX, SELECTION_BORDER_PX, SELECTION_BORDER_PX, 0),
    false);
  private static final Border CONTENT_BORDER_DEFAULT = JBUI.Borders.customLine(
    StudioColorsKt.getPrimaryContentBackground(), SELECTION_BORDER_PX, 0, SELECTION_BORDER_PX, 0);
  private static final Border CONTENT_BORDER_SELECTED = JBUI.Borders.customLine(
    StudioColorsKt.getSelectionBackground(), SELECTION_BORDER_PX, 0, SELECTION_BORDER_PX, 0);

  @NotNull private final JPanel myComponent;
  @NotNull private final JPanel myTitleBackPanel;
  @NotNull private final JPanel myTitleFrontPanel;
  @NotNull private final JLabel myTitleLabel;
  @NotNull private final JComponent myTrackContent;

  private Track(@NotNull TrackModel trackModel, @NotNull JComponent trackContent) {
    myTrackContent = trackContent;

    // Icon for reordering tracks via drag-n-drop.
    JLabel recorderIconLabel = new JLabel(REORDER_ICON);
    recorderIconLabel.setVerticalAlignment(SwingConstants.TOP);
    recorderIconLabel.setBorder(JBUI.Borders.emptyTop(4));

    myTitleLabel = new JLabel(trackModel.getTitle());
    myTitleLabel.setVerticalAlignment(SwingConstants.TOP);
    myTitleLabel.setToolTipText(trackModel.getTitleTooltip());
    myTitleLabel.setIconTextGap(0);
    if (trackModel.isCollapsible()) {
      myTitleLabel.setIcon(trackModel.isCollapsed() ? COLLAPSE_ICON : EXPAND_ICON);
      myTitleLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          // Get the icon's bounding box relative to the label.
          Rectangle iconRect = new Rectangle(myTitleLabel.getInsets().left, myTitleLabel.getInsets().top,
                                             myTitleLabel.getIcon().getIconWidth(), myTitleLabel.getIcon().getIconHeight());
          // Single-clicking the icon or doubling-clicking the label should expand/collapse the track.
          if (iconRect.contains(e.getPoint()) || e.getClickCount() == 2) {
            trackModel.setCollapsed(!trackModel.isCollapsed());
            myTitleLabel.setIcon(trackModel.isCollapsed() ? COLLAPSE_ICON : EXPAND_ICON);
          }
        }
      });
    }
    int leftPadding = myTitleLabel.getIcon() != null ? 2 : COLLAPSE_ICON.getIconWidth() + myTitleLabel.getIconTextGap() + 2;
    myTitleLabel.setBorder(JBUI.Borders.empty(4, leftPadding, 4, 0));

    // Front panel has a dynamic background color based on selection state.
    myTitleFrontPanel = new JPanel(new BorderLayout());
    myTitleFrontPanel.add(recorderIconLabel, BorderLayout.WEST);
    myTitleFrontPanel.add(myTitleLabel, BorderLayout.CENTER);

    // Back panel has a static background color but its border color changes based on selection state.
    // Note the border has to be applied to this back panel, otherwise it will be completely covered by the front panel and Swing will
    // NOT render its pixels, causing the root panel's background color to show through front panel's (transparent) background color.
    myTitleBackPanel = new JPanel(new BorderLayout());
    myTitleBackPanel.setBackground(StudioColorsKt.getPrimaryContentBackground());
    myTitleBackPanel.add(myTitleFrontPanel);

    myComponent = new JPanel(new TabularLayout(COL_SIZES, "Fit"));
    if (trackModel.getHideHeader()) {
      myComponent.add(trackContent, new TabularLayout.Constraint(0, 0, 2));
    }
    else {
      myComponent.add(myTitleBackPanel, new TabularLayout.Constraint(0, 0));
      myComponent.add(trackContent, new TabularLayout.Constraint(0, 1));
    }
  }

  /**
   * Factory method to instantiate a Track.
   *
   * @param <M>           data model type
   * @param <R>           renderer enum type
   * @param trackModel    data model
   * @param trackRenderer UI renderer
   * @return a Track that visualizes the given {@link TrackModel} using the provided {@link TrackRenderer}
   */
  @NotNull
  public static <M, R extends Enum> Track create(@NotNull TrackModel<M, R> trackModel, @NotNull TrackRenderer<M, R> trackRenderer) {
    return new Track(trackModel, trackRenderer.render(trackModel));
  }

  /**
   * Update UI to reflect selection state.
   *
   * @return current instance
   */
  @NotNull
  public Track updateSelected(boolean selected) {
    myTitleFrontPanel.setBackground(selected ? StudioColorsKt.getSelectionOverlayBackground() : null);
    myTitleBackPanel.setBorder(selected ? TITLE_BORDER_SELECTED : TITLE_BORDER_DEFAULT);
    myTrackContent.setBorder(selected ? CONTENT_BORDER_SELECTED : CONTENT_BORDER_DEFAULT);
    return this;
  }

  /**
   * @return the UI component of this Track.
   */
  @NotNull
  public JComponent getComponent() {
    return myComponent;
  }

  @NotNull
  public JComponent getTrackContent() {
    return myTrackContent;
  }

  @NotNull
  public JLabel getTitleLabel() {
    return myTitleLabel;
  }

  @VisibleForTesting
  @NotNull
  JPanel getTitleFrontPanel() {
    return myTitleFrontPanel;
  }
}
