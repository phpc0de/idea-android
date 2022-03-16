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
package com.android.tools.idea.sdk.install.patch;

import com.android.io.CancellableFileIo;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.ProgressIndicator;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.lang.UrlClassLoader;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Studio side of the integration between studio and the IJ patcher.
 */
public class PatchRunner {
  /**
   * Name of the patcher jar file from the patcher package.
   */
  private static final String PATCHER_JAR_FN = "patcher.jar";

  /**
   * Runner class we'll invoke from the jar.
   */
  private static final String RUNNER_CLASS_NAME = "com.intellij.updater.Runner";

  /**
   * Interface from the patcher jar for the UI. Needed to get the update method.
   */
  private static final String UPDATER_UI_CLASS_NAME = "com.intellij.updater.UpdaterUI";

  /**
   * Interface for the actual UI class we'll create.
   */
  private static final String REPO_UI_CLASS_NAME = "com.android.tools.idea.sdk.updater.RepoUpdaterUI";

  /**
   * Class that can generate patches that this patcher can read.
   */
  private static final String PATCH_GENERATOR_CLASS_NAME = "com.android.tools.idea.sdk.updater.PatchGenerator";

  private final Class<?> myRunnerClass;
  private final Class<?> myUiBaseClass;
  private final Class<?> myUiClass;
  private final Class<?> myGeneratorClass;
  private final Path myPatcherJar;

  /**
   * Cache of patcher classes. Key is jar file, subkey is class name.
   */
  private static final Map<LocalPackage, PatchRunner> ourCache = CollectionFactory.createWeakMap();

  /**
   * Run the IJ patcher by reflection.
   */
  public boolean run(@NotNull Path destination, @NotNull Path patchFile, @NotNull ProgressIndicator progress)
    throws RestartRequiredException {
    Object ui;
    try {
      ui = myUiClass.getDeclaredConstructor(Component.class, ProgressIndicator.class).newInstance(null, progress);
    }
    catch (ReflectiveOperationException e) {
      progress.logWarning("Failed to create updater UI!", e);
      return false;
    }

    Method initLogger;
    try {
      initLogger = myRunnerClass.getMethod("initLogger");
      initLogger.invoke(null);
    }
    catch (ReflectiveOperationException e) {
      progress.logWarning("Failed to initialize logger!", e);
      return false;
    }

    Method doInstall;
    try {
      doInstall = myRunnerClass.getMethod("doInstall", String.class, myUiBaseClass, String.class);
    }
    catch (Throwable e) {
      progress.logWarning("Failed to find main method in runner!", e);
      return false;
    }

    try {
      progress.logInfo("Running patcher...");
      if (!(Boolean)doInstall.invoke(null, patchFile.toString(), ui, destination.toString())) {
        progress.logWarning("Failed to apply patch");
        return false;
      }
      progress.logInfo("Patch applied.");
    }
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof RestartRequiredException) {
        throw (RestartRequiredException)e.getTargetException();
      }
      progress.logWarning("Failed to run patcher", e);
      return false;
    }
    catch (Throwable e) {
      progress.logWarning("Failed to run patcher", e);
      return false;
    }

    return true;

  }

  /**
   * Generate a patch.
   *
   * @param existingRoot        The "From" (the original state of the files before the patch is applied.
   * @param newRoot             The "To" (the new state after the patch is applied).
   * @param existingDescription A user-friendly description of the existing package.
   * @param newDescription      A user-friendly description of the new package.
   * @param destination         The patch file to generate.
   * @return {@code true} if the generation succeeded.
   */
  public boolean generatePatch(@Nullable File existingRoot,
                               @NotNull File newRoot,
                               @Nullable String existingDescription,
                               @NotNull String newDescription,
                               @NotNull File destination,
                               @NotNull ProgressIndicator progress) {
    try {
      Method generateMethod = myGeneratorClass.getMethod("generateFullPackage", File.class, File.class, File.class, String.class,
                                                         String.class, ProgressIndicator.class);
      return (Boolean)generateMethod.invoke(null, newRoot, existingRoot, destination, existingDescription, newDescription, progress);
    }
    catch (NoSuchMethodException e) {
      progress.logWarning("Patcher doesn't support full package generation!");
      return false;
    }
    catch (InvocationTargetException e) {
      Throwable reason = e.getTargetException();
      progress.logWarning("Patch invocation failed! ", reason);
      return false;
    }
    catch (IllegalAccessException e) {
      progress.logWarning("Patch generation failed!");
      return false;
    }
  }

  @Nullable
  private static Path getPatcherFile(@Nullable LocalPackage patcherPackage) {
    Path patcherFile = patcherPackage == null ? null : patcherPackage.getLocation().resolve(PATCHER_JAR_FN);
    return patcherFile != null && CancellableFileIo.exists(patcherFile) ? patcherFile : null;
  }

  @VisibleForTesting
  PatchRunner(@NotNull Path jarFile,
              @NotNull Class<?> runnerClass,
              @NotNull Class<?> uiBaseClass,
              @NotNull Class<?> uiClass,
              @NotNull Class<?> generatorClass) {
    myPatcherJar = jarFile;
    myRunnerClass = runnerClass;
    myUiBaseClass = uiBaseClass;
    myUiClass = uiClass;
    myGeneratorClass = generatorClass;
  }

  /**
   * Gets a class loader for the given jar.
   */
  private static @NotNull ClassLoader getClassLoader(@NotNull Path patcherJar) {
    return UrlClassLoader.build().files(List.of(patcherJar)).allowLock(false).parent(PatchInstaller.class.getClassLoader()).get();
  }

  @NotNull
  public Path getPatcherJar() {
    return myPatcherJar;
  }

  public static class RestartRequiredException extends RuntimeException {
  }

  public interface Factory {
    @Nullable
    PatchRunner getPatchRunner(@NotNull LocalPackage runnerPackage, @NotNull ProgressIndicator progress);
  }

  public static class DefaultFactory implements Factory {
    @Override
    @Nullable
    public PatchRunner getPatchRunner(@NotNull LocalPackage runnerPackage, @NotNull ProgressIndicator progress) {
      PatchRunner result = ourCache.get(runnerPackage);
      if (result != null) {
        return result;
      }
      try {
        Path patcherFile = getPatcherFile(runnerPackage);
        if (patcherFile == null) {
          progress.logWarning("Failed to find patcher JAR!");
          return null;
        }
        ClassLoader loader = getClassLoader(patcherFile);
        Class<?> runnerClass = Class.forName(RUNNER_CLASS_NAME, true, loader);
        Class<?> uiBaseClass = Class.forName(UPDATER_UI_CLASS_NAME, true, loader);
        Class<?> uiClass = Class.forName(REPO_UI_CLASS_NAME, true, loader);
        Class<?> generatorClass = Class.forName(PATCH_GENERATOR_CLASS_NAME, true, loader);

        result = new PatchRunner(patcherFile, runnerClass, uiBaseClass, uiClass, generatorClass);
        ourCache.put(runnerPackage, result);
        return result;
      }
      catch (ClassNotFoundException e) {
        progress.logWarning("Failed to load patcher classes!");
        return null;
      }
    }
  }
}
