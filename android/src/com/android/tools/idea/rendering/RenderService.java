/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.rendering;

import static com.intellij.lang.annotation.HighlightSeverity.ERROR;

import com.android.ide.common.rendering.api.MergeCookie;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.diagnostics.crash.StudioCrashReporter;
import com.android.tools.idea.layoutlib.LayoutLibrary;
import com.android.tools.idea.layoutlib.RenderingException;
import com.android.tools.idea.layoutlib.UnsupportedJavaRuntimeException;
import com.android.tools.idea.model.MergedManifestException;
import com.android.tools.idea.model.MergedManifestManager;
import com.android.tools.idea.model.MergedManifestSnapshot;
import com.android.tools.idea.project.AndroidProjectInfo;
import com.android.tools.idea.projectsystem.AndroidProjectSettingsService;
import com.android.tools.idea.rendering.classloading.ClassTransform;
import com.android.tools.idea.rendering.imagepool.ImagePool;
import com.android.tools.idea.rendering.imagepool.ImagePoolFactory;
import com.android.tools.idea.rendering.parsers.ILayoutPullParserFactory;
import com.android.tools.idea.rendering.parsers.LayoutPullParsers;
import com.android.tools.idea.rendering.parsers.TagSnapshot;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * The {@link RenderService} provides rendering and layout information for Android layouts. This is a wrapper around the layout library.
 */
public class RenderService implements Disposable {
  private static RenderExecutor ourExecutor;

  /**
   * {@link Key} used to keep the RenderService instance project association. They key is also used as synchronization object to guard the
   * access to the new instances.
   */
  private static final Key<RenderService> KEY = Key.create(RenderService.class.getName());

  static {
    ourExecutor = RenderExecutor.create();
    // Register the executor to be shutdown on close
    ShutDownTracker.getInstance().registerShutdownTask(RenderService::shutdownRenderExecutor);
  }

  private final Project myProject;

  @TestOnly
  public static void initializeRenderExecutor() {
    assert ApplicationManager.getApplication().isUnitTestMode(); // Only to be called from unit testszs

    ourExecutor = RenderExecutor.create();
  }

  private static void shutdownRenderExecutor() {
    ourExecutor.shutdown();
  }

  /**
   * Shutdowns the render thread and cancels any pending tasks.
   * @param timeoutSeconds if >0, wait at most this number of seconds before killing any running tasks.
   */
  @TestOnly
  public static void shutdownRenderExecutor(@SuppressWarnings("SameParameterValue") long timeoutSeconds) {
    assert ApplicationManager.getApplication().isUnitTestMode(); // Only to be called from unit tests

    ourExecutor.shutdown(timeoutSeconds);
  }

  private static final String JDK_INSTALL_URL = "https://developer.android.com/preview/setup-sdk.html#java8";

  private final Object myCredential = new Object();

  private final ImagePool myImagePool = ImagePoolFactory.createImagePool();

  @NotNull
  public static RenderAsyncActionExecutor getRenderAsyncActionExecutor() {
    return ourExecutor;
  }

  /**
   * @return the {@linkplain RenderService} for the given facet.
   */
  @NotNull
  public static RenderService getInstance(@NotNull Project project) {
    synchronized (KEY) {
      RenderService renderService = project.getUserData(KEY);
      if (renderService == null) {
        renderService = new RenderService(project);
        project.putUserData(KEY, renderService);
      }
      return renderService;
    }
  }

  @TestOnly
  public static void setForTesting(@NotNull Project project, @Nullable RenderService renderService) {
    synchronized (KEY) {
      project.putUserData(KEY, renderService);
    }
  }

  /**
   * Returns true if the current thread is the render thread managed by this executor.
   */
  public static boolean isCurrentThreadARenderThread() {
    return ourExecutor.isCurrentThreadARenderThread();
  }

  @VisibleForTesting
  protected RenderService(@NotNull Project project) {
    myProject = project;
    Disposer.register(project, this);
  }

  @Nullable
  public static LayoutLibrary getLayoutLibrary(@Nullable final Module module, @Nullable IAndroidTarget target) {
    if (module == null || target == null) {
      return null;
    }
    Project project = module.getProject();
    AndroidPlatform platform = AndroidPlatform.getInstance(module);
    if (platform != null) {
      try {
        return platform.getSdkData().getTargetData(target).getLayoutLibrary(project);
      }
      catch (RenderingException e) {
        // Ignore.
      }
    }
    return null;
  }

  /** Returns true if the given file can be rendered */
  public static boolean canRender(@Nullable PsiFile file) {
    return file != null && LayoutPullParsers.isSupported(file);
  }

