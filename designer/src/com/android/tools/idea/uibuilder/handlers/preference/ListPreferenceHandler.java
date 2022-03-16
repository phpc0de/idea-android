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
package com.android.tools.idea.uibuilder.handlers.preference;

import com.android.resources.ResourceType;
import com.android.tools.idea.common.api.InsertType;
import com.android.tools.idea.common.command.NlWriteCommandActionUtil;
import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.xml.XmlBuilder;
import com.android.tools.idea.uibuilder.api.XmlType;
import com.android.tools.idea.common.model.NlComponent;
import com.google.common.collect.ImmutableList;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

import static com.android.SdkConstants.ATTR_TITLE;
import static com.android.SdkConstants.PreferenceAttributes.*;
import static com.android.SdkConstants.PreferenceTags.LIST_PREFERENCE;

public final class ListPreferenceHandler extends PreferenceHandler {
  @Language("XML")
  @NotNull
  @Override
  public String getXml(@NotNull String tagName, @NotNull XmlType xmlType) {
    switch (xmlType) {
      case COMPONENT_CREATION:
        return new XmlBuilder()
          .startTag(tagName)
          .androidAttribute(ATTR_DEFAULT_VALUE, 1)
          .androidAttribute(ATTR_TITLE, "List preference")
          .endTag(tagName)
          .toString();
      case PREVIEW_ON_PALETTE:
      case DRAG_PREVIEW:
        return NO_PREVIEW;
      default:
        throw new AssertionError(xmlType);
    }
  }

  @Override
  @NotNull
  public List<String> getInspectorProperties() {
    return ImmutableList.of(
      ATTR_DEFAULT_VALUE,
      ATTR_ENTRIES,
      ATTR_ENTRY_VALUES,
      ATTR_KEY,
      ATTR_TITLE,
      ATTR_SUMMARY,
      ATTR_DEPENDENCY,
      ATTR_ICON,
      ATTR_DIALOG_ICON);
  }

  @Override
  public boolean onCreate(@NotNull ViewEditor editor,
                          @Nullable NlComponent parent,
                          @NotNull NlComponent newChild,
                          @NotNull InsertType type) {
    if (!super.onCreate(editor, parent, newChild, type)) {
      return false;
    }

    if (type.equals(InsertType.CREATE)) {
      EnumSet<ResourceType> array = EnumSet.of(ResourceType.ARRAY);
      String entries = editor.displayResourceInput("Choose entries Resource", array);

      if (entries == null) {
        return false;
      }

      String entryValues = editor.displayResourceInput("Choose entryValues Resource", array);

      if (entryValues == null) {
        return false;
      }

      NlWriteCommandActionUtil.run(newChild, "Set ListPreference", () -> {
        newChild.setAndroidAttribute(ATTR_ENTRIES, entries);
        newChild.setAndroidAttribute(ATTR_ENTRY_VALUES, entryValues);
        newChild.setAndroidAttribute(ATTR_KEY, generateKey(newChild, LIST_PREFERENCE, "list_preference_"));
      });
    }

    return true;
  }
}
