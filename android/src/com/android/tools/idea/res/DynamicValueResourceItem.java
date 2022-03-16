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
package com.android.tools.idea.res;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.ResourceValueImpl;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.SingleNamespaceResourceRepository;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.util.PathString;
import com.android.resources.ResourceType;
import com.intellij.psi.EmptyResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ResourceItem} for an item that is dynamically defined in a Gradle file.
 */
public class DynamicValueResourceItem implements ResourceItem, ResolvableResourceItem {
  @NotNull private final ResourceValue myResourceValue;
  @NotNull private final DynamicValueResourceRepository myOwner;

  public DynamicValueResourceItem(@NotNull DynamicValueResourceRepository owner,
                                  @NotNull ResourceType type,
                                  @NotNull String name,
                                  @NotNull String value) {
    myOwner = owner;
    myResourceValue = new ResourceValueImpl(owner.getNamespace(), type, name, value);
  }

  @Override
  @NotNull
  public ResolveResult createResolveResult() {
    // TODO: Try to find the item in the Gradle files
    return EmptyResolveResult.INSTANCE;
  }

  @Override
  @NotNull
  public String getName() {
    return myResourceValue.getName();
  }

  @Override
  @NotNull
  public ResourceType getType() {
    return myResourceValue.getResourceType();
  }

  @Override
  @Nullable
  public String getLibraryName() {
    return null;
  }

  @Override
  @NotNull
  public SingleNamespaceResourceRepository getRepository() {
    return myOwner;
  }

  @Override
  @NotNull
  public ResourceNamespace getNamespace() {
    return myOwner.getNamespace();
  }

  @Override
  @NotNull
  public ResourceReference getReferenceToSelf() {
    return myResourceValue.asReference();
  }

  @Override
  @NotNull
  public FolderConfiguration getConfiguration() {
    return DEFAULT_CONFIGURATION;
  }

  @Override
  @NotNull
  public String getKey() {
    return myResourceValue.getResourceUrl().toString().substring(1);
  }

  @Override
  @NotNull
  public ResourceValue getResourceValue() {
    return myResourceValue;
  }

  @Override
  @Nullable
  public PathString getSource() {
    return null;
  }

  @Override
  public boolean isFileBased() {
    return false;
  }
}