  @NotNull
  public RenderLogger createLogger(@NotNull AndroidFacet facet) {
    Module module = facet.getModule();
    return new RenderLogger(module.getName(), module, myCredential);
  }

  @NotNull
  public RenderLogger getNopLogger() {
    return RenderLogger.NOP_RENDER_LOGGER;
  }

  /**
   * Returns a {@link RenderTaskBuilder} that can be used to build a new {@link RenderTask}
   */
  @NotNull
  public RenderTaskBuilder taskBuilder(@NotNull AndroidFacet facet,
                                       @NotNull Configuration configuration) {
    return new RenderTaskBuilder(this, facet, configuration, myImagePool, myCredential);
  }

  @Override
  public void dispose() {
    myProject.putUserData(KEY, null);
    myImagePool.dispose();
  }

  @Nullable
  public AndroidPlatform getPlatform(@NotNull AndroidFacet facet) {
    return AndroidPlatform.getInstance(facet.getModule());
  }

  @Nullable
  private static AndroidPlatform getPlatform(@NotNull final AndroidFacet facet, @Nullable RenderLogger logger) {
    Module module = facet.getModule();
    AndroidPlatform platform = AndroidPlatform.getInstance(module);
    if (platform == null && logger != null) {
      RenderProblem.Html message = RenderProblem.create(ERROR);
      logger.addMessage(message);
      message.getHtmlBuilder().addLink("No Android SDK found. Please ", "configure", " an Android SDK.",
         logger.getLinkManager().createRunnableLink(() -> {
           Project project = module.getProject();
           ProjectSettingsService service = ProjectSettingsService.getInstance(project);
           if (AndroidProjectInfo.getInstance(project).requiresAndroidModel() && service instanceof AndroidProjectSettingsService) {
             ((AndroidProjectSettingsService)service).openSdkSettings();
             return;
           }
           AndroidSdkUtils.openModuleDependenciesConfigurable(module);
         }));
    }
    return platform;
  }

  /**
   * Runs a action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   *
   * @deprecated This method is not safe to call, it might block unexpectedly waiting for the render thread.
   *  Use {@link RenderService#getRenderAsyncActionExecutor()} instead.
   */
  @Deprecated
  public static void runRenderAction(@NotNull final Runnable runnable) throws Exception {
    runRenderAction(Executors.callable(runnable));
  }

  /**
   * Runs a action that requires the rendering lock. Layoutlib is not thread safe so any rendering actions should be called using this
   * method.
   *
   * @deprecated This method is not safe to call, it might block unexpectedly waiting for the render thread.
   *  Use {@link RenderService#getRenderAsyncActionExecutor()} instead.
   */
  @Deprecated
  public static <T> T runRenderAction(@NotNull Callable<T> callable) throws Exception {
    return ourExecutor.runAction(callable);
  }

  /**
   * @return true if the underlying {@link RenderExecutor} is busy, false otherwise.
   */
  public static boolean isBusy() {
    return ourExecutor.isBusy();
  }

  /**
   * Given a {@link ViewInfo} from a layoutlib rendering, checks that the view info provides
   * valid bounds. This is normally the case. However, there are known scenarios, where
   * for various reasons, the View is left in a state where some of its bounds (left, right, top
   * or bottom) are not properly resolved; they carry MeasureSpec state along, which depending
   * on whether the specification was AT_MOST or EXACTLY this will either be a very large number,
   * or a very small (negative) number. In these cases we don't want to pass on the values to
   * further UI editing processing, since it for example can lead to calling Graphics#drawLine
   * with giant coordinates which can freeze the IDE; see for example
   * https://code.google.com/p/android/issues/detail?id=178690.
   * <p/>
   * To detect this, we simply need to check to see if the MeasureSpec mode bits are set
   * in any of the four bounds fields of the {@link ViewInfo}. Note however that these
   * view bounds are sometimes manipulated (e.g. values added or subtracted if a parent view
   * bound is also invalid) so rather than simply looking for the mode mask strictly, we look
   * in the nearby range too.
   *
   * @param view the {@link ViewInfo} to check
   * @return Normally the {@link ViewInfo} itself, but a placeholder 0-bound {@link ViewInfo} if
   * the view bounds are indeed invalid
   */
  @NotNull
  public static ViewInfo getSafeBounds(@NotNull ViewInfo view) {
    int left = Math.abs(view.getLeft());
    int right = Math.abs(view.getRight());
    int top = Math.abs(view.getTop());
    int bottom = Math.abs(view.getBottom());

    if (left < MAX_MAGNITUDE && right < MAX_MAGNITUDE && top < MAX_MAGNITUDE && bottom < MAX_MAGNITUDE) {
      return view;
    }
    else {
      // Not extracted as a constant; we expect this scenario to be rare
      return new ViewInfo(null, null, 0, 0, 0, 0);
    }
  }

