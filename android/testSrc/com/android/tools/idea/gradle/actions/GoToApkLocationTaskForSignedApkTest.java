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
package com.android.tools.idea.gradle.actions;

import static com.intellij.notification.NotificationType.INFORMATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.tools.idea.gradle.project.build.invoker.GradleInvocationResult;
import com.android.tools.idea.project.AndroidNotification;
import com.android.tools.idea.testing.IdeComponents;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.PlatformTestCase;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.gradle.tooling.BuildCancelledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link GoToApkLocationTask}.
 */
public class GoToApkLocationTaskForSignedApkTest extends PlatformTestCase {
  private static final String NOTIFICATION_TITLE = "Build APK";
  @Mock private AndroidNotification myMockNotification;
  private GoToApkLocationTask myTask;
  private boolean isRevealFileActionSupported;
  private SortedMap<String, File> buildsToPaths;
  private static final String buildVariant1 = "FreeDebug";
  private static final String buildVariant2 = "PaidDebug";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.initMocks(this);
    isRevealFileActionSupported = true;
    List<Module> modules = Collections.singletonList(getModule());
    List<String> buildVariants = new ArrayList<>();
    buildVariants.add(buildVariant1);
    buildVariants.add(buildVariant2);
    File myApkPath1 = createTempDir(buildVariant1 + "apkLocation");
    File myApkPath2 = createTempDir(buildVariant2 + "apkLocation");
    // Simulate the paths of the APK for the module with one or more build variants.
    buildsToPaths = new TreeMap<>();
    buildsToPaths.put(buildVariant1, myApkPath1);
    buildsToPaths.put(buildVariant2, myApkPath2);
    IdeComponents ideComponents = new IdeComponents(getProject());
    BuildsToPathsMapper mockGenerator = ideComponents.mockProjectService(BuildsToPathsMapper.class);
    when(mockGenerator.getBuildsToPaths(any(), any(), any(), anyBoolean(), anyString())).thenReturn(buildsToPaths);
    myTask = new GoToApkLocationTask(getProject(), modules, NOTIFICATION_TITLE, buildVariants, "") {
      @Override
      boolean isRevealFileActionSupported() {
        return isRevealFileActionSupported;  // Inject ability to simulate both behaviors.
      }
    };
    ideComponents.replaceProjectService(AndroidNotification.class, myMockNotification);
  }

  public void testExecuteWithCancelledBuild() {
    String message = "Build cancelled.";
    GradleInvocationResult result = createBuildResult(new BuildCancelledException(message));
    myTask.execute(result);
    verify(myMockNotification).showBalloon(NOTIFICATION_TITLE, message, INFORMATION);
  }

  public void testExecuteWithFailedBuild() {
    String message = "Errors while building APK. You can find the errors in the 'Messages' view.";
    myTask.execute(createBuildResult(new Throwable("Unknown error with gradle build")));
    verify(myMockNotification).showBalloon(NOTIFICATION_TITLE, message, NotificationType.ERROR);
  }

  public void testExecuteWithSuccessfulBuild() {
    Module module = getModule();
    myTask.execute(createBuildResult(null /* build successful - no errors */));
    String moduleName = module.getName();
    String message = getExpectedModuleNotificationMessage(moduleName, buildVariant1, buildVariant2);
    verify(myMockNotification).showBalloon(NOTIFICATION_TITLE, message, INFORMATION,
                                           new GoToApkLocationTask.OpenFolderNotificationListener(buildsToPaths, myProject));
  }

  public void testExecuteWithSuccessfulBuildNoRevealFileAction() {
    isRevealFileActionSupported = false;
    myTask.execute(createBuildResult(null /* build successful - no errors */));
    String message = getExpectedModuleNotificationMessageNoRevealFileAction(getModule().getName());
    verify(myMockNotification).showBalloon(NOTIFICATION_TITLE, message, INFORMATION,
                                           new GoToApkLocationTask.OpenEventLogHyperlink());
  }

  @NotNull
  private static String getExpectedModuleNotificationMessage(@NotNull String moduleName,
                                                             @Nullable String buildVariant1Name,
                                                             @Nullable String buildVariant2Name) {
    return "APK(s) generated successfully for module '" +
           moduleName +
           "' with 2 build variants:" +
           getExpectedModuleLineNotificationMessage(buildVariant1Name) +
           getExpectedModuleLineNotificationMessage(buildVariant2Name);
  }

  @NotNull
  private static String getExpectedModuleLineNotificationMessage(@NotNull String buildVariantName) {
    return "<br/>Build variant '" +
           buildVariantName +
           "': <a href=\"" +
           GoToApkLocationTask.MODULE +
           buildVariantName +
           "\">locate</a> or <a href=\"" +
           GoToApkLocationTask.ANALYZE +
           buildVariantName +
           "\">analyze</a> the APK.";
  }

  @NotNull
  private static String getExpectedModuleNotificationMessageNoRevealFileAction(@NotNull String moduleName) {
    return "APK(s) generated successfully for module '" + moduleName + "' with 2 build variants";
  }

  @NotNull
  private static GradleInvocationResult createBuildResult(@Nullable Throwable buildError) {
    return new GradleInvocationResult(Collections.emptyList(), Collections.emptyList(), buildError);
  }
}
