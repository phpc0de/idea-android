/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.handlers.constraint.targets;

import com.android.SdkConstants;
import com.android.tools.idea.common.model.AndroidDpCoordinate;
import com.android.tools.idea.common.model.NlAttributesHolder;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.scene.SceneComponent;
import com.android.tools.idea.common.scene.target.DragBaseTarget;
import com.android.tools.idea.common.scene.target.MultiComponentTarget;
import com.android.tools.idea.uibuilder.api.actions.ToggleAutoConnectAction;
import com.android.tools.idea.uibuilder.handlers.constraint.ConstraintComponentUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Implements a target allowing dragging a widget for the ConstraintLayout viewgroup
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class ConstraintDragTarget extends DragBaseTarget implements MultiComponentTarget {

  @AndroidDpCoordinate protected int myOffsetX;
  @AndroidDpCoordinate protected int myOffsetY;

  @AndroidDpCoordinate private final static int ourSnapMarginDistance = 4; // snap on 4dp

  private ConstraintDropHandler myDropHandler;

  @Override
  public void setComponent(@NotNull SceneComponent component) {
    super.setComponent(component);
    myDropHandler = new ConstraintDropHandler(myComponent);
  }

  @Override
  protected boolean isAutoConnectionEnabled() {
    return !SdkConstants.CONSTRAINT_LAYOUT_GUIDELINE.isEqualsIgnoreCase(myComponent.getNlComponent().getTagName()) &&
           ToggleAutoConnectAction.isAutoconnectOn();
  }

  @Override
  protected void updateAttributes(@NotNull NlAttributesHolder attributes, @AndroidDpCoordinate int x, @AndroidDpCoordinate int y) {
    SceneComponent constraintLayoutComponent = myComponent.getParent();
    assert constraintLayoutComponent != null;
    myDropHandler.updateAttributes(attributes, constraintLayoutComponent, x, y);
  }

  /**
   * Handle the drop logic for ConstraintLayout.
   */
  public static class ConstraintDropHandler {
    @NotNull private final SceneComponent myComponent;
    private final ChainChecker myChainChecker = new ChainChecker();

    public ConstraintDropHandler(@NotNull SceneComponent component) {
      myComponent = component;
    }

    public void updateAttributes(@NotNull NlAttributesHolder attributes,
                                 @NotNull SceneComponent parent,
                                 @AndroidDpCoordinate int x,
                                 @AndroidDpCoordinate int y) {
      SceneComponent targetStartComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourStartAttributes);
      SceneComponent targetEndComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourEndAttributes);
      SceneComponent targetLeftComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourLeftAttributes);
      SceneComponent targetRightComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourRightAttributes);
      String targetStartMargin = SdkConstants.ATTR_LAYOUT_MARGIN_START;
      String targetEndMargin = SdkConstants.ATTR_LAYOUT_MARGIN_END;
      boolean useStartEnd = myComponent.useRtlAttributes();
      boolean isInRTL = myComponent.getScene().isInRTL();

      int dx1 = 0;
      int dx2 = 0;

      if (targetStartComponent == null && targetEndComponent == null) {
        // No start/end, let's use left/right
        targetStartComponent = targetLeftComponent;
        targetEndComponent = targetRightComponent;
        targetStartMargin = SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
        targetEndMargin = SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
        useStartEnd = false;
        isInRTL = false;
        if (targetStartComponent != null) {
          dx1 = getLeftTargetOrigin(targetStartComponent) + getMarginValue(targetStartMargin);
        }
        if (targetEndComponent != null) {
          dx2 = getRightTargetOrigin(targetEndComponent) - getMarginValue(targetEndMargin);
        }
      }
      else {
        if (targetStartComponent != null) {
          dx1 = getStartTargetOrigin(targetStartComponent, isInRTL);
          int margin = getMarginValue(targetStartMargin);
          if (isInRTL) {
            dx1 -= margin;
          }
          else {
            dx1 += margin;
          }
        }
        if (targetEndComponent != null) {
          dx2 = getEndTargetOrigin(targetEndComponent, isInRTL);
          int margin = getMarginValue(targetEndMargin);
          if (isInRTL) {
            dx2 += margin;
          }
          else {
            dx2 -= margin;
          }
        }
      }
      myChainChecker.checkIsInChain(myComponent);

      if (targetStartComponent != null && targetEndComponent != null) {
        if (!myChainChecker.isInHorizontalChain()) {
          float dw = dx2 - dx1 - myComponent.getDrawWidth();
          float bias = (x - dx1) / dw;
          if (useStartEnd && isInRTL) {
            dw = dx1 - dx2 - myComponent.getDrawWidth();
            bias = (dw - (x - dx2)) / dw;
          }
          if (bias < 0) {
            bias = 0;
          }
          if (bias > 1) {
            bias = 1;
          }
          String biasValue = null;
          if ((int)(bias * 1000) != 500) {
            bias = (int)(bias * 1000) / 1000f;
            biasValue = String.valueOf(bias);
            if (biasValue.equalsIgnoreCase("NaN")) {
              biasValue = null;
            }
          }
          attributes.setAttribute(SdkConstants.SHERPA_URI,
                                  SdkConstants.ATTR_LAYOUT_HORIZONTAL_BIAS, biasValue);
        }
      }
      else if (targetStartComponent != null) {
        int dx = x - getLeftTargetOrigin(targetStartComponent);
        if (useStartEnd) {
          if (isInRTL) {
            dx = getStartTargetOrigin(targetStartComponent, true) - (x + myComponent.getDrawWidth());
          }
          else {
            dx = x - getStartTargetOrigin(targetStartComponent, false);
          }
        }
        applyMargin(attributes, targetStartMargin, dx);
      }
      else if (targetEndComponent != null) {
        int dx = getRightTargetOrigin(targetEndComponent) - (x + myComponent.getDrawWidth());
        if (useStartEnd) {
          if (isInRTL) {
            dx = x - getEndTargetOrigin(targetEndComponent, true);
          }
          else {
            dx = getEndTargetOrigin(targetEndComponent, false) - (x + myComponent.getDrawWidth());
          }
        }
        applyMargin(attributes, targetEndMargin, dx);
      }
      else {
        String positionX = String.format(SdkConstants.VALUE_N_DP, x - parent.getDrawX());
        attributes.setAttribute(SdkConstants.TOOLS_URI, SdkConstants.ATTR_LAYOUT_EDITOR_ABSOLUTE_X, positionX);
      }

      SceneComponent targetTopComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourTopAttributes);
      SceneComponent targetBottomComponent = getTargetComponent(parent, ConstraintComponentUtilities.ourBottomAttributes);
      if (targetTopComponent != null && targetBottomComponent != null) {
        if (!myChainChecker.isInVerticalChain()) {
          int dy1 = getTopTargetOrigin(targetTopComponent) + getMarginValue(SdkConstants.ATTR_LAYOUT_MARGIN_TOP);
          int dy2 = getBottomTargetOrigin(targetBottomComponent) - getMarginValue(SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM);
          float dh = dy2 - dy1 - myComponent.getDrawHeight();
          float bias = (y - dy1) / dh;
          if (bias < 0) {
            bias = 0;
          }
          if (bias > 1) {
            bias = 1;
          }
          String biasValue = null;
          if ((int)(bias * 1000) != 500) {
            bias = (int)(bias * 1000) / 1000f;
            biasValue = String.valueOf(bias);
            if (biasValue.equalsIgnoreCase("NaN")) {
              biasValue = null;
            }
          }
          attributes.setAttribute(SdkConstants.SHERPA_URI,
                                  SdkConstants.ATTR_LAYOUT_VERTICAL_BIAS, biasValue);
        }
      }
      else if (targetTopComponent != null) {
        int dy = y - getTopTargetOrigin(targetTopComponent);
        applyMargin(attributes, SdkConstants.ATTR_LAYOUT_MARGIN_TOP, dy);
      }
      else if (targetBottomComponent != null) {
        int dy = getBottomTargetOrigin(targetBottomComponent) - (y + myComponent.getDrawHeight());
        applyMargin(attributes, SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM, dy);
      }
      else {
        String positionY = String.format(SdkConstants.VALUE_N_DP, y - parent.getDrawY());
        attributes.setAttribute(SdkConstants.TOOLS_URI, SdkConstants.ATTR_LAYOUT_EDITOR_ABSOLUTE_Y, positionY);
      }
      ConstraintComponentUtilities.cleanup(attributes, myComponent.getNlComponent());
    }

    @Nullable
    private SceneComponent getTargetComponent(@NotNull SceneComponent parent, @NotNull ArrayList<String> attributes) {
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      String target;
      int count = attributes.size();
      for (int i = 0; i < count; i++) {
        target = nlComponent.getAttribute(SdkConstants.SHERPA_URI, attributes.get(i));
        if (target != null) {
          if (target.equalsIgnoreCase(SdkConstants.ATTR_PARENT)) {
            return parent;
          }
          target = NlComponent.extractId(target);
          if (target != null) {
            return myComponent.getScene().getSceneComponent(target);
          }
        }
      }
      return null;
    }

    private void applyMargin(NlAttributesHolder attributes, String attribute, @AndroidDpCoordinate int currentValue) {
      currentValue = Math.max(0, currentValue);
      currentValue = currentValue / ourSnapMarginDistance * ourSnapMarginDistance; // snap
      String marginString = myComponent.getAuthoritativeNlComponent().getLiveAttribute(SdkConstants.ANDROID_URI, attribute);
      int marginValue = -1;
      if (marginString != null) {
        marginValue = getMarginValue(attribute);
      }
      if (currentValue == 0) {
        attributes.removeAttribute(SdkConstants.ANDROID_URI, attribute);
        if (SdkConstants.ATTR_LAYOUT_MARGIN_END.equals(attribute)) {
          attributes.removeAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT);
        }
        else if (SdkConstants.ATTR_LAYOUT_MARGIN_START.equals(attribute)) {
          attributes.removeAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_LEFT);
        }
      }
      else if (marginValue != -1 && marginValue == currentValue) {
        attributes.setAttribute(SdkConstants.ANDROID_URI, attribute, marginString);
        if (SdkConstants.ATTR_LAYOUT_MARGIN_END.equals(attribute)) {
          attributes.setAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT, marginString);
        }
        else if (SdkConstants.ATTR_LAYOUT_MARGIN_START.equals(attribute)) {
          attributes.setAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_LEFT, marginString);
        }
      }
      else {
        String marginY = String.format(SdkConstants.VALUE_N_DP, currentValue);
        attributes.setAttribute(SdkConstants.ANDROID_URI, attribute, marginY);
        if (SdkConstants.ATTR_LAYOUT_MARGIN_END.equals(attribute)) {
          attributes.setAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT, marginY);
        }
        else if (SdkConstants.ATTR_LAYOUT_MARGIN_START.equals(attribute)) {
          attributes.setAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_LEFT, marginY);
        }
      }
    }

    private int getStartTargetOrigin(@NotNull SceneComponent target, boolean isInRtl) {
      int origin = target.getDrawX();
      if (isInRtl) {
        origin += target.getDrawWidth();
      }
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_START_TO_END_OF) != null) {
        if (isInRtl) {
          origin = target.getDrawX();
        }
        else {
          origin += target.getDrawWidth();
        }
      }
      return origin;
    }

    private int getEndTargetOrigin(@NotNull SceneComponent target, boolean isInRtl) {
      int origin = target.getDrawX();
      if (isInRtl) {
        origin += target.getDrawWidth();
      }
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_END_TO_END_OF) != null) {
        if (isInRtl) {
          origin = target.getDrawX();
        }
        else {
          origin += target.getDrawWidth();
        }
      }
      return origin;
    }

    private int getLeftTargetOrigin(@NotNull SceneComponent target) {
      int origin = target.getDrawX();
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_LEFT_TO_RIGHT_OF) != null) {
        origin += target.getDrawWidth();
      }
      return origin;
    }

    private int getRightTargetOrigin(@NotNull SceneComponent target) {
      int origin = target.getDrawX();
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_RIGHT_TO_RIGHT_OF) != null) {
        origin += target.getDrawWidth();
      }
      return origin;
    }

    protected int getTopTargetOrigin(@NotNull SceneComponent target) {
      int origin = target.getDrawY();
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_TOP_TO_BOTTOM_OF) != null) {
        origin += target.getDrawHeight();
      }
      return origin;
    }

    private int getBottomTargetOrigin(@NotNull SceneComponent target) {
      int origin = target.getDrawY();
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      if (nlComponent.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_BOTTOM_TO_BOTTOM_OF) != null) {
        origin += target.getDrawHeight();
      }
      return origin;
    }

    private int getMarginValue(String attribute) {
      // TODO handles RTL + margin
      NlComponent nlComponent = myComponent.getAuthoritativeNlComponent();
      return ConstraintComponentUtilities.getDpValue(nlComponent, nlComponent.getAttribute(SdkConstants.ANDROID_URI, attribute));
    }
  }
}
