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

package com.android.tools.idea.uibuilder.handlers.constraint.drawing;

import com.android.tools.idea.common.scene.draw.ColorSet;
import com.android.tools.idea.uibuilder.handlers.constraint.drawing.decorator.ColorTheme;
import com.android.tools.idea.uibuilder.handlers.constraint.drawing.decorator.WidgetDecoratorConstants;
import java.awt.Color;

/**
 * Default color set for the "blueprint" UI mode
 */
@SuppressWarnings("UseJBColor")
public class BlueprintColorSet extends ColorSet {

    public BlueprintColorSet() {

        mStyle = WidgetDecoratorConstants.BLUEPRINT_STYLE;

        mDrawBackground = true;
        mDrawWidgetInfos = false;

        // Base colors

        mBackground = new Color(0x225C6E);
        mComponentObligatoryBackground = new Color(0x225C6E);
        mComponentBackground = new Color(0x3386CFE5, true);
        mFrames = new Color(0xCC86CFE5, true);
        mConstraints = new Color(0xCC86CFE5, true);
        mSoftConstraintColor = new Color(102, 129, 204, 80);
        mButtonBackground  = new Color(51, 105, 153, 0);
        mMargins = new Color(0xCC86CFE5, true);;
        mText = new Color(220, 220, 220);
        mSnapGuides = new Color(220, 220, 220);
        mFakeUI = new Color(0x5DA0B5);
        myUnconstrainedColor = new Color(220, 103, 53);

        // Subdued colors

        mSubduedConstraints = ColorTheme.updateBrightness(mConstraints, 0.7f);
        mSubduedBackground = ColorTheme.updateBrightness(mBackground, 0.8f);
        mSubduedText = ColorTheme.fadeToColor(mText, mSubduedBackground, 0.6f);
        mSubduedFrames = ColorTheme.updateBrightness(mFrames, 0.8f);

        // Light colors

        mHighlightedBackground = ColorTheme.updateBrightness(mBackground, 1.3f);
        mHighlightedFrames = mFrames;
        mHighlightedSnapGuides = new Color(220, 220, 220, 128);
        mHighlightedConstraints = new Color(0xEAFAFF);
        mComponentHighlightedBackground = ColorTheme.updateBrightness(mComponentBackground, 1.0f, 0x66);

        // Selected colors

        mSelectedBackground = ColorTheme.updateBrightness(mBackground, 1.3f);
        mSelectedConstraints = Color.white;
        mSelectedFrames = new Color(0xEAFAFF);
        mSelectedText = ColorTheme.fadeToColor(mText, mSelectedBackground, 0.7f);

        // Anchor colors

        mAnchorCircle = Color.white;
        mAnchorCreationCircle = Color.white;
        mAnchorDisconnectionCircle = new Color(0xDB5860);
        mAnchorConnectionCircle = new Color(0xE3F3FF);

        mSelectionColor = Color.white;

        // Widget actions

        mWidgetActionBackground = ColorTheme.fadeToColor(mSelectedConstraints, mSelectedBackground, 0.9f);
        mWidgetActionSelectedBackground = ColorTheme.fadeToColor(mSelectedConstraints, mSelectedBackground, 0.5f);

        // Tooltip

        mTooltipBackground = Color.white;
        mTootipText = Color.black;

        // Inspector colors

        mInspectorStrokeColor = mFrames;
        mInspectorTrackBackgroundColor = new Color(228, 228, 238);
        mInspectorTrackColor = new Color(208, 208, 218);
        mInspectorHighlightsStrokeColor = new Color(160, 160, 180, 128);

        mInspectorBackgroundColor =
                ColorTheme.fadeToColor(mBackground, Color.WHITE, 0.1f);
        mInspectorFillColor = ColorTheme
                .fadeToColor(ColorTheme.updateBrightness(mBackground, 1.3f),
                        Color.WHITE, 0.1f);

        // Lasso colors

        mLassoSelectionBorder = new Color(mSelectedFrames.getRed(), mSelectedFrames.getGreen(), mSelectedFrames.getBlue(), 192);
        mLassoSelectionFill = new Color(mSelectedFrames.getRed(), mSelectedFrames.getGreen(), mSelectedFrames.getBlue(), 26);
    }
}
