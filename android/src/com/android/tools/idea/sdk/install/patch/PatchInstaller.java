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

import com.android.repository.api.Downloader;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.PackageOperation;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoManager;
import com.android.repository.impl.installer.AbstractInstaller;
import com.android.repository.impl.meta.Archive;
import com.android.repository.util.InstallerUtil;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Installer for binary diff packages, as built by {@code com.intellij.updater.Runner}.
 */
class PatchInstaller extends AbstractInstaller implements PatchOperation {

  private static final String PATCH_JAR_FN = "patch.jar";
  private final LocalPackage myExisting;
  private Path myPatchFile;

  public PatchInstaller(@Nullable LocalPackage existing,
                        @NotNull RemotePackage p,
                        @NotNull Downloader downloader,
                        @NotNull RepoManager mgr) {
    super(p, mgr, downloader);
    myExisting = existing;
  }

  @Override
  protected boolean doComplete(@Nullable Path installTemp,
                               @NotNull ProgressIndicator progress) {
    if (myPatchFile == null) {
      myPatchFile = installTemp.resolve(PATCH_JAR_FN);
    }
    return PatchInstallerUtil.installPatch(this, myPatchFile, progress);
  }

  @Override
  protected boolean doPrepare(@NotNull Path tempDir,
                              @NotNull ProgressIndicator progress) {
    LocalPackage local = getRepoManager().getPackages().getLocalPackages().get(getPackage().getPath());
    Archive archive = getPackage().getArchive();
    assert archive != null;

    Archive.PatchType patch = archive.getPatch(local.getVersion());
    assert patch != null;

    myPatchFile = downloadPatchFile(patch, tempDir, progress);
    if (myPatchFile == null) {
      progress.logWarning("Patch failed to download.");
      return false;
    }
    return true;
  }

  @Nullable
  @Override
  public LocalPackage getExisting() {
    return myExisting;
  }

  @Nullable
  @Override
  public LocalPackage getPatcher(@NotNull ProgressIndicator progress) {
    LocalPackage dependantPatcher = PatchInstallerUtil.getDependantPatcher(getPackage(), getRepoManager());
    if (dependantPatcher == null) {
      dependantPatcher = tryToCompletePatcherInstall(progress);
    }
    if (dependantPatcher == null) {
      progress.logWarning("Failed to find SDK Patch Applier!");
    }
    return dependantPatcher;
  }

  @Nullable
  private LocalPackage tryToCompletePatcherInstall(@NotNull ProgressIndicator progress) {
    PackageOperation op = PatchInstallerUtil.getInProgressDependantPatcherInstall(getPackage(), getRepoManager());
    if (op != null && op.getInstallStatus() == InstallStatus.PREPARED) {
      // It's ready to be installed, but not complete yet. We have to finish it now so we can use it.
      op.complete(progress.createSubProgress(0.9));
      progress.setFraction(0.9);
    }

    // Maybe it completed already, but we haven't reloaded the local SDK. Do so now.
    getRepoManager().reloadLocalIfNeeded(progress.createSubProgress(1));
    progress.setFraction(1);
    return PatchInstallerUtil.getDependantPatcher(getPackage(), getRepoManager());
  }

  @NotNull
  @Override
  public Path getNewFilesRoot() {
    // PatchInstaller doesn't need to generate a patch on the fly, so it doesn't have or need this information.
    throw new UnsupportedOperationException("PatchInstaller can't generate patches");
  }

  @NotNull
  @Override
  public String getNewVersionName() {
    return getPackage().getDisplayName() + " Version " + getPackage().getVersion();
  }

  /**
   * Resolves and downloads the patch for the given {@link Archive.PatchType}.
   *
   * @return {@code null} if unsuccessful.
   */
  @Nullable
  private Path downloadPatchFile(@NotNull Archive.PatchType patch,
                                 @NotNull Path tempDir,
                                 @NotNull ProgressIndicator progress) {
    URL url = InstallerUtil.resolveUrl(patch.getUrl(), getPackage(), progress);
    if (url == null) {
      progress.logWarning("Failed to resolve URL: " + patch.getUrl());
      return null;
    }
    try {
      Path patchFile = tempDir.resolve(PATCH_JAR_FN);
      getDownloader().downloadFullyWithCaching(url, patchFile, patch.getTypedChecksum(), progress);
      return patchFile;
    }
    catch (IOException e) {
      progress.logWarning("Error during downloading", e);
      return null;
    }
  }
}