  /**
   * Returns the {@link XmlTag} associated with a {@link ViewInfo}, if any
   *
   * @param view the view to check
   * @return the corresponding tag, if any
   */
  @Nullable
  public static XmlTag getXmlTag(@NotNull ViewInfo view) {
    Object cookie = view.getCookie();
    if (cookie != null) {
      if (cookie instanceof TagSnapshot) {
        TagSnapshot snapshot = (TagSnapshot)cookie;
        return snapshot.tag;
      }
      if (cookie instanceof MergeCookie) {
        cookie = ((MergeCookie) cookie).getCookie();
        if (cookie instanceof TagSnapshot) {
          TagSnapshot snapshot = (TagSnapshot)cookie;
          return snapshot.tag;
        }
      }
      if (cookie instanceof XmlTag) {
        return (XmlTag)cookie;
      }
    }

    return null;
  }

  @NotNull
  public ImagePool getSharedImagePool() {
    return myImagePool;
  }

  /** This is the View.MeasureSpec mode shift */
  private static final int MEASURE_SPEC_MODE_SHIFT = 30;

  /**
   * The maximum absolute value of bounds. This tries to identify values that carry
   * remnants of View.MeasureSpec mode bits, but accounts for the fact that sometimes arithmetic
   * is carried out on these values afterwards to bring them to lower values than they started
   * at, and we want to include those as well; there's a lot of room since the bits are shifted
   * quite a long way compared to the current relevant screen pixel ranges.
   */
  private static final int MAX_MAGNITUDE = 1 << (MEASURE_SPEC_MODE_SHIFT - 5);

  private static Logger getLogger() {
    return Logger.getInstance(RenderService.class);
  }

  public static class RenderTaskBuilder {
    private final RenderService myService;
    private final AndroidFacet myFacet;
    private final Configuration myConfiguration;
    private final Object myCredential;
    @NotNull private ImagePool myImagePool;
    @Nullable private PsiFile myPsiFile;
    @Nullable private RenderLogger myLogger;
    @Nullable private ILayoutPullParserFactory myParserFactory;
    private boolean isSecurityManagerEnabled = true;
    private float myDownscaleFactor = 1f;
    private boolean showDecorations = true;
    private boolean showWithToolsVisibilityAndPosition = true;
    private int myMaxRenderWidth = -1;
    private int myMaxRenderHeight = -1;
    private boolean isShadowEnabled = true;
    private boolean useHighQualityShadows = true;
    private boolean enableLayoutScanner = false;
    private boolean enableLayoutScannerOptimization = false;
    private SessionParams.RenderingMode myRenderingMode = null;
    private boolean useTransparentBackground = false;
    @NotNull private Function<Module, MergedManifestSnapshot> myManifestProvider =
      module -> {
        try {
          return MergedManifestManager.getMergedManifest(module).get(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
          throw new ProcessCanceledException(e);
        }
        catch (TimeoutException e) {
          getLogger().warn(e);
        }
        catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof ProcessCanceledException) {
            throw (ProcessCanceledException)cause;
          }
          else if (cause instanceof MergedManifestException) {
            getLogger().warn(e);
          }
          else {
            getLogger().error(e);
          }
        }

        return null;
      };

    /**
     * If two RenderTasks share the same ModuleClassLoader they share the same compose framework. This way they share the state. If we would
     * like to control the state of the framework we want to create a dedicated ClassLoader so that the RenderTask has its own compose
     * framework. Having a dedicated ClassLoader also allows for clearing resources right after the RenderTask no longer used.
     */
    private boolean privateClassLoader = false;

    /**
     * Force classes preloading in RenderTask right after creation.
     */
    private Collection<String> classesToPreload = Collections.emptyList();

    /**
     * Additional bytecode transform to apply to project classes when loaded.
     */
    private ClassTransform myAdditionalProjectTransform = ClassTransform.getIdentity();

    /**
     * Additional bytecode transform to apply to non project classes when loaded.
     */
    private ClassTransform myAdditionalNonProjectTransform = ClassTransform.getIdentity();

    /**
     * Handler called when a new class loader has been instantiated. This allows resetting some state that
     * might be specific to the classes currently loaded.
     */
    @NotNull
    private Runnable myOnNewModuleClassLoader = () -> {};

