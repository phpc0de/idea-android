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
package com.android.tools.idea.gradle.util;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.io.PathKt;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link ContentEntries}.
 */
public class ContentEntriesTest extends HeavyPlatformTestCase {
  public void testFindContentEntryWithFileInContentEntry() {
    List<ContentEntry> contentEntries = new ArrayList<>();
    ContentEntry contentEntry = createContentEntry(getModule());
    contentEntries.add(contentEntry);

    Module module2 = createModule("module2");
    contentEntries.add(createContentEntry(module2));

    Path fakeLibraryPath = createFakeLibraryIn(contentEntry);
    ContentEntry found = ContentEntries.findParentContentEntry(fakeLibraryPath.toFile(), contentEntries.stream());
    assertSame(contentEntry, found);
  }

  public void testFindContentEntryWithFileNotInContentEntry() {
    List<ContentEntry> contentEntries = new ArrayList<>();
    contentEntries.add(createContentEntry(getModule()));

    Module module2 = createModule("module2");
    contentEntries.add(createContentEntry(module2));

    // This file exists outside the project. Should be in any content roots.
    Path fakeLibraryPath = createFakeLibraryOutsideProject();
    assertThat(ContentEntries.findParentContentEntry(fakeLibraryPath.toFile(), contentEntries.stream())).isNull();
  }

  public void testIsPathInContentEntryWithFileInContentEntry() {
    ContentEntry contentEntry = createContentEntry(getModule());
    Path fakeLibraryPath = createFakeLibraryIn(contentEntry);
    assertTrue(ContentEntries.isPathInContentEntry(fakeLibraryPath.toFile(), contentEntry));
  }

  private static @NotNull Path createFakeLibraryIn(@NotNull ContentEntry contentEntry) {
    VirtualFile contentEntryRootFile = contentEntry.getFile();
    assertNotNull(contentEntryRootFile);
    return contentEntryRootFile.toNioPath().resolve("fakeLibrary.jar");
  }

  public void testIsPathInContentEntryWithFileNotInContentEntry() {
    ContentEntry contentEntry = createContentEntry(getModule());
    Path fakeLibraryPath = createFakeLibraryOutsideProject();
    assertFalse(ContentEntries.isPathInContentEntry(fakeLibraryPath.toFile(), contentEntry));
  }

  private @NotNull Path createFakeLibraryOutsideProject() {
    Path result = getTempDir().newPath("fakeLibrary.jar");
    PathKt.createFile(result);
    return result;
  }

  @NotNull
  private ContentEntry createContentEntry(@NotNull Module module) {
    VirtualFile rootFolder = getTempDir().createVirtualDir();
    ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
    ContentEntry contentEntry = rootModel.addContentEntry(rootFolder);
    ApplicationManager.getApplication().runWriteAction(rootModel::commit);
    return contentEntry;
  }
}