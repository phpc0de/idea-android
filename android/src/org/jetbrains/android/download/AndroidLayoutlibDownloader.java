// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android.download;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.download.FileDownloader;
import java.io.File;
import org.jetbrains.android.sdk.StudioEmbeddedRenderTarget;
import org.jetbrains.annotations.NotNull;

public class AndroidLayoutlibDownloader extends AndroidComponentDownloader {

  private static final Logger LOG = Logger.getInstance(AndroidLayoutlibDownloader.class);

  private static class Holder {
    private static final AndroidLayoutlibDownloader INSTANCE = new AndroidLayoutlibDownloader();
  }

  private AndroidLayoutlibDownloader() {
    // singleton. Use "getInstance"
  }

  @Override
  protected boolean doDownload(File pluginDir, FileDownloader downloader) {
    boolean res = super.doDownload(pluginDir, downloader);
    if (res) {
      StudioEmbeddedRenderTarget.resetInstance();
    }
    return res;
  }

  @NotNull
  @Override
  protected String getArtifactName() {
    return "layoutlib-resources";
  }

  @NotNull
  @Override
  protected String getExtension() {
    return "jar";
  }

  public static AndroidLayoutlibDownloader getInstance() {
    return AndroidLayoutlibDownloader.Holder.INSTANCE;
  }

  @Override
  protected @NotNull String getVersion() {
    LOG.assertTrue(super.getVersion().startsWith("27.3.0."), "Obsolete version override.");
    return "27.3.0.1";
  }
}
