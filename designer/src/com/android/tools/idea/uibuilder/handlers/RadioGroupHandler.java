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
package com.android.tools.idea.uibuilder.handlers;

import com.android.xml.XmlBuilder;
import com.android.tools.idea.uibuilder.api.XmlType;
import com.android.tools.idea.uibuilder.handlers.linear.LinearLayoutHandler;
import com.android.tools.idea.common.model.NlComponent;
import com.google.common.collect.ImmutableList;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.android.SdkConstants.*;

/**
 * Handler for the {@code <RadioGroup>} layout
 */
@SuppressWarnings("unused") // Loaded by reflection
public class RadioGroupHandler extends LinearLayoutHandler {

  @Override
  @NotNull
  public List<String> getInspectorProperties() {
    return ImmutableList.of(ATTR_CHECKED_BUTTON);
  }

  @NotNull
  @Override
  public String getTitleAttributes(@NotNull NlComponent component) {
    if (!component.getTagName().equals(RADIO_GROUP)) {
      return super.getTitleAttributes(component);
    }
    return isVertical(component) ? "(vertical)" : "(horizontal)";
  }

  @Override
  public boolean isVertical(@NotNull NlComponent component) {
    // RadioGroup is vertical by default.
    return !VALUE_HORIZONTAL.equals(component.resolveAttribute(ANDROID_URI, ATTR_ORIENTATION));
  }

  @Override
  @Language("XML")
  @NotNull
  public String getXml(@NotNull String tagName, @NotNull XmlType xmlType) {
    // Use the default layout parameters for a horizontal RadioGroup:
    switch (xmlType) {
      case COMPONENT_CREATION:
        return new XmlBuilder()
          .startTag(tagName)
          .androidAttribute(ATTR_LAYOUT_WIDTH, VALUE_WRAP_CONTENT)
          .androidAttribute(ATTR_LAYOUT_HEIGHT, VALUE_WRAP_CONTENT)
          .endTag(tagName)
          .toString();
      default:
        return super.getXml(tagName, xmlType);
    }
  }
}
