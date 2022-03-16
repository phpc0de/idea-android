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
package com.android.tools.idea.profilers;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.project.sync.hyperlink.OpenUrlHyperlink;
import com.android.tools.idea.profilers.analytics.StudioFeatureTracker;
import com.android.tools.idea.profilers.perfetto.traceprocessor.TraceProcessorServiceImpl;
import com.android.tools.idea.profilers.profilingconfig.CpuProfilerConfigConverter;
import com.android.tools.idea.profilers.profilingconfig.CpuProfilingConfigService;
import com.android.tools.idea.profilers.stacktrace.IntelliJNativeFrameSymbolizer;
import com.android.tools.idea.profilers.stacktrace.ProfilerCodeNavigator;
import com.android.tools.idea.project.AndroidNotification;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.editor.ProfilerState;
import com.android.tools.idea.run.profiler.CpuProfilerConfigsState;
import com.android.tools.inspectors.common.api.stacktrace.CodeNavigator;
import com.android.tools.nativeSymbolizer.NativeSymbolizer;
import com.android.tools.nativeSymbolizer.NativeSymbolizerKt;
import com.android.tools.nativeSymbolizer.SymbolFilesLocatorKt;
import com.android.tools.profilers.FeatureConfig;
import com.android.tools.profilers.IdeProfilerServices;
import com.android.tools.profilers.Notification;
import com.android.tools.profilers.ProfilerPreferences;
import com.android.tools.profilers.analytics.FeatureTracker;
import com.android.tools.profilers.cpu.config.ProfilingConfiguration;
import com.android.tools.profilers.perfetto.traceprocessor.TraceProcessorService;
import com.android.tools.profilers.stacktrace.NativeFrameSymbolizer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntellijProfilerServices implements IdeProfilerServices, Disposable {

  private static Logger getLogger() {
    return Logger.getInstance(IntellijProfilerServices.class);
  }

  private final ProfilerCodeNavigator myCodeNavigator;
  @NotNull private final NativeFrameSymbolizer myNativeSymbolizer;
  private final StudioFeatureTracker myFeatureTracker;

  @NotNull private final Project myProject;
  @NotNull private final IntellijProfilerPreferences myPersistentPreferences;
  @NotNull private final TemporaryProfilerPreferences myTemporaryPreferences;

  public IntellijProfilerServices(@NotNull Project project) {
    myProject = project;
    myFeatureTracker = new StudioFeatureTracker(myProject);
    NativeSymbolizer nativeSymbolizer = NativeSymbolizerKt.createNativeSymbolizer(project);
    Disposer.register(this, nativeSymbolizer::stop);
    myNativeSymbolizer = new IntelliJNativeFrameSymbolizer(nativeSymbolizer);
    myCodeNavigator = new ProfilerCodeNavigator(project, nativeSymbolizer, myFeatureTracker);
    myPersistentPreferences = new IntellijProfilerPreferences();
    myTemporaryPreferences = new TemporaryProfilerPreferences();
  }

  @Override
  public void dispose() {
    // Dispose logic handled in constructor.
  }

  @NotNull
  @Override
  public Executor getMainExecutor() {
    return ApplicationManager.getApplication()::invokeLater;
  }

  @NotNull
  @Override
  public Executor getPoolExecutor() {
    return ApplicationManager.getApplication()::executeOnPooledThread;
  }

  @Override
  public Set<String> getAllProjectClasses() {
    Query<PsiClass> query = AllClassesSearch.INSTANCE.search(ProjectScope.getProjectScope(myProject), myProject);

    Set<String> classNames = new HashSet<>();
    query.forEach(aClass -> {
      classNames.add(aClass.getQualifiedName());
    });
    return classNames;
  }

  @Override
  public void saveFile(@NotNull File file, @NotNull Consumer<FileOutputStream> fileOutputStreamConsumer, @Nullable Runnable postRunnable) {
    File parentDir = file.getParentFile();
    if (!parentDir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      parentDir.mkdirs();
    }
    if (!file.exists()) {
      try {
        if (!file.createNewFile()) {
          getLogger().error("Could not create new file at: " + file.getPath());
          return;
        }
      }
      catch (IOException e) {
        getLogger().error(e);
      }
    }

    try (FileOutputStream fos = new FileOutputStream(file)) {
      fileOutputStreamConsumer.accept(fos);
    }
    catch (IOException e) {
      getLogger().error(e);
    }

    VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
    if (virtualFile != null) {
      virtualFile.refresh(true, false, postRunnable);
    }
  }

  @NotNull
  @Override
  public NativeFrameSymbolizer getNativeFrameSymbolizer() {
    return myNativeSymbolizer;
  }

  @NotNull
  @Override
  public CodeNavigator getCodeNavigator() {
    return myCodeNavigator;
  }

  @NotNull
  @Override
  public FeatureTracker getFeatureTracker() {
    return myFeatureTracker;
  }

  /**
   * Note - this opens the Run Configuration dialog which is modal and blocking until the dialog closes.
   */
  @Override
  public void enableAdvancedProfiling() {
    // Attempts to find the AndroidRunConfiguration to enable the profiler state validation.
    AndroidRunConfigurationBase androidConfiguration = null;
    RunManager runManager = RunManager.getInstance(myProject);
    if (runManager != null) {
      RunnerAndConfigurationSettings configurationSettings = runManager.getSelectedConfiguration();
      if (configurationSettings != null && configurationSettings.getConfiguration() instanceof AndroidRunConfigurationBase) {
        androidConfiguration = (AndroidRunConfigurationBase)configurationSettings.getConfiguration();
        androidConfiguration.getProfilerState().setCheckAdvancedProfiling(true);
      }
    }

    EditConfigurationsDialog dialog = new EditConfigurationsDialog(myProject);
    dialog.show();

    if (androidConfiguration != null) {
      androidConfiguration.getProfilerState().setCheckAdvancedProfiling(false);
    }
  }

  @NotNull
  @Override
  public FeatureConfig getFeatureConfig() {
    return new FeatureConfigProd();
  }

  @NotNull
  @Override
  public ProfilerPreferences getTemporaryProfilerPreferences() {
    return myTemporaryPreferences;
  }

  @NotNull
  @Override
  public ProfilerPreferences getPersistentProfilerPreferences() {
    return myPersistentPreferences;
  }

  @Override
  public void openYesNoDialog(String message, String title, Runnable yesCallback, Runnable noCallback) {
    int dialogResult = Messages.showYesNoDialog(myProject, message, title, Messages.getWarningIcon());
    (dialogResult == Messages.YES ? yesCallback : noCallback).run();
  }

  @Override
  @Nullable
  public <T> T openListBoxChooserDialog(@NotNull String title,
                                        @Nullable String message,
                                        @NotNull List<T> options,
                                        @NotNull Function<T, String> listBoxPresentationAdapter) {

    AtomicReference<T> selectedValue = new AtomicReference<>();
    Supplier<T> dialog = () -> {
      ListBoxChooserDialog<T> listBoxDialog = new ListBoxChooserDialog<>(title, message, options, listBoxPresentationAdapter);
      listBoxDialog.show();
      return listBoxDialog.getExitCode() != DialogWrapper.OK_EXIT_CODE ? null : listBoxDialog.getSelectedValue();
    };
    // Check if we are on a thread that is able to dispatch ui events. If we are show the dialog, otherwise invoke the dialog later.
    if (SwingUtilities.isEventDispatchThread()) {
      selectedValue.set(dialog.get());
    }
    else {
      // Object to control communication between the render thread and the capture thread.
      CountDownLatch latch = new CountDownLatch(1);
      try {
        // Tell UI thread that we want to show a dialog then block capture thread
        // until user has made a selection.
        ApplicationManager.getApplication().invokeLater(() -> {
          selectedValue.set(dialog.get());
          latch.countDown();
        });
        //noinspection WaitNotInLoop
        latch.await();
      }
      catch (InterruptedException ex) {
        // If our wait was interrupted continue.
      }
    }
    return selectedValue.get();
  }

  /**
   * Gets a {@link List} of directories containing the symbol files corresponding to the architecture of the session currently selected.
   */
  @NotNull
  @Override
  public List<String> getNativeSymbolsDirectories() {
    String arch = myCodeNavigator.fetchCpuAbiArch();
    Map<String, Set<File>> archToDirectories = SymbolFilesLocatorKt.getArchToSymDirsMap(myProject);
    if (!archToDirectories.containsKey(arch)) {
      return Collections.emptyList();
    }
    return ContainerUtil.map(archToDirectories.get(arch), file -> file.getAbsolutePath());
  }

  @Override
  public List<ProfilingConfiguration> getUserCpuProfilerConfigs(int apiLevel) {
    CpuProfilerConfigsState configsState = CpuProfilerConfigsState.getInstance(myProject);
    CpuProfilingConfigService oldService = CpuProfilingConfigService.getInstance(myProject);

    // We use the deprecated |oldService| to migrate the user created configurations to the new persistent class.
    // |oldService| probably will be removed in coming versions of Android Studio: http://b/74601959
    oldService.getConfigurations().forEach(old -> configsState.addUserConfig(CpuProfilerConfigConverter.fromProto(old.toProto())));
    // We don't need configurations from |oldService| anymore, so clear it.
    oldService.setConfigurations(Collections.emptyList());

    return ContainerUtil.map(
      CpuProfilerConfigConverter.toProto(configsState.getUserConfigs(), apiLevel),
      ProfilingConfiguration::fromProto);
  }

  @Override
  public List<ProfilingConfiguration> getDefaultCpuProfilerConfigs(int apiLevel) {
    return ContainerUtil.map(
      CpuProfilerConfigConverter.toProto(CpuProfilerConfigsState.getDefaultConfigs(), apiLevel),
      ProfilingConfiguration::fromProto
    );
  }

  @Override
  public boolean isNativeProfilingConfigurationPreferred() {
    // File extensions that we consider native. We can add more later if we feel that's necessary.
    ImmutableList<String> nativeExtensions = ImmutableList.of("c", "cc", "cpp", "cxx", "c++", "h", "hh", "hpp", "hxx", "h++");
    // If the user is viewing at least one (IntelliJ allows the user to view multiple files at the same time) native file,
    // we want to give preference to a native CPU profiling configuration.
    return Arrays.stream(FileEditorManager.getInstance(myProject).getSelectedFiles())
      .anyMatch(file -> {
        String extension = file.getExtension();
        return extension != null && nativeExtensions.contains(StringUtil.toLowerCase(extension));
      });
  }

  @Override
  public int getNativeMemorySamplingRateForCurrentConfig() {
    RunnerAndConfigurationSettings settings = RunManager.getInstance(myProject).getSelectedConfiguration();
    if (settings != null && settings.getConfiguration() instanceof AndroidRunConfigurationBase) {
      AndroidRunConfigurationBase runConfig = (AndroidRunConfigurationBase)settings.getConfiguration();
      return runConfig.getProfilerState().NATIVE_MEMORY_SAMPLE_RATE_BYTES;
    }
    return ProfilerState.DEFAULT_NATIVE_MEMORY_SAMPLE_RATE_BYTES;
  }

  @Override
  public void showNotification(@NotNull Notification notification) {
    NotificationType type = null;

    switch (notification.getSeverity()) {
      case INFO:
        type = NotificationType.INFORMATION;
        break;
      case WARNING:
        type = NotificationType.WARNING;
        break;
      case ERROR:
        type = NotificationType.ERROR;
        break;
    }

    Notification.UrlData urlData = notification.getUrlData();
    if (urlData != null) {
      OpenUrlHyperlink hyperlink = new OpenUrlHyperlink(urlData.getUrl(), urlData.getText());
      AndroidNotification.getInstance(myProject)
        .showBalloon(notification.getTitle(), notification.getText(), type, AndroidNotification.BALLOON_GROUP, false,
                     hyperlink);
    }
    else {
      AndroidNotification.getInstance(myProject)
        .showBalloon(notification.getTitle(), notification.getText(), type, AndroidNotification.BALLOON_GROUP);
    }
  }

  @NotNull
  @Override
  public TraceProcessorService getTraceProcessorService() {
    return TraceProcessorServiceImpl.getInstance();
  }

  /**
   * Implementation of {@link FeatureConfig} with values used in production.
   */
  @VisibleForTesting
  public static class FeatureConfigProd implements FeatureConfig {
    @Override
    public boolean isCpuCaptureStageEnabled() {
      return StudioFlags.PROFILER_CPU_CAPTURE_STAGE.get();
    }

    @Override
    public boolean isNativeMemorySampleEnabled() {
      return StudioFlags.PROFILER_ENABLE_NATIVE_SAMPLE.get();
    }

    @Override
    public boolean isCpuNewRecordingWorkflowEnabled() {
      return StudioFlags.PROFILER_CPU_NEW_RECORDING_WORKFLOW.get();
    }

    @Override
    public boolean isEnergyProfilerEnabled() {
      return StudioFlags.PROFILER_ENERGY_PROFILER_ENABLED.get();
    }

    @Override
    public boolean isJniReferenceTrackingEnabled() {
      return StudioFlags.PROFILER_TRACK_JNI_REFS.get();
    }

    @Override
    public boolean isLiveAllocationsEnabled() {
      return StudioFlags.PROFILER_USE_LIVE_ALLOCATIONS.get();
    }

    @Override
    public boolean isLiveAllocationsSamplingEnabled() {
      return StudioFlags.PROFILER_SAMPLE_LIVE_ALLOCATIONS.get();
    }

    @Override
    public boolean isMemoryCSVExportEnabled() {
      return StudioFlags.PROFILER_MEMORY_CSV_EXPORT.get();
    }

    @Override
    public boolean isMemorySnapshotEnabled() {
      return StudioFlags.PROFILER_MEMORY_SNAPSHOT.get();
    }

    @Override
    public boolean isPerformanceMonitoringEnabled() {
      return StudioFlags.PROFILER_PERFORMANCE_MONITORING.get();
    }

    @Override
    public boolean isProfileableEnabled() {
      return StudioFlags.PROFILEABLE.get();
    }

    @Override
    public boolean isProfileableInQrEnabled() {
      return StudioFlags.PROFILEABLE_IN_QR.get();
    }

    @Override
    public boolean isStartupCpuProfilingEnabled() {
      return StudioFlags.PROFILER_STARTUP_CPU_PROFILING.get();
    }

    @Override
    public boolean isUnifiedPipelineEnabled() {
      return StudioFlags.PROFILER_UNIFIED_PIPELINE.get();
    }

    @Override
    public boolean isUseTraceProcessor() {
      return StudioFlags.PROFILER_USE_TRACEPROCESSOR.get();
    }

    @Override
    public boolean isCustomEventVisualizationEnabled() {
      return StudioFlags.PROFILER_CUSTOM_EVENT_VISUALIZATION.get();
    }
  }
}
