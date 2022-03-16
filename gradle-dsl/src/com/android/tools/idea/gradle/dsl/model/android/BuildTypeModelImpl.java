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
package com.android.tools.idea.gradle.dsl.model.android;

import static com.android.tools.idea.gradle.dsl.parser.crashlytics.FirebaseCrashlyticsDslElement.FIREBASE_CRASHLYTICS;

import com.android.tools.idea.gradle.dsl.api.android.BuildTypeModel;
import com.android.tools.idea.gradle.dsl.api.crashlytics.FirebaseCrashlyticsModel;
import com.android.tools.idea.gradle.dsl.api.ext.ResolvedPropertyModel;
import com.android.tools.idea.gradle.dsl.model.crashlytics.FirebaseCrashlyticsModelImpl;
import com.android.tools.idea.gradle.dsl.parser.android.BuildTypeDslElement;
import com.android.tools.idea.gradle.dsl.parser.crashlytics.FirebaseCrashlyticsDslElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class BuildTypeModelImpl extends FlavorTypeModelImpl implements BuildTypeModel {
  /**
   * these names are used within the implementation below, and also in the construction of Dsl elements
   * representing the state of {@link BuildTypeDslElement}s.
   */
  @NonNls public static final String CRUNCH_PNGS = "mCrunchPngs";
  @NonNls public static final String DEBUGGABLE = "mDebuggable";
  @NonNls public static final String DEFAULT = "mDefault";
  @NonNls public static final String EMBED_MICRO_APP = "mEmbedMicroApp";
  @NonNls public static final String JNI_DEBUGGABLE = "mJniDebuggable";
  @NonNls public static final String MINIFY_ENABLED = "mMinifyEnabled";
  @NonNls public static final String PSEUDO_LOCALES_ENABLED = "mPseudoLocalesEnabled";
  @NonNls public static final String RENDERSCRIPT_DEBUGGABLE = "mRenderscriptDebuggable";
  @NonNls public static final String RENDERSCRIPT_OPTIM_LEVEL = "mRenderscriptOptimLevel";
  @NonNls public static final String SHRINK_RESOURCES = "mShrinkResources";
  @NonNls public static final String TEST_COVERAGE_ENABLED = "mTestCoverageEnabled";
  @NonNls public static final String USE_PROGUARD = "mUseProguard";
  @NonNls public static final String ZIP_ALIGN_ENABLED = "mZipAlignEnabled";

  public BuildTypeModelImpl(@NotNull BuildTypeDslElement dslElement) {
    super(dslElement);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel crunchPngs() {
    return getModelForProperty(CRUNCH_PNGS);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel debuggable() {
    return getModelForProperty(DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel embedMicroApp() {
    return getModelForProperty(EMBED_MICRO_APP);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel isDefault() {
    return getModelForProperty(DEFAULT);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel jniDebuggable() {
    return getModelForProperty(JNI_DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel minifyEnabled() {
    return getModelForProperty(MINIFY_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel pseudoLocalesEnabled() {
    return getModelForProperty(PSEUDO_LOCALES_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel renderscriptDebuggable() {
    return getModelForProperty(RENDERSCRIPT_DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel renderscriptOptimLevel() {
    return getModelForProperty(RENDERSCRIPT_OPTIM_LEVEL);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel shrinkResources() {
    return getModelForProperty(SHRINK_RESOURCES);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel testCoverageEnabled() {
    return getModelForProperty(TEST_COVERAGE_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel useProguard() {
    return getModelForProperty(USE_PROGUARD);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel zipAlignEnabled() {
    return getModelForProperty(ZIP_ALIGN_ENABLED);
  }

  @Override
  @NotNull
  public FirebaseCrashlyticsModel firebaseCrashlytics() {
    FirebaseCrashlyticsDslElement firebaseCrashlyticsDslElement = myDslElement.ensurePropertyElement(FIREBASE_CRASHLYTICS);
    return new FirebaseCrashlyticsModelImpl(firebaseCrashlyticsDslElement);
  }

}