    private RenderTaskBuilder(@NotNull RenderService service,
                              @NotNull AndroidFacet facet,
                              @NotNull Configuration configuration,
                              @NotNull ImagePool defaultImagePool,
                              @NotNull Object credential) {
      myService = service;
      myFacet = facet;
      myConfiguration = configuration;
      myImagePool = defaultImagePool;
      myCredential = credential;
    }


    /**
     * Forces preloading classes in RenderTask after creation.
     */
    @NotNull
    public RenderTaskBuilder preloadClasses(Collection<String> classesToPreload) {
      this.classesToPreload = classesToPreload;
      return this;
    }

    /**
     * Forces the task to create its own ModuleClassLoader instead of using a shared one from the ModuleClassLoaderManager
     */
    @NotNull
    public RenderTaskBuilder usePrivateClassLoader() {
      privateClassLoader = true;
      return this;
    }

    @NotNull
    public RenderTaskBuilder withPsiFile(@NotNull PsiFile psiFile) {
      this.myPsiFile = psiFile;
      return this;
    }

    @NotNull
    public RenderTaskBuilder withLogger(@NotNull RenderLogger logger) {
      this.myLogger = logger;
      return this;
    }

    @NotNull
    public RenderTaskBuilder withParserFactory(@NotNull ILayoutPullParserFactory parserFactory) {
      this.myParserFactory = parserFactory;
      return this;
    }

    public RenderTaskBuilder withLayoutScanner(Boolean enableLayoutScanner) {
      this.enableLayoutScanner = enableLayoutScanner;
      return this;
    }

    public RenderTaskBuilder withLayoutScannerOptimization(Boolean enableScannerOptimization) {
      this.enableLayoutScannerOptimization = enableScannerOptimization;
      return this;
    }

    /**
     * Disables the image pooling for this render task
     */
    @SuppressWarnings("unused")
    @NotNull
    public RenderTaskBuilder disableImagePool() {
      this.myImagePool = ImagePoolFactory.getNonPooledPool();
      return this;
    }

    /**
     * Disables the image pooling for this render task
     */
    @SuppressWarnings("unused")
    @NotNull
    public RenderTaskBuilder withDownscaleFactor(float downscaleFactor) {
      this.myDownscaleFactor = downscaleFactor;
      return this;
    }

    /**
     * @see RenderTask#setMaxRenderSize(int, int)
     */
    @NotNull
    public RenderTaskBuilder withMaxRenderSize(int maxRenderWidth, int maxRenderHeight) {
      myMaxRenderWidth = maxRenderWidth;
      myMaxRenderHeight = maxRenderHeight;
      return this;
    }

