/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.profilers.memory.adapters.classifiers;

import com.android.tools.profiler.proto.Memory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Native callstack {@link ClassifierSet} that represents a heapprofd call graph node. End nodes return a {@link NativeAllocationMethodSet}
 * as a leaf.
 */
public class NativeCallStackSet extends ClassifierSet {
  private final int myCallstackDepth;
  private final Memory.AllocationStack.StackFrame myStackFrame;

  @NotNull
  public static Classifier createDefaultClassifier() {
    return new NativeFunctionClassifier(0);
  }

  public NativeCallStackSet(@NotNull Memory.AllocationStack.StackFrame stackFrame, int callstackDepth) {
    super(stackFrame.getMethodName());
    myCallstackDepth = callstackDepth;
    myStackFrame = stackFrame;
  }

  public String getFileName() {
    return myStackFrame.getFileName();
  }

  public int getLineNumber() {
    return myStackFrame.getLineNumber();
  }

  @NotNull
  public String getModuleName() { return myStackFrame.getModuleName(); }

  @NotNull
  @Override
  public Classifier createSubClassifier() {
    return new NativeFunctionClassifier(myCallstackDepth);
  }
}
