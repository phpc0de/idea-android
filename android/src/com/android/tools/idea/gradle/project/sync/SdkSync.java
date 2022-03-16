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
package com.android.tools.idea.gradle.project.sync;

import static com.android.tools.idea.sdk.SdkPaths.validateAndroidNdk;
import static com.android.tools.idea.sdk.SdkPaths.validateAndroidSdk;
import static com.intellij.openapi.util.io.FileUtil.filesEqual;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;

import com.android.tools.idea.IdeInfo;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.util.LocalProperties;
import com.android.tools.idea.sdk.IdeSdks;
import com.android.tools.idea.sdk.SdkMerger;
import com.android.tools.idea.sdk.SdkPaths.ValidationResult;
import com.android.tools.idea.sdk.SelectSdkDialog;
import com.android.tools.idea.ui.GuiTestingService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ModalityUiUtil;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SdkSync {
  private static final String ERROR_DIALOG_TITLE = "Sync Android SDKs";

  @NotNull
  public static SdkSync getInstance() {
    return ApplicationManager.getApplication().getService(SdkSync.class);
  }

  public void syncIdeAndProjectAndroidSdks(@NotNull LocalProperties localProperties) {
    syncIdeAndProjectAndroidSdk(localProperties, new FindValidSdkPathTask(), null);
    syncIdeAndProjectAndroidNdk(localProperties);
  }

  public void syncIdeAndProjectAndroidSdks(@NotNull Project project) throws IOException {
    LocalProperties localProperties = new LocalProperties(project);
    syncIdeAndProjectAndroidSdk(localProperties, new FindValidSdkPathTask(), project);
    syncIdeAndProjectAndroidNdk(localProperties);
  }

  public void syncIdeAndProjectAndroidNdk(@NotNull Project project) throws IOException {
    LocalProperties localProperties = new LocalProperties(project);
    syncIdeAndProjectAndroidNdk(localProperties);
  }

  @VisibleForTesting
  void syncIdeAndProjectAndroidSdk(@NotNull LocalProperties localProperties,
                                   @NotNull FindValidSdkPathTask findSdkPathTask,
                                   @Nullable Project project) {
    if (localProperties.hasAndroidDirProperty()) {
      // if android.dir is specified, we don't sync SDKs. User is working with SDK sources.
      return;
    }

    File ideAndroidSdkPath = IdeSdks.getInstance().getAndroidSdkPath();
    File projectAndroidSdkPath = localProperties.getAndroidSdkPath();

    if (ideAndroidSdkPath != null && projectAndroidSdkPath == null) {
      if (localProperties.getPropertiesFilePath().exists() || IdeInfo.getInstance().isAndroidStudio()) {
        // If we have the IDE default SDK and we don't have a project SDK, update local.properties with default SDK path and exit.
        // In IDEA we don't want local.properties to be created in plain java-gradle projects, so we update local.properties only if the
        // file exists
        setProjectSdk(localProperties, ideAndroidSdkPath);
      }
      return;
    }
    else if (ideAndroidSdkPath != null && projectAndroidSdkPath != null) {

      ValidationResult validationResult = validateAndroidSdk(projectAndroidSdkPath, true);
      if (!validationResult.success) {
        // If we have the IDE default SDK and we don't have a valid project SDK, update local.properties with default SDK path and exit.
        ApplicationManager.getApplication().invokeAndWait(() -> {
          if (!ApplicationManager.getApplication().isUnitTestMode()) {
            String error = validationResult.message;
            if (isEmpty(error)) {
              error = String.format("The path \n'%1$s'\n" + "does not refer to a valid Android SDK.", projectAndroidSdkPath.getPath());
            }
            String format =
              "%1$s\n\n%3$s will use this Android SDK instead:\n'%2$s'\nand will modify the project's local.properties file.";
            String errorMessage =
              String.format(format, error, ideAndroidSdkPath.getPath(), ApplicationNamesInfo.getInstance().getFullProductName());
            if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
              Messages.showErrorDialog(errorMessage, ERROR_DIALOG_TITLE);
            } else {
              Logger.getInstance(SdkSync.class).warn(errorMessage);
            }
          }
          setProjectSdk(localProperties, ideAndroidSdkPath);
        });
        return;
      }
    }
    else if (ideAndroidSdkPath == null && projectAndroidSdkPath == null) {
      if (IdeInfo.getInstance().isAndroidStudio()) {
        // We don't have any SDK (IDE or project.)
        // In IDEA there are non-android gradle projects. IDEA should not create local.properties file and should not ask users to configure
        // Android SDK unless we are sure that they are working with Android projects (e.g. local.properties file already exists)
        // Note that local.properties file does not imply Android (this can also be KMPP. See IDEA-265504)
        File selectedPath = findSdkPathTask.selectValidSdkPath();
        if (selectedPath == null) {
          throw new ExternalSystemException("Unable to continue until an Android SDK is specified");
        }
        setIdeSdk(localProperties, selectedPath);
      }
      return;
    } else {
      assert ideAndroidSdkPath == null && projectAndroidSdkPath != null;

      if (IdeSdks.getInstance().isValidAndroidSdkPath(projectAndroidSdkPath)) {
        // If we have a valid project SDK but we don't have IDE default SDK, update IDE with project SDK path and exit.
        setIdeSdk(localProperties, projectAndroidSdkPath);
        return;
      }
      else {
        // If we have an invalid project SDK and we don't have IDE default SDK, prompt the user
        File selectedPath = findSdkPathTask.selectValidSdkPath();
        if (selectedPath == null) {
          throw new ExternalSystemException("Unable to continue until an Android SDK is specified");
        }
        setIdeSdk(localProperties, selectedPath);
        return;
      }
    }

    if (!filesEqual(ideAndroidSdkPath, projectAndroidSdkPath)) {
      String msg = String.format("The project and %3$s point to different Android SDKs.\n\n" +
                                 "%3$s's default SDK is in:\n" +
                                 "%1$s\n\n" +
                                 "The project's SDK (specified in local.properties) is in:\n" +
                                 "%2$s\n\n" +
                                 "To keep results consistent between IDE and command line builds, only one path can be used. " +
                                 "Do you want to:\n\n" +
                                 "[1] Use %3$s's default SDK (modifies the project's local.properties file.)\n\n" +
                                 "[2] Use the project's SDK (modifies %3$s's default.)\n\n" +
                                 "Note that switching SDKs could cause compile errors if the selected SDK doesn't have the " +
                                 "necessary Android platforms or build tools.",
                                 ideAndroidSdkPath.getPath(), projectAndroidSdkPath.getPath(),
                                 ApplicationNamesInfo.getInstance().getFullProductName());
      ApplicationManager.getApplication().invokeAndWait(() -> {
        // We need to pass the project, so on Mac, the "Mac sheet" showing this message shows inside the IDE during UI tests, otherwise
        // it will show outside and the UI testing infrastructure cannot see it. It is overall a good practice to pass the project when
        // showing a message, to ensure that the message shows in the IDE instance containing the project.
        boolean result = MessageDialogBuilder.yesNo("Android SDK Manager", msg)
          .yesText("Use " + ApplicationNamesInfo.getInstance().getFullProductName() + "'s SDK")
          .noText("Use Project's SDK")
          .ask(project);
        if (result) {
          // Use Android Studio's SDK
          setProjectSdk(localProperties, ideAndroidSdkPath);
        }
        else {
          // Use project's SDK
          setIdeSdk(localProperties, projectAndroidSdkPath);
        }
        if (GuiTestingService.getInstance().isGuiTestingMode() &&
            !GuiTestingService.getInstance().getGuiTestSuiteState().isSkipSdkMerge()) {
          mergeIfNeeded(projectAndroidSdkPath, ideAndroidSdkPath);
        }
      });
    }
  }

  private void syncIdeAndProjectAndroidNdk(@NotNull LocalProperties localProperties) {
    if (StudioFlags.NDK_SIDE_BY_SIDE_ENABLED.get()) {
      // When side-by-side NDK is enabled, don't force ndk.dir. Instead, the more
      // recent gradle plugin will decide what the correct NDK folder is.
      // If this is an older plugin that doesn't support side-by-side NDK then
      // there may be a sync error about missing NDK. This should be fixed up after
      // the sync failure with error handlers.
      return;
    }
    File projectAndroidNdkPath = localProperties.getAndroidNdkPath();
    File ideAndroidNdkPath = IdeSdks.getInstance().getAndroidNdkPath();

    if (projectAndroidNdkPath != null) {
      if (!validateAndroidNdk(projectAndroidNdkPath, false).success) {
        if (ideAndroidNdkPath != null) {
          Logger.getInstance(SdkSync.class).warn(String.format("Replacing invalid NDK path %1$s with %2$s",
                                                               projectAndroidNdkPath, ideAndroidNdkPath));
          setProjectNdk(localProperties, ideAndroidNdkPath);
          return;
        }
        Logger.getInstance(SdkSync.class).warn(String.format("Removing invalid NDK path: %s", projectAndroidNdkPath));
        setProjectNdk(localProperties, null);
      }
      return;
    }
    setProjectNdk(localProperties, ideAndroidNdkPath);
  }

  private static void setProjectNdk(@NotNull LocalProperties localProperties, @Nullable File ndkPath) {
    File currentNdkPath = localProperties.getAndroidNdkPath();
    if (filesEqual(currentNdkPath, ndkPath)) {
      return;
    }
    localProperties.setAndroidNdkPath(ndkPath);
    try {
      localProperties.save();
    }
    catch (IOException e) {
      String msg = String.format("Unable to save '%1$s'", localProperties.getPropertiesFilePath().getPath());
      throw new ExternalSystemException(msg, e);
    }
  }

  private void setIdeSdk(@NotNull LocalProperties localProperties, @NotNull File projectAndroidSdkPath) {
    // There is one case where DefaultSdks.setAndroidSdkPath will not update local.properties in the project. The conditions for this to
    // happen are:
    // 1. This is a fresh install of Android Studio and user does not set Android SDK
    // 2. User imports a project that does not have a local.properties file
    // Just to be on the safe side, we update local.properties.
    setProjectSdk(localProperties, projectAndroidSdkPath);

    ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
      IdeSdks.getInstance().setAndroidSdkPath(projectAndroidSdkPath, null);
    }));
  }

  private static void setProjectSdk(@NotNull LocalProperties localProperties, @NotNull File androidSdkPath) {
    if (Registry.is("android.sdk.local.properties.update.disabled")) {
      Logger.getInstance(SdkSync.class).warn("local.properties should be updated, but update is now disabled.");
      return;
    }
    if (filesEqual(localProperties.getAndroidSdkPath(), androidSdkPath)) {
      return;
    }
    localProperties.setAndroidSdkPath(androidSdkPath);
    try {
      localProperties.save();
    }
    catch (IOException e) {
      String msg = String.format("Unable to save '%1$s'", localProperties.getPropertiesFilePath().getPath());
      throw new ExternalSystemException(msg, e);
    }
  }

  private static void mergeIfNeeded(@NotNull File sourceSdk, @NotNull File destSdk) {
    if (SdkMerger.hasMergeableContent(sourceSdk, destSdk)) {
      String msg = String.format("The Android SDK at\n\n%1$s\n\nhas packages not in your project's SDK at\n\n%2$s\n\n" +
                                 "Would you like to copy into the project SDK?", sourceSdk.getPath(), destSdk.getPath());
      int result = MessageDialogBuilder.yesNo("Merge SDKs", msg).yesText("Copy").noText("Do not copy").show();
      if (result == Messages.YES) {
        new Task.Backgroundable(null, "Merging Android SDKs", false) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            SdkMerger.mergeSdks(sourceSdk, destSdk, indicator);
          }
        }.queue();
      }
    }
  }

  @VisibleForTesting
  static class FindValidSdkPathTask {
    @Nullable
    File selectValidSdkPath() {
      Ref<File> pathRef = new Ref<>();
      ApplicationManager.getApplication().invokeAndWait(() -> findValidSdkPath(pathRef));
      return pathRef.get();
    }

    private static void findValidSdkPath(@NotNull Ref<File> pathRef) {
      Sdk jdk = IdeSdks.getInstance().getJdk();
      String jdkPath = jdk != null ? jdk.getHomePath() : null;
      SelectSdkDialog dialog = new SelectSdkDialog(jdkPath, null);
      dialog.setModal(true);
      if (!dialog.showAndGet()) {
        String msg = "An Android SDK is needed to continue. Would you like to try again?";
        if (MessageDialogBuilder.yesNo(ERROR_DIALOG_TITLE, msg).show() == Messages.YES) {
          findValidSdkPath(pathRef);
        }
        return;
      }
      File path = new File(dialog.getAndroidHome());
      if (!IdeSdks.getInstance().isValidAndroidSdkPath(path)) {
        String format = "The path\n'%1$s'\ndoes not refer to a valid Android SDK. Would you like to try again?";
        if (MessageDialogBuilder.yesNo(ERROR_DIALOG_TITLE, String.format(format, path.getPath())).show() == Messages.YES) {
          findValidSdkPath(pathRef);
        }
        return;
      }
      pathRef.set(path);
    }
  }
}
