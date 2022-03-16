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
package com.android.tools.idea.testartifacts.scopes;

import com.google.common.collect.ImmutableList;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.io.File;

import static com.intellij.openapi.util.io.FileUtil.createIfNotExists;
import static com.intellij.openapi.util.io.FileUtil.getTempDirectory;

public class FileRootSearchScopeTest extends HeavyPlatformTestCase {
  public void testAcceptFileCreatedLater() {
    File root = new File(getTempDirectory(), "root");
    FileRootSearchScope searchScope = new FileRootSearchScope(getProject(), ImmutableList.of(root));
    createIfNotExists(root);
    assertTrue(searchScope.accept(getVirtualFile(root)));
  }

  public void testScopeUnite() throws Exception {
    File srcRoot = createTempDir("srcRoot");
    FileRootSearchScope scope1 = new FileRootSearchScope(getProject(), ImmutableList.of(srcRoot));

    File genSrcRoot = createTempDir("genSrcRoot");
    FileRootSearchScope scope2 = new FileRootSearchScope(getProject(), ImmutableList.of(genSrcRoot));

    FileRootSearchScope unitedScope = scope1.add(scope2);
    assertTrue(unitedScope.accept(getVirtualFile(srcRoot)));
    assertTrue(unitedScope.accept(getVirtualFile(genSrcRoot)));
  }

  public void testAcceptNotExistedFileInExistedRoot() throws Exception {
    File root = createTempDir("root");
    FileRootSearchScope searchScope = new FileRootSearchScope(getProject(), ImmutableList.of(root));
    searchScope.accept(new File(root, "notexist"));
  }

  public void testAcceptNotExistedFileInNonExistedRoot() throws Exception {
    File root = new File("root");
    FileRootSearchScope searchScope = new FileRootSearchScope(getProject(), ImmutableList.of(root));
    searchScope.accept(new File(root, "notexist"));
  }
}
