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
package com.android.tools.idea.compose.preview.animation

import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import com.android.annotations.concurrency.GuardedBy
import com.android.annotations.concurrency.Slow
import com.android.tools.idea.common.surface.DesignSurface
import com.android.tools.idea.compose.preview.analytics.AnimationToolingEvent
import com.android.tools.idea.compose.preview.analytics.AnimationToolingUsageTracker
import com.android.tools.idea.compose.preview.animation.ComposePreviewAnimationManager.onAnimationSubscribed
import com.android.tools.idea.compose.preview.animation.ComposePreviewAnimationManager.onAnimationUnsubscribed
import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.MoreExecutors
import com.google.wireless.android.sdk.stats.ComposeAnimationToolingEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.UIUtil

private val LOG = Logger.getInstance(ComposePreviewAnimationManager::class.java)

/**
 * Responsible for opening the [AnimationInspectorPanel] and managing its state. `PreviewAnimationClockMethodTransform` intercepts calls to
 * `subscribe` and `unsubscribe` calls on `ui-tooling` and redirects them to [onAnimationSubscribed] and [onAnimationUnsubscribed],
 * respectively. These methods will then update the [AnimationInspectorPanel] content accordingly, adding tabs for newly subscribed
 * animations and closing tabs corresponding to unsubscribed animations.
 */
object ComposePreviewAnimationManager {
  @get:VisibleForTesting
  var currentInspector: AnimationInspectorPanel? = null
    private set

  @get:VisibleForTesting
  @GuardedBy("subscribedAnimationsLock")
  val subscribedAnimations = mutableSetOf<ComposeAnimation>()
  private val subscribedAnimationsLock = Any()

  private var newInspectorOpenedCallback: (() -> Unit)? = null

  internal val onSubscribedUnsubscribedExecutor =
    if (ApplicationManager.getApplication().isUnitTestMode)
      MoreExecutors.directExecutor()
    else
      AppExecutorUtil.createBoundedApplicationPoolExecutor("Animation Subscribe/Unsubscribe Callback Handler", 1)

  @Slow
  fun createAnimationInspectorPanel(surface: DesignSurface, parent: Disposable, onNewInspectorOpen: () -> Unit): AnimationInspectorPanel {
    newInspectorOpenedCallback = onNewInspectorOpen
    AnimationToolingUsageTracker.getInstance(surface).logEvent(AnimationToolingEvent(
      ComposeAnimationToolingEvent.ComposeAnimationToolingEventType.OPEN_ANIMATION_INSPECTOR
    ))
    return invokeAndWaitIfNeeded {
      val animationInspectorPanel = AnimationInspectorPanel(surface)
      Disposer.register(parent, animationInspectorPanel)
      currentInspector = animationInspectorPanel
      animationInspectorPanel
    }
  }

  fun closeCurrentInspector() {
    currentInspector?.let {
      Disposer.dispose(it)
      AnimationToolingUsageTracker.getInstance(it.surface).logEvent(AnimationToolingEvent(
        ComposeAnimationToolingEvent.ComposeAnimationToolingEventType.CLOSE_ANIMATION_INSPECTOR
      ))
    }
    currentInspector = null
    newInspectorOpenedCallback = null
    synchronized(subscribedAnimationsLock) {
      subscribedAnimations.clear()
    }
  }

  /**
   * Expected to be called just before opening an inspector, e.g. before triggering the toolbar action. Invokes [newInspectorOpenedCallback]
   * to handle potential operations that need to be done before opening the new inspector, e.g. closing another one that might be open.
   */
  fun onAnimationInspectorOpened() {
    newInspectorOpenedCallback?.invoke()
  }

  /**
   * Sets the panel clock, adds the animation to the subscribed list, and creates the corresponding tab in the [AnimationInspectorPanel].
   */
  fun onAnimationSubscribed(clock: Any?, animation: ComposeAnimation) {
    if (clock == null) return
    val inspector = currentInspector ?: return
    if (inspector.animationClock == null) {
      inspector.animationClock = AnimationClock(clock)
    }

    // Handle the case where the clock has changed while the inspector is open. That might happen when we were still processing a
    // subscription from a previous inspector when the new inspector was open. In this case, we should unsubscribe all the previously
    // subscribed animations, as they were tracked by the previous clock.
    inspector.animationClock?.let {
      if (it.clock != clock) {
        // Make a copy of the list to prevent ConcurrentModificationException
        synchronized(subscribedAnimationsLock) { subscribedAnimations.toSet() }.forEach { animationToUnsubscribe ->
          onAnimationUnsubscribed(animationToUnsubscribe)
        }
        // After unsubscribing the old animations, update the clock
        inspector.animationClock = AnimationClock(clock)
      }
    }

    if (synchronized(subscribedAnimationsLock) { subscribedAnimations.add(animation) }) {
      UIUtil.invokeLaterIfNeeded {
        // Create the tab corresponding to the animation
        inspector.createTab(animation)
        if (animation.type == ComposeAnimationType.TRANSITION_ANIMATION) {
          // For transition animations, make sure to populate the tab with the states and animated properties.
          inspector.updateTransitionStates(animation, handleKnownStateTypes(animation.states)) {
            // When the tab is populated, add it to the animation inspector
            UIUtil.invokeLaterIfNeeded { inspector.addTab(animation) }
          }
        }
        else {
          // TODO(b/170428636): populate tab before adding it for ANIMATED_VALUE animations.
          inspector.addTab(animation)
        }
      }
    }
  }

  /**
   * Removes the animation from the subscribed list and removes the corresponding tab in the [AnimationInspectorPanel].
   */
  fun onAnimationUnsubscribed(animation: ComposeAnimation) {
    if (synchronized(subscribedAnimationsLock) { subscribedAnimations.remove(animation) }) {
      UIUtil.invokeLaterIfNeeded {
        currentInspector?.removeTab(animation)
      }
    }
  }

  /**
   * Whether the animation inspector is open.
   */
  fun isInspectorOpen() = currentInspector != null

  /**
   * Invalidates the current animation inspector, so it doesn't display animations out-of-date.
   */
  fun invalidate() {
    currentInspector?.let { UIUtil.invokeLaterIfNeeded { it.invalidatePanel() } }
  }

  /**
   * Due to a limitation in the Compose Animation framework, we might not know all the available states for a given animation, only the
   * initial/current one. However, we can infer all the states based on the initial one depending on its type, e.g. for a boolean we know
   * the available states are only `true` or `false`.
   */
  private fun handleKnownStateTypes(originalStates: Set<Any>) = when (originalStates.iterator().next()) {
    is Boolean -> setOf(true, false)
    else -> originalStates
  }
}

@Suppress("unused") // Called via reflection from PreviewAnimationClockMethodTransform
fun animationSubscribed(clock: Any?, animation: Any?) {
  if (LOG.isDebugEnabled) {
    LOG.debug("Animation subscribed: $animation")
  }
  ComposePreviewAnimationManager.onSubscribedUnsubscribedExecutor.execute {
    (animation as? ComposeAnimation)?.let { onAnimationSubscribed(clock, it) }
  }
}

@Suppress("unused") // Called via reflection from PreviewAnimationClockMethodTransform
fun animationUnsubscribed(animation: Any?) {
  if (LOG.isDebugEnabled) {
    LOG.debug("Animation unsubscribed: $animation")
  }
  if (animation == null) return
  ComposePreviewAnimationManager.onSubscribedUnsubscribedExecutor.execute {
    (animation as? ComposeAnimation)?.let { onAnimationUnsubscribed(it) }
  }
}
