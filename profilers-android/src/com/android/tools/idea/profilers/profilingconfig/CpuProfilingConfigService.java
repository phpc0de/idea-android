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
package com.android.tools.idea.profilers.profilingconfig;

import com.android.tools.profilers.cpu.config.ProfilingConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service to persist the state of {@link ProfilingConfiguration}.
 * @deprecated Use {@link com.android.tools.idea.run.profiler.CpuProfilerConfigsState} instead.
 *             This class should be removed in later versions of Android Studio (http://b/74601959).
 */
@Deprecated
@State(name = "CpuProfilingConfigService", storages = @Storage("cpuProfilingConfigs.xml"))
public class CpuProfilingConfigService implements PersistentStateComponent<CpuProfilingConfigService> {

  private List<ProfilingConfiguration> myConfigurations;

  public CpuProfilingConfigService() {
    myConfigurations = new ArrayList<>();
  }

  public static CpuProfilingConfigService getInstance(Project project) {
    return project.getService(CpuProfilingConfigService.class);
  }

  public List<ProfilingConfiguration> getConfigurations() {
    return myConfigurations;
  }

  public void setConfigurations(List<ProfilingConfiguration> configurations) {
    myConfigurations = configurations;
  }

  @Nullable
  @Override
  public CpuProfilingConfigService getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CpuProfilingConfigService state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
