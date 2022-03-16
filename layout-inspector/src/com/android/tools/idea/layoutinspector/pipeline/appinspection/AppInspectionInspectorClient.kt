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
package com.android.tools.idea.layoutinspector.pipeline.appinspection

import com.android.ddmlib.AndroidDebugBridge
import com.android.tools.idea.appinspection.api.AppInspectionApiServices
import com.android.tools.idea.appinspection.ide.AppInspectionDiscoveryService
import com.android.tools.idea.appinspection.inspector.api.process.ProcessDescriptor
import com.android.tools.idea.concurrency.coroutineScope
import com.android.tools.idea.concurrency.createChildScope
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.layoutinspector.metrics.LayoutInspectorMetrics
import com.android.tools.idea.layoutinspector.metrics.statistics.SessionStatistics
import com.android.tools.idea.layoutinspector.model.AndroidWindow
import com.android.tools.idea.layoutinspector.model.InspectorModel
import com.android.tools.idea.layoutinspector.pipeline.AbstractInspectorClient
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient.Capability
import com.android.tools.idea.layoutinspector.pipeline.InspectorClientSettings
import com.android.tools.idea.layoutinspector.pipeline.TreeLoader
import com.android.tools.idea.layoutinspector.pipeline.appinspection.compose.ComposeLayoutInspectorClient
import com.android.tools.idea.layoutinspector.pipeline.appinspection.view.ViewLayoutInspectorClient
import com.android.tools.idea.layoutinspector.properties.PropertiesProvider
import com.android.tools.idea.layoutinspector.skia.SkiaParserImpl
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.wireless.android.sdk.stats.DynamicLayoutInspectorEvent.DynamicLayoutInspectorEventType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import layoutinspector.view.inspection.LayoutInspectorViewProtocol
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.util.EnumSet

/**
 * An [InspectorClient] that talks to an app-inspection based inspector running on a target device.
 *
 * @param apiServices App inspection services used for initializing and shutting down app
 *     inspection-based inspectors.
 * @param scope App inspection APIs use coroutines, while this class's interface does not, so this
 *     coroutine scope is used to handle the bridge between the two approaches.
 */
class AppInspectionInspectorClient(
  adb: AndroidDebugBridge,
  process: ProcessDescriptor,
  private val model: InspectorModel,
  stats: SessionStatistics,
  @TestOnly private val apiServices: AppInspectionApiServices = AppInspectionDiscoveryService.instance.apiServices,
  @TestOnly private val scope: CoroutineScope = model.project.coroutineScope.createChildScope(true),
) : AbstractInspectorClient(process) {

  private lateinit var viewInspector: ViewLayoutInspectorClient
  private lateinit var propertiesProvider: AppInspectionPropertiesProvider

  /** Compose inspector, may be null if user's app isn't using the compose library. */
  @VisibleForTesting
  var composeInspector: ComposeLayoutInspectorClient? = null
    private set

  private val exceptionHandler = CoroutineExceptionHandler { _, t ->
    fireError(t.message!!)
  }

  private val debugViewAttributes = DebugViewAttributes(adb, model.project, process)

  private val metrics = LayoutInspectorMetrics.create(model.project, process, stats)

  override val capabilities =
    EnumSet.of(Capability.SUPPORTS_CONTINUOUS_MODE,
               Capability.SUPPORTS_FILTERING_SYSTEM_NODES,
               Capability.SUPPORTS_SYSTEM_NODES,
               Capability.SUPPORTS_SKP)!!

  private val skiaParser = SkiaParserImpl(
    {
      viewInspector.updateScreenshotType(LayoutInspectorViewProtocol.Screenshot.Type.BITMAP)
      capabilities.remove(Capability.SUPPORTS_SKP)
    })

  override val treeLoader: TreeLoader = AppInspectionTreeLoader(
    model.project,
    logEvent = metrics::logEvent,
    skiaParser
  )
  override val provider: PropertiesProvider
    get() = propertiesProvider
  override val isCapturing: Boolean
    get() = InspectorClientSettings.isCapturingModeOn

  override fun doConnect(): ListenableFuture<Nothing> {
    val future = SettableFuture.create<Nothing>()
    val connectFailureHandler = CoroutineExceptionHandler { _, t -> future.setException(t) }

    scope.launch(connectFailureHandler) {
      metrics.logEvent(DynamicLayoutInspectorEventType.ATTACH_REQUEST)

      if (StudioFlags.DYNAMIC_LAYOUT_INSPECTOR_ENABLE_COMPOSE_SUPPORT.get()) {
        composeInspector = ComposeLayoutInspectorClient.launch(apiServices, process, model)
      }

      viewInspector = ViewLayoutInspectorClient.launch(apiServices, process, model, scope, composeInspector, ::fireError, ::fireTreeEvent)
      propertiesProvider = AppInspectionPropertiesProvider(viewInspector, composeInspector, model)

      metrics.logEvent(DynamicLayoutInspectorEventType.ATTACH_SUCCESS)

      debugViewAttributes.set()

      if (isCapturing) {
        startFetching()
      }
      else {
        refresh()
      }

      future.set(null)
    }
    return future
  }

  override fun doDisconnect(): ListenableFuture<Nothing> {
    val future = SettableFuture.create<Nothing>()
    scope.launch(exceptionHandler) {
      debugViewAttributes.clear()
      viewInspector.disconnect()
      composeInspector?.disconnect()
      skiaParser.shutdown()
      metrics.logEvent(DynamicLayoutInspectorEventType.SESSION_DATA)

      future.set(null)
    }
    return future
  }

  override fun startFetching() {
    scope.launch(exceptionHandler) {
      viewInspector.startFetching(continuous = true)
    }
  }

  override fun stopFetching() {
    scope.launch(exceptionHandler) {
      // Reset the scale to 1 to support zooming while paused, and get an SKP if possible.
      if (capabilities.contains(Capability.SUPPORTS_SKP)) {
        updateScreenshotType(AndroidWindow.ImageType.SKP, 1.0f)
      }
      else {
        viewInspector.updateScreenshotType(null, 1.0f)
      }
      viewInspector.stopFetching()
    }
  }

  override fun refresh() {
    scope.launch(exceptionHandler) {
      viewInspector.startFetching(continuous = false)
    }
  }

  override fun updateScreenshotType(type: AndroidWindow.ImageType?, scale: Float) {
    if (model.pictureType != type || scale >= 0f) {
      viewInspector.updateScreenshotType(type?.protoType, scale)
    }
  }

  override fun addDynamicCapabilities(dynamicCapabilities: Set<Capability>) {
    capabilities.addAll(dynamicCapabilities)
  }
}
