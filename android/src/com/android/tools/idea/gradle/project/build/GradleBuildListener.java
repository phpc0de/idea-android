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
package com.android.tools.idea.gradle.project.build;

import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GradleBuildListener {
  void buildExecutorCreated(@NotNull GradleBuildInvoker.Request request);

  void buildStarted(@NotNull BuildContext context);

  void buildFinished(@NotNull BuildStatus status, @Nullable BuildContext context);

  abstract class Adapter implements GradleBuildListener {
    @Override
    public void buildExecutorCreated(@NotNull GradleBuildInvoker.Request request) {
    }

    @Override
    public void buildStarted(@NotNull BuildContext context) {
    }

    @Override
    public void buildFinished(@NotNull BuildStatus status, @Nullable BuildContext context) {
    }
  }
}
