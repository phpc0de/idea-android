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
package com.android.tools.idea.experimental.codeanalysis.datastructs.graph.node;

import com.android.tools.idea.experimental.codeanalysis.datastructs.graph.BlockGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IfBranchingNode extends ConditionCheckNode {

  /**
   * Get the CFG for the true branch
   * @return The CFG for the true branch
   */
  @NotNull
  BlockGraph getThenBranchCFG();

  /**
   * Get the CFG for the else branch. Will be null if
   * "else" does not exist.
   * @return The CFG for the else branch
   */
  @Nullable
  BlockGraph getElseBranchCFG();

  /**
   * Check if this if has "else" branch
   * @return Return true if this IF has "else"
   */
  boolean hasElse();
}
