/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.setup.module.idea.java;

import com.android.tools.idea.gradle.project.sync.setup.module.common.ModuleDependenciesSetup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;

import static com.intellij.openapi.roots.OrderRootType.CLASSES;
import static com.intellij.openapi.roots.OrderRootType.SOURCES;

class JavaModuleDependenciesSetup extends ModuleDependenciesSetup {
  private static final Logger LOG = Logger.getInstance(JavaModuleDependenciesSetup.class);
  void setUpLibraryDependency(@NotNull Module module,
                              @NotNull IdeModifiableModelsProvider modelsProvider,
                              @NotNull String libraryName,
                              @NotNull DependencyScope scope,
                              @NotNull File binaryPath,
                              @Nullable File sourcePath,
                              @Nullable File documentationPath,
                              boolean isExported) {
    // let's use the same format for libraries imported from Gradle, to be compatible with API like ExternalSystemApiUtil.isExternalSystemLibrary()
    // and be able to reuse common cleanup service, see LibraryDataService.postProcess()
    String prefix = GradleConstants.SYSTEM_ID.getReadableName() + ": ";
    libraryName = libraryName.isEmpty() || StringUtil.startsWith(libraryName, prefix) ? libraryName : prefix + libraryName;

    boolean newLibrary = false;
    Library library = modelsProvider.getLibraryByName(libraryName);
    if (library == null || !isLibraryValid(modelsProvider.getModifiableLibraryModel(library), new File[]{binaryPath})) {
      if (library != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(library.getName() + " not valid after sync.");
        }
        modelsProvider.removeLibrary(library);
      }
      library = modelsProvider.createLibrary(libraryName);
      newLibrary = true;
    }

    if (newLibrary) {
      updateLibraryRootPath(library, CLASSES, modelsProvider, binaryPath);
      updateLibraryRootPath(library, SOURCES, modelsProvider, sourcePath);
      updateLibraryRootPath(library, JavadocOrderRootType.getInstance(), modelsProvider, documentationPath);
    }
    addLibraryAsDependency(library, libraryName, scope, module, modelsProvider, isExported);
  }

  private void updateLibraryRootPath(@NotNull Library library,
                                     @NotNull OrderRootType rootType,
                                     @NotNull IdeModifiableModelsProvider modelsProvider,
                                     @Nullable File path) {
    if (path != null) {
      updateLibraryRootTypePaths(library, rootType, modelsProvider, path);
    }
  }
}
