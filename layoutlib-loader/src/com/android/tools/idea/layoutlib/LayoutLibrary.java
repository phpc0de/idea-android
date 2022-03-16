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

package com.android.tools.idea.layoutlib;

import com.android.ide.common.rendering.api.Bridge;
import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.Result.Status;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.sdk.LoadStatus;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Class to use the Layout library.
 * <p>
 * Use {@link #load(Bridge, ClassLoader)} to get an instance
 * <p>
 * Use the layout library with:
 * {@link #init}, {@link #createSession(SessionParams)},
 * {@link #dispose()}, {@link #clearResourceCaches(Object)}.
 */
public class LayoutLibrary implements Disposable {
    public static final String LAYOUTLIB_NATIVE_PLUGIN = "com.android.layoutlib";
    public static final String LAYOUTLIB_STANDARD_PLUGIN = "com.android.layoutlib.legacy";

    /** Link to the layout bridge */
    private final Bridge mBridge;
    /** classloader used to load the jar file */
    private final ClassLoader mClassLoader;

    private boolean mIsDisposed;

    /**
     * Returns the classloader used to load the classes in the layoutlib jar file.
     */
    public ClassLoader getClassLoader() {
        return mClassLoader;
    }

    /**
     * Returns a {@link LayoutLibrary} instance using the given {@link Bridge} and {@link ClassLoader}
     */
    static LayoutLibrary load(Bridge bridge, ClassLoader classLoader) {
        return new LayoutLibrary(bridge, classLoader);
    }

    private LayoutLibrary(Bridge bridge,  ClassLoader classLoader) {
        mBridge = bridge;
        mClassLoader = classLoader;
    }

    public static boolean isNative() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return true;
        }
        IdeaPluginDescriptor nativePlugin = PluginManagerCore.getPlugin(PluginId.findId(LAYOUTLIB_NATIVE_PLUGIN));
        return nativePlugin != null && nativePlugin.isEnabled();
    }

    // ------ Layout Lib API proxy

    /**
     * Initializes the Layout Library object. This must be called before any other action is taken
     * on the instance.
     *
     * @param platformProperties The build properties for the platform.
     * @param fontLocation the location of the fonts in the SDK target.
     * @param nativeLibDirPath the absolute path of the directory containing all the native libraries for layoutlib.
     * @param icuDataPath the location of the ICU data used natively.
     * @param enumValueMap map attrName ⇒ { map enumFlagName ⇒ Integer value }. This is typically
     *          read from attrs.xml in the SDK target.
     * @param log a {@link ILayoutLog} object. Can be null.
     * @return true if success.
     */
    public boolean init(Map<String, String> platformProperties,
                        File fontLocation,
                        String nativeLibDirPath,
                        String icuDataPath,
                        Map<String, Map<String, Integer>> enumValueMap,
                        ILayoutLog log) {
        if (mBridge != null) {
            return mBridge.init(platformProperties, fontLocation, nativeLibDirPath, icuDataPath, enumValueMap, log);
        }

        return false;
    }

    /**
     * Prepares the layoutlib to unloaded.
     *
     * @see Bridge#dispose()
     */
    @Override
    public void dispose() {
        if (mBridge != null) {
            mIsDisposed = mBridge.dispose();
        }
    }

    public boolean isDisposed() {
        return mIsDisposed;
    }

    /**
     * Starts a layout session by inflating and rendering it. The method returns a
     * {@link RenderSession} on which further actions can be taken.
     * <p>
     *
     * @return a new {@link RenderSession} object that contains the result of the scene creation and
     * first rendering or null if {@link #getStatus()} doesn't return {@link LoadStatus#LOADED}.
     *
     * @see Bridge#createSession(SessionParams)
     */
    public RenderSession createSession(SessionParams params) {
        if (mBridge != null) {
            return mBridge.createSession(params);
        }

        return null;
    }

    /**
     * Renders a Drawable. If the rendering is successful, the result image is accessible through
     * {@link Result#getData()}. It is of type {@link BufferedImage}
     * @param params the rendering parameters.
     * @return the result of the action.
     */
    public Result renderDrawable(DrawableParams params) {
        if (mBridge != null) {
            return mBridge.renderDrawable(params);
        }

        return Status.NOT_IMPLEMENTED.createResult();
    }

    /**
     * Clears the resource cache for a specific project.
     * <p>This cache contains bitmaps and nine patches that are loaded from the disk and reused
     * until this method is called.
     * <p>The cache is not configuration dependent and should only be cleared when a
     * resource changes (at this time only bitmaps and 9 patches go into the cache).
     *
     * @param projectKey the key for the project.
     *
     * @see Bridge#clearResourceCaches(Object)
     */
    public void clearResourceCaches(Object projectKey) {
        if (mBridge != null) {
            mBridge.clearResourceCaches(projectKey);
        }
    }

    /**
     * Removes a font from the Typeface cache inside layoutlib.
     *
     * @param path the path of the font file to be removed from the cache.
     */
    public void clearFontCache(String path) {
        if (mBridge != null) {
            mBridge.clearFontCache(path);
        }
    }

    /**
     * Clears all caches for a specific project.
     *
     * @param projectKey the key for the project.
     */
    public void clearAllCaches(Object projectKey) {
        if (mBridge != null) {
            mBridge.clearAllCaches(projectKey);
        }
    }

    /**
     * Utility method returning the parent of a given view object.
     *
     * @param viewObject the object for which to return the parent.
     *
     * @return a {@link Result} indicating the status of the action, and if success, the parent
     *      object in {@link Result#getData()}
     */
    public Result getViewParent(Object viewObject) {
        if (mBridge != null) {
            return mBridge.getViewParent(viewObject);
        }
        return Status.ERROR_UNKNOWN.createResult();
    }

    /**
     * Returns true if the character orientation of the locale is right to left.
     * @param locale The locale formatted as language-region
     * @return true if the locale is right to left.
     */
    public boolean isRtl(String locale) {
        return mBridge != null && mBridge.isRtl(locale);
    }

    @VisibleForTesting
    protected LayoutLibrary() {
        mBridge = null;
        mClassLoader = null;
    }
}
