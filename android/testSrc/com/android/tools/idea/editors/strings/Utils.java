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
package com.android.tools.idea.editors.strings;

import com.android.tools.idea.editors.strings.table.StringResourceTableModel;
import com.android.tools.idea.res.LocalResourceRepository;
import com.android.tools.idea.res.ResourcesTestsUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

final class Utils {
  private Utils() {
  }

  static void loadResources(@NotNull StringResourceViewPanel panel, @NotNull Collection<Path> resPaths) {
    AndroidFacet facet = panel.getFacet();
    LocalFileSystem system = LocalFileSystem.getInstance();

    Collection<VirtualFile> resVirtualFiles = resPaths.stream()
      .map(path -> system.findFileByIoFile(path.toFile()))
      .collect(Collectors.toList());

    LocalResourceRepository repository = ResourcesTestsUtil.createTestModuleRepository(facet, resVirtualFiles);
    panel.getTable().setModel(new StringResourceTableModel(StringResourceRepository.create(repository), facet.getModule().getProject()));
  }
}
