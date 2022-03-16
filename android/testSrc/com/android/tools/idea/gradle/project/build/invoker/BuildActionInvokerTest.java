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
package com.android.tools.idea.gradle.project.build.invoker;

import com.android.tools.idea.gradle.util.BuildMode;
import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.intellij.openapi.util.Ref;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for making sure that {@link org.gradle.tooling.BuildAction} is run when passed to {@link GradleBuildInvoker}.
 */
public class BuildActionInvokerTest extends AndroidGradleTestCase {
  public void testBuildWithBuildAction() throws Exception {
    loadSimpleApplication();

    Ref<String> model = new Ref<>("");
    CountDownLatch latch = new CountDownLatch(1);

    GradleBuildInvoker invoker = GradleBuildInvoker.getInstance(getProject());
    invoker.add(result -> {
      Object resultModel = result.getModel();
      if (resultModel instanceof String) {
        model.set((String)resultModel);
      }
      latch.countDown();
    });

    ListMultimap<Path, String> tasks = ArrayListMultimap.create();
    tasks.put(new File(getProject().getBasePath()).toPath(), "assembleDebug");
    invoker.executeTasks(tasks, BuildMode.ASSEMBLE, Collections.emptyList(), new TestBuildAction());
    latch.await();

    assertEquals("test", model.get());
  }
}
