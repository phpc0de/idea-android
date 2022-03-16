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
package com.android.tools.idea.lint.common;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public class AndroidQuickfixContexts {
  public static abstract class Context {
    private final ContextType myType;

    private Context(@NotNull ContextType type) {
      myType = type;
    }

    @NotNull
    public ContextType getType() {
      return myType;
    }
  }

  public static class ContextType {
    private ContextType() {
    }
  }

  public static class BatchContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private static final BatchContext INSTANCE = new BatchContext();

    private BatchContext() {
      super(TYPE);
    }

    @NotNull
    public static BatchContext getInstance() {
      return INSTANCE;
    }
  }

  public static class EditorContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private final Editor myEditor;

    private EditorContext(@NotNull Editor editor) {
      super(TYPE);
      myEditor = editor;
    }

    @NotNull
    public Editor getEditor() {
      return myEditor;
    }

    @NotNull
    public static EditorContext getInstance(@NotNull Editor editor) {
      return new EditorContext(editor);
    }
  }

  public static class DesignerContext extends Context {
    public static final ContextType TYPE = new ContextType();
    private static final DesignerContext INSTANCE = new DesignerContext();

    private DesignerContext() {
      super(TYPE);
    }

    @NotNull
    public static DesignerContext getInstance() {
      return INSTANCE;
    }
  }
}
