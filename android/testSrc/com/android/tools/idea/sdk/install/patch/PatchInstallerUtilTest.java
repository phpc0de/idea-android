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

import static com.android.repository.testframework.FakePackage.FakeLocalPackage;
import static com.android.repository.testframework.FakePackage.FakeRemotePackage;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.android.repository.api.LocalPackage;
import com.android.repository.api.RemotePackage;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.testframework.FakeDependency;
import com.android.repository.testframework.FakeRepoManager;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;

/**
 * Tests for {@link PatchInstallerUtil}
 *
 * TODO: there should probably be tests for the more advanced functionality of PatchInstallerUtil as well.
 */
public class PatchInstallerUtilTest {

  @Test
  public void getDependantPatcher() {
    LocalPackage target = new FakeLocalPackage("patcher;v2");
    List<LocalPackage> local = ImmutableList.of(new FakeLocalPackage("patcher;v1"), target, new FakeLocalPackage("patcher;v3"));
    FakeRemotePackage update = new FakeRemotePackage("p");
    List<RemotePackage> remote = ImmutableList.of(update, new FakeRemotePackage("patcher;v4"));
    update.setDependencies(ImmutableList.of(new FakeDependency(target.getPath())));
    RepositoryPackages packages = new RepositoryPackages(local, remote);
    FakeRepoManager mgr = new FakeRepoManager(packages);
    LocalPackage patcher = PatchInstallerUtil.getDependantPatcher(update, mgr);
    assertEquals(target, patcher);
  }

  @Test
  public void dependantPatcherNotInstalled() {
    FakeLocalPackage target = new FakeLocalPackage("patcher;v2");
    List<LocalPackage> local = ImmutableList.of(new FakeLocalPackage("patcher;v1"), new FakeLocalPackage("patcher;v3"));
    FakeRemotePackage update = new FakeRemotePackage("p");
    List<RemotePackage> remote = ImmutableList.of(update, new FakeRemotePackage("patcher;v4"));
    update.setDependencies(ImmutableList.of(new FakeDependency(target.getPath())));
    RepositoryPackages packages = new RepositoryPackages(local, remote);
    FakeRepoManager mgr = new FakeRepoManager(packages);
    LocalPackage patcher = PatchInstallerUtil.getDependantPatcher(update, mgr);
    assertNull(patcher);
  }

  @Test
  public void getLatestPatcher() {
    LocalPackage target = new FakeLocalPackage("patcher;v3");
    List<LocalPackage> local = ImmutableList.of(new FakeLocalPackage("patcher;v1"), target, new FakeLocalPackage("patcher;v2"));
    RepositoryPackages packages = new RepositoryPackages(local, ImmutableList.of());
    FakeRepoManager mgr = new FakeRepoManager(packages);
    LocalPackage patcher = PatchInstallerUtil.getLatestPatcher(mgr);
    assertEquals(target, patcher);
  }

  @Test
  public void noLatestPatcher() {
    List<LocalPackage> local = ImmutableList.of(new FakeLocalPackage("foo"));
    RepositoryPackages packages = new RepositoryPackages(local, ImmutableList.of());
    FakeRepoManager mgr = new FakeRepoManager(packages);
    LocalPackage patcher = PatchInstallerUtil.getLatestPatcher(mgr);
    assertNull(patcher);
  }

  @Test
  public void comparePaths() {
    assertTrue(PatchInstallerUtil.comparePatcherPaths("patcher;v1", "patcher;v2") < 0);
    assertTrue(PatchInstallerUtil.comparePatcherPaths("patcher;v2", "patcher;v1") > 0);
    assertEquals(0, PatchInstallerUtil.comparePatcherPaths("patcher;v2", "patcher;v2"));
    assertTrue(PatchInstallerUtil.comparePatcherPaths("bogus", "patcher;v2") < 0);
    assertTrue(PatchInstallerUtil.comparePatcherPaths("patcher;v1", "bogus") > 0);
  }
}