    /**
     * Disables the security manager for the {@link RenderTask}.
     * Bazel has its own security manager. We allow rendering tests to disable the security manager by calling this method. If this method
     * is not called from a test method, it will throw an {@link IllegalStateException}
     */
    @TestOnly
    @VisibleForTesting
    @NotNull
    public RenderTaskBuilder disableSecurityManager() {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        throw new IllegalStateException("This method can only be called in unit test mode");
      }
      this.isSecurityManagerEnabled = false;
      return this;
    }

    /**
     * Disables the decorations (status and navigation bars) for the rendered image.
     */
    @NotNull
    public RenderTaskBuilder disableDecorations() {
      this.showDecorations = false;
      return this;
    }

    @NotNull
    public RenderTaskBuilder disableShadow() {
      this.isShadowEnabled = false;
      return this;
    }

    @NotNull
    public RenderTaskBuilder disableHighQualityShadow() {
      this.useHighQualityShadows = false;
      return this;
    }

    @NotNull
    public RenderTaskBuilder disableToolsVisibilityAndPosition() {
      this.showWithToolsVisibilityAndPosition = false;
      return this;
    }

    /**
     * @see RenderTask#setRenderingMode(SessionParams.RenderingMode)
     */
    @NotNull
    public RenderTaskBuilder withRenderingMode(@NotNull SessionParams.RenderingMode renderingMode) {
      myRenderingMode = renderingMode;
      return this;
    }

    /**
     * @see RenderTask#setTransparentBackground()
     */
    @NotNull
    public RenderTaskBuilder useTransparentBackground() {
      useTransparentBackground = true;
      return this;
    }

    /**
     * Sets the {@link MergedManifestSnapshot} provider
     */
    @NotNull
    public RenderTaskBuilder setMergedManifestProvider(@NotNull Function<Module, MergedManifestSnapshot> provider) {
      myManifestProvider = provider;
      return this;
    }

    /**
     * Sets an additional Java bytecode transformation to be applied to the loaded project classes.
     */
    @NotNull
    public RenderTaskBuilder setProjectClassesTransform(@NotNull ClassTransform transform) {
      myAdditionalProjectTransform = transform;
      return this;
    }

    /**
     * Sets an additional Java bytecode transformation to be applied to the loaded non project classes.
     */
    @NotNull
    public RenderTaskBuilder setNonProjectClassesTransform(@NotNull ClassTransform transform) {
      myAdditionalNonProjectTransform = transform;
      return this;
    }

    /**
     * Sets a callback to be notified when a new class loader has been instantiated.
     */
    @NotNull
    public RenderTaskBuilder setOnNewClassLoader(@NotNull Runnable runnable) {
      myOnNewModuleClassLoader = runnable;
      return this;
    }

    /**
     * Builds a new {@link RenderTask}. The returned future always completes successfully but the value might be null if the RenderTask
     * can not be created.
     */
    @NotNull
    public CompletableFuture<RenderTask> build() {
      if (myLogger == null) {
        withLogger(myService.createLogger(myFacet));
      }

      StackTraceCapture stackTraceCaptureElement = RenderTaskAllocationTrackerKt.captureAllocationStackTrace();

      return CompletableFuture.supplyAsync(() -> {
        AndroidPlatform platform = getPlatform(myFacet, myLogger);
        if (platform == null) {
          return null;
        }

        IAndroidTarget target = myConfiguration.getTarget();
        if (target == null) {
          myLogger.addMessage(RenderProblem.createPlain(ERROR, "No render target was chosen"));
          return null;
        }

        Module module = myFacet.getModule();
        if (module.isDisposed()) {
          getLogger().warn("Module was already disposed");
          return null;
        }
        LayoutLibrary layoutLib;
        try {
          layoutLib = platform.getSdkData().getTargetData(target).getLayoutLibrary(module.getProject());
        }
        catch (UnsupportedJavaRuntimeException e) {
          RenderProblem.Html javaVersionProblem = RenderProblem.create(ERROR);
          javaVersionProblem.getHtmlBuilder()
            .add(e.getPresentableMessage())
            .newline()
            .addLink("Install a supported JDK", JDK_INSTALL_URL);
          myLogger.addMessage(javaVersionProblem);
          return null;
        }
        catch (RenderingException e) {
          String message = e.getPresentableMessage();
          message = message != null ? message : AndroidBundle.message("android.layout.preview.default.error.message");
          myLogger.addMessage(RenderProblem.createPlain(ERROR, message, module.getProject(), myLogger.getLinkManager(), e));
          return null;
        }

        Device device = myConfiguration.getDevice();
        if (device == null) {
          myLogger.addMessage(RenderProblem.createPlain(ERROR, "No device selected"));
          return null;
        }

        try {
          RenderTask task =
            new RenderTask(myFacet, myService, myConfiguration, myLogger, layoutLib,
                           device, myCredential, StudioCrashReporter.getInstance(), myImagePool,
                           myParserFactory, isSecurityManagerEnabled, myDownscaleFactor, stackTraceCaptureElement, myManifestProvider,
                           privateClassLoader, myAdditionalProjectTransform, myAdditionalNonProjectTransform, myOnNewModuleClassLoader,
                           classesToPreload);
          if (myPsiFile instanceof XmlFile) {
            task.setXmlFile((XmlFile)myPsiFile);
          }

          task
            .setDecorations(showDecorations)
            .setHighQualityShadows(useHighQualityShadows)
            .setShadowEnabled(isShadowEnabled)
            .setShowWithToolsVisibilityAndPosition(showWithToolsVisibilityAndPosition)
            .setEnableLayoutScanner(enableLayoutScanner)
            .setEnableLayoutScannerOptimization(enableLayoutScannerOptimization);

          if (myMaxRenderWidth != -1 && myMaxRenderHeight != -1) {
            task.setMaxRenderSize(myMaxRenderWidth, myMaxRenderHeight);
          }

          if (useTransparentBackground) {
            task.setTransparentBackground();
          }

          if (myRenderingMode != null) {
            task.setRenderingMode(myRenderingMode);
          }

          return task;
        } catch (IllegalStateException | IncorrectOperationException | AssertionError e) {
          // Ignore the exception if it was generated when the facet is being disposed (project is being closed)
          if (!module.isDisposed()) {
            throw e;
          }
        }

        return null;
      }, AppExecutorUtil.getAppExecutorService());
    }

    /**
     * Builds a new {@link RenderTask}.
     * @deprecated Use {@link RenderTaskBuilder#build}
     */
    @Deprecated
    @Nullable
    public RenderTask buildSynchronously() {
      return Futures.getUnchecked(build());
    }
  }
}
