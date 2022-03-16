/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.dependencies.treeview;

import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings;
import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractBaseTreeStructure;
import com.android.tools.idea.gradle.structure.model.PsModule;
import org.jetbrains.annotations.NotNull;

class ResolvedDependenciesTreeStructure extends AbstractBaseTreeStructure {
  @NotNull private final ResolvedDependenciesTreeRootNode myRootNode;

  ResolvedDependenciesTreeStructure(@NotNull PsModule module,
                                    @NotNull PsUISettings uiSettings) {
    myRootNode = new ResolvedDependenciesTreeRootNode(module, uiSettings);
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRootNode;
  }

  void reset() {
    myRootNode.reset();
  }
}
