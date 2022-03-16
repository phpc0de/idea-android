/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.compose.preview

import com.android.tools.idea.compose.preview.ComposePreviewBundle.message
import com.android.tools.idea.compose.preview.util.PreviewElementInstance
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

/**
 * Class representing groups available for selection in the [ComposePreviewManager].
 */
@Suppress("DataClassPrivateConstructor")
data class PreviewGroup private constructor(
  val displayName: String,
  val icon: Icon?,
  val name: String?
) {
  companion object {
    fun namedGroup(displayName: String, icon: Icon? = null, name: String = displayName): PreviewGroup = PreviewGroup(displayName, icon,
                                                                                                                     name)

    /**
     * [PreviewGroup] to be used when no filtering is to be applied to the preview.
     */
    val ALL_PREVIEW_GROUP = PreviewGroup(
      displayName = message("group.switch.all"),
      icon = null,
      name = null
    )
  }
}

/**
 * Interface that provides access to the Compose Preview logic.
 */
interface ComposePreviewManager {
  /**
   * Enum that determines the current status of the interactive preview.
   *
   * The transitions are are like:
   * DISABLED -> STARTED -> READY -> STOPPING
   *    ^                               +
   *    |                               |
   *    +-------------------------------+
   */
  enum class InteractiveMode {
    DISABLED,
    /** Status when interactive has been started but the first render has not happened yet. */
    STARTING,
    /** Interactive is ready and running. */
    READY,
    /** The interactive preview is stopping but it has not been fully disposed yet. */
    STOPPING;

    fun isStartingOrReady() = this == STARTING || this == READY
  }
  /**
   * Status of the preview.
   *
   * @param hasRuntimeErrors true if the project has any runtime errors that prevent the preview being up to date.
   *  For example missing classes.
   * @param hasSyntaxErrors true if the preview is displaying content of a file that has syntax errors.
   * @param isOutOfDate true if the preview needs a refresh to be up to date.
   * @param isRefreshing true if the view is currently refreshing.
   * @param interactiveMode represents current state of preview interactivity.
   */
  data class Status(val hasRuntimeErrors: Boolean,
                    val hasSyntaxErrors: Boolean,
                    val isOutOfDate: Boolean,
                    val isRefreshing: Boolean,
                    val interactiveMode: InteractiveMode) {
    /**
     * True if the preview has errors that will need a refresh
     */
    val hasErrors = hasRuntimeErrors || hasSyntaxErrors
  }

  fun status(): Status

  /**
   * When true, a build will automatically be triggered when the user makes a source code change.
   */
  var isBuildOnSaveEnabled: Boolean

  /**
   * List of available groups in this preview. The editor can contain multiple groups and only will be displayed at a given time.
   */
  val availableGroups: Collection<PreviewGroup>

  /**
   * Group name from [availableGroups] currently selected or null if we do not want to do group filtering.
   */
  var groupFilter: PreviewGroup

  /**
   * Represents the [PreviewElementInstance] open in the Interactive Preview. Null if no preview is in interactive mode.
   */
  var interactivePreviewElementInstance: PreviewElementInstance?

  /**
   * Represents the [PreviewElementInstance] open in the Animation Inspector. Null if no preview is being inspected.
   */
  var animationInspectionPreviewElementInstance: PreviewElementInstance?

  /**
   * Returns true if the current preview has the live literals feature available.
   */
  val hasLiveLiterals: Boolean

  /**
   * Enables/disables live literals in the preview.
   */
  val isLiveLiteralsEnabled: Boolean

  /**
   * When true, the ComposeViewAdapter will search for Composables that can return a DesignInfo object.
   */
  val hasDesignInfoProviders: Boolean
}

val ComposePreviewManager.isInStaticAndNonAnimationMode: Boolean
  get() = animationInspectionPreviewElementInstance == null && status().interactiveMode == ComposePreviewManager.InteractiveMode.DISABLED

/**
 * Interface that provides access to the Compose Preview logic that is not stable or meant for public
 * use.
 * This interface contains only temporary or experimental methods.
 */
@ApiStatus.Experimental
interface ComposePreviewManagerEx : ComposePreviewManager {
  /**
   * If enabled, the bounds for the different `@Composable` elements will be displayed in the surface.
   */
  var showDebugBoundaries: Boolean
}