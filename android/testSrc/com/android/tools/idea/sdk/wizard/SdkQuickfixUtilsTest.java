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
package com.android.tools.idea.sdk.wizard;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.android.repository.api.LocalPackage;
import com.android.repository.api.RepoManager;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.testframework.FakePackage;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.repository.testframework.FakeRepoManager;
import com.android.repository.testframework.MockFileOp;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.android.tools.idea.wizard.model.ModelWizardDialog;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.util.Disposer;
import java.util.Collections;

/**
 * Tests for {@link SdkQuickfixUtils}.
 */
public class SdkQuickfixUtilsTest extends AndroidGradleTestCase {
  RepoManager myRepoManager;
  AndroidSdkHandler mySdkHandler;
  MockFileOp fileOp = new MockFileOp();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    initMocks(this.getClass());

    RepositoryPackages packages = new RepositoryPackages();
    String sdkPath = "/sdk";
    String avdPath = "/avd";
    myRepoManager = spy(new FakeRepoManager(fileOp.toPath(sdkPath), packages));
    mySdkHandler = new AndroidSdkHandler(fileOp.toPath(sdkPath), fileOp.toPath(avdPath), fileOp, myRepoManager);
    assertNotNull(mySdkHandler);
    FakeProgressIndicator progress = new FakeProgressIndicator();
    assertSame(myRepoManager, mySdkHandler.getSdkManager(progress));
  }

  public void testCreateDialogForPathsNoOpMessageNull() {
    ModelWizardDialog dialog;
    dialog = SdkQuickfixUtils.createDialogForPaths(null, Collections.emptyList(), null);
    assertNull(dialog);
  }

  public void testCreateDialogForPathsNoOpMessage() {
    String errorMessage = "This is an error from testCreateDialogNoOpMessage";
    // createDialogForPaths will cause a RuntimeException instead of creating a dialog when it is headless or in testing mode
    // Check for that exception instead of trying to see if showErrorDialog was called
    boolean causedException = false;
    try {
      ModelWizardDialog dialog = SdkQuickfixUtils.createDialogForPaths(null, Collections.emptyList(), errorMessage);
      Disposer.register(getTestRootDisposable(), dialog.getDisposable());
    }
    catch (RuntimeException error) {
      causedException = true;
      assertThat(error.getMessage()).isEqualTo(errorMessage);
    }
    assertThat(causedException).isTrue();
  }

  public void testCreateDialogNoRepoReloadsWhenUninstallsOnly() {
    LocalPackage localPackage = new FakePackage.FakeLocalPackage("some;sdk;package", fileOp);

    ModelWizardDialog dialog = SdkQuickfixUtils.createDialog(null, null, null,
                                                             Collections.emptyList(), ImmutableList.of(localPackage), mySdkHandler,
                                                             null, false);
    assertNotNull(dialog);
    Disposer.register(getTestRootDisposable(), dialog.getDisposable());
    // We're fine with non-zero cache expiration values, as those inherently optimize the redundant downloads.
    // One such call is currently made from the wizard, which starts SDK installation once built - we will accept that.
    verify(myRepoManager, never()).loadSynchronously(eq(0), any(), any(), any(), any(), any(), any());
  }

  public void testCreateDialogNoUncachedRepoReloads() {
    LocalPackage localPackage = new FakePackage.FakeLocalPackage("some;sdk;package", fileOp);
    try {
      SdkQuickfixUtils.createDialog(null, null, ImmutableList.of("some;other;package"),
                                    null, ImmutableList.of(localPackage), mySdkHandler,
                                    null, false);
    }
    catch (RuntimeException e) {
      // Expected RuntimeException when creating the dialog in unit test mode.
      assertThat(e.getMessage()).contains("All packages are not available for download!");
    }

    verify(myRepoManager, never()).loadSynchronously(eq(0), any(), any(), any(), any(), any(), any());
    verify(myRepoManager, times(1)).loadSynchronously(eq(RepoManager.DEFAULT_EXPIRATION_PERIOD_MS), any(), any(), any(), any(),
                                         any(), any());
  }
}
