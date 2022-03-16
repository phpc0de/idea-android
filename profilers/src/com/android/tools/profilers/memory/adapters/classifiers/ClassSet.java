/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.profilers.memory.adapters.classifiers;

import static com.android.tools.profilers.memory.adapters.ClassDb.INVALID_CLASS_ID;

import com.android.tools.adtui.model.filter.Filter;
import com.android.tools.profilers.memory.adapters.ClassDb;
import com.android.tools.profilers.memory.adapters.InstanceObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Classifies {@link InstanceObject}s based on their {@link Class}.
 */
public class ClassSet extends ClassifierSet {
  public static final ClassSet EMPTY_SET = new ClassSet(new ClassDb.ClassEntry(INVALID_CLASS_ID, INVALID_CLASS_ID, "null"));

  @NotNull private final ClassDb.ClassEntry myClassEntry;

  @NotNull
  public static Classifier createDefaultClassifier() {
    return new ClassClassifier();
  }

  public ClassSet(@NotNull ClassDb.ClassEntry classEntry) {
    super(classEntry.getSimpleClassName());
    myClassEntry = classEntry;
  }

  @NotNull
  public ClassDb.ClassEntry getClassEntry() {
    return myClassEntry;
  }

  @NotNull
  @Override
  public Classifier createSubClassifier() {
    // Do nothing, as this is a leaf node (presently).
    return Classifier.IDENTITY_CLASSIFIER;
  }

  @Override
  protected void applyFilter(@NotNull Filter filter, boolean hasMatchedAncestor, boolean filterChanged) {
    if (!filterChanged && !myNeedsRefiltering) {
      return;
    }
    myIsMatched = matches(filter);
    myFilterMatchCount = myIsMatched ? 1 : 0;
    myIsFiltered = filter != null && !myIsMatched && !hasMatchedAncestor;
    myNeedsRefiltering = false;
  }

  @Override
  protected boolean matches(@NotNull Filter filter) {
    return filter.matches(myClassEntry.getClassName());
  }

  private static final class ClassClassifier extends Classifier {
    @NotNull private final Map<ClassDb.ClassEntry, ClassSet> myClassMap = new LinkedHashMap<>();

    @Nullable
    @Override
    public ClassifierSet getClassifierSet(@NotNull InstanceObject instance, boolean createIfAbsent) {
      ClassDb.ClassEntry classEntry = instance.getClassEntry();
      ClassSet classSet = myClassMap.get(classEntry);
      if (classSet == null && createIfAbsent) {
        classSet = new ClassSet(classEntry);
        myClassMap.put(classEntry, classSet);
      }
      return classSet;
    }

    @NotNull
    @Override
    public List<ClassifierSet> getFilteredClassifierSets() {
      return myClassMap.values().stream().filter(child -> !child.getIsFiltered()).collect(Collectors.toList());
    }

    @NotNull
    @Override
    protected List<ClassifierSet> getAllClassifierSets() {
      return myClassMap.values().stream().collect(Collectors.toList());
    }
  }
}
