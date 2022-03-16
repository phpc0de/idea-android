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
package com.android.tools.idea.rendering;

import com.android.sdklib.IAndroidTarget;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.res.ResourceClassRegistry;
import com.android.tools.idea.res.ResourceIdManager;
import com.android.tools.idea.res.ResourceRepositoryManager;
import com.android.tools.idea.ui.designer.DesignSurfaceNotificationManager;
import com.android.tools.idea.ui.designer.EditorDesignSurface;
import com.google.common.collect.ImmutableCollection;
import com.intellij.openapi.module.Module;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidTargetData;
import org.jetbrains.android.uipreview.ModuleClassLoaderManager;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderUtils {
  public static void clearCache(@NotNull ImmutableCollection<Configuration> configurations) {
    configurations
      .forEach(configuration -> {
        // Clear layoutlib bitmap cache (in case files have been modified externally)
        IAndroidTarget target = configuration.getTarget();
        Module module = configuration.getModule();
        if (module != null) {
          ModuleClassLoaderManager.get().clearCache(module);
          ResourceIdManager.get(module).resetDynamicIds();
          ResourceClassRegistry.get(module.getProject()).clearCache();
          if (target != null) {
            AndroidTargetData targetData = AndroidTargetData.getTargetData(target, module);
            if (targetData != null) {
              targetData.clearAllCaches(module);
            }
          }

          // Reset resources for the current module and all the dependencies
          AndroidFacet facet = AndroidFacet.getInstance(module);
          Stream.concat(AndroidUtils.getAllAndroidDependencies(module, true).stream(), Stream.of(facet))
            .filter(Objects::nonNull)
            .forEach(f -> ResourceRepositoryManager.getInstance(f).resetAllCaches());
        }
      });
  }

  /** Refresh the design surface, and send notification if it's supported. */
  public static void refreshRenderAndNotify(
    @NotNull EditorDesignSurface surface, @Nullable DesignSurfaceNotificationManager notification) {
      if (notification != null) {
        notification.showNotification("Start refreshing the layout.");
      }
      clearCache(surface.getConfigurations());

      surface.forceUserRequestedRefresh().thenAccept(unused -> {
        if (notification != null) {
          notification.showThenHideNotification("Finished refreshing the layout.", 2000);
        }
      });
  }
}
