/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run.editor;

import com.android.tools.idea.instantapp.InstantAppUrlFinder;
import com.android.tools.idea.run.AndroidRunConfiguration;
import com.android.tools.idea.run.ApkProvider;
import com.android.tools.idea.run.tasks.AppLaunchTask;
import com.android.tools.idea.run.ValidationError;
import com.android.tools.idea.run.activity.StartActivityFlagsProvider;
import com.android.tools.idea.run.tasks.AndroidDeepLinkLaunchTask;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.android.AndroidProjectTypes.PROJECT_TYPE_INSTANTAPP;
import static com.android.tools.idea.instantapp.InstantApps.findFeatureModules;

public class DeepLinkLaunch extends LaunchOption<DeepLinkLaunch.State> {
  public static final DeepLinkLaunch INSTANCE = new DeepLinkLaunch();

  public static final class State extends LaunchOptionState {
    public String DEEP_LINK = "";

    @Nullable
    @Override
    public AppLaunchTask getLaunchTask(@NotNull String applicationId,
                                       @NotNull AndroidFacet facet,
                                       @NotNull StartActivityFlagsProvider startActivityFlagsProvider,
                                       @NotNull ProfilerState profilerState,
                                       @NotNull ApkProvider apkProvider) {
      return new AndroidDeepLinkLaunchTask(DEEP_LINK, startActivityFlagsProvider);
    }

    @NotNull
    @Override
    public List<ValidationError> checkConfiguration(@NotNull AndroidFacet facet) {
      boolean isInstantApp = facet.getConfiguration().getProjectType() == PROJECT_TYPE_INSTANTAPP;

      if ((DEEP_LINK == null || DEEP_LINK.isEmpty())) {
        if (isInstantApp) {
          // The new AIA SDK library supports launching instant apps without a URL
          return ImmutableList.of();
        }
        else {
          return ImmutableList.of(ValidationError.warning("URL not specified"));
        }
      }

      if (isInstantApp) {
        boolean matched = false;
        List<Module> featureModules = findFeatureModules(facet);
        for (Module featureModule : featureModules) {
          if (new InstantAppUrlFinder(featureModule).matchesUrl(DEEP_LINK)) {
            matched = true;
            break;
          }
        }
        if (!matched) {
          return ImmutableList.of(ValidationError.warning("URL \"" + DEEP_LINK + "\" not defined in the manifest."));
        }
      }
      return ImmutableList.of();
    }
  }

  @NotNull
  @Override
  public String getId() {
    return AndroidRunConfiguration.LAUNCH_DEEP_LINK;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "URL";
  }

  @NotNull
  @Override
  public State createState() {
    return new State();
  }

  @NotNull
  @Override
  public LaunchOptionConfigurable<State> createConfigurable(@NotNull Project project, @NotNull LaunchOptionConfigurableContext context) {
    return new DeepLinkConfigurable(project, context);
  }
}

