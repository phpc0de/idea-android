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

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

import static com.intellij.openapi.util.io.FileUtil.isAncestor;
import static com.intellij.openapi.vfs.VfsUtilCore.urlToPath;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;

public final class ContentEntries {
  private ContentEntries() {
  }

  @Nullable
  public static ContentEntry findParentContentEntry(@NotNull File path, @NotNull Stream<ContentEntry> contentEntries) {
    Optional<ContentEntry> optional = contentEntries.filter(contentEntry -> isPathInContentEntry(path, contentEntry)).findFirst();
    return optional.isPresent() ? optional.get() : null;
  }

  @NotNull
  public static Collection<ContentEntry> findChildContentEntries(@NotNull File path, @NotNull Stream<ContentEntry> contentEntries) {
    return contentEntries.filter(contentEntry -> isContentEntryUnderPath(path, contentEntry)).collect(Collectors.toList());
  }

  public static boolean isPathInContentEntry(@NotNull File path, @NotNull ContentEntry contentEntry) {
    return isAncestor(findContentEntryPath(contentEntry), path, false);
  }

  private static boolean isContentEntryUnderPath(@NotNull File path, @NotNull ContentEntry contentEntry) {
    return isAncestor(path, findContentEntryPath(contentEntry), false);
  }

  @NotNull
  private static File findContentEntryPath(@NotNull ContentEntry contentEntry) {
    VirtualFile rootFile = contentEntry.getFile();
    if (rootFile == null) {
      String s = urlToPath(contentEntry.getUrl());
      return new File(s);
    }
    else {
      return virtualToIoFile(rootFile);
    }
  }
}
