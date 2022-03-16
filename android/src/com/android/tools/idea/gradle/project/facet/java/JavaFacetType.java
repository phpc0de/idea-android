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
package com.android.tools.idea.gradle.project.facet.java;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaFacetType extends FacetType<JavaFacet, JavaFacetConfiguration> {
  public JavaFacetType() {
    super(JavaFacet.getFacetTypeId(), JavaFacet.getFacetId(), JavaFacet.getFacetName());
  }

  @Override
  @NotNull
  public JavaFacetConfiguration createDefaultConfiguration() {
    return new JavaFacetConfiguration();
  }

  @Override
  public JavaFacet createFacet(@NotNull Module module,
                               String name,
                               @NotNull JavaFacetConfiguration configuration,
                               @Nullable Facet underlyingFacet) {
    return new JavaFacet(module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }
}
