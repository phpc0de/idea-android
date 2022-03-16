/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.schedule.filters

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.databinding.ObservableBoolean
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.EventFilterCategory.NONE
import com.google.samples.apps.iosched.util.hasSameValue

sealed class EventFilter(isChecked: Boolean) {

    enum class EventFilterCategory(@StringRes val resId: Int) {
        NONE(0),
        TOPICS(R.string.category_heading_tracks),
        EVENT_TYPES(R.string.category_heading_types)
    }

    val isChecked = ObservableBoolean(isChecked)

    /** Determines the category heading to show in the filters sheet. */
    abstract fun getFilterCategory(): EventFilterCategory

    /** Return the background color when filled. */
    abstract fun getColor(): Int

    /** Return a color to use when the filter is selected, or TRANSPARENT to use the default. */
    open fun getSelectedTextColor(): Int = Color.TRANSPARENT

    /** Return a string resource to display, or 0 to use the value of [getText]. */
    open fun getTextResId(): Int = 0

    /** Return a string to display when [getTextResId] returns 0. */
    open fun getText(): String = ""

    /** Return a short string resource to display, or 0 to use the value of [getShortText]. */
    open fun getShortTextResId(): Int = 0

    /** Return a short string string to display when [getShortTextResId] returns 0. */
    open fun getShortText(): String = ""

    /** Filter for user's starred and reserved events. */
    class MyEventsFilter(isChecked: Boolean) : EventFilter(isChecked) {

        override fun getFilterCategory(): EventFilterCategory = NONE

        override fun getColor(): Int = Color.parseColor("#4768fd") // @color/indigo

        override fun getSelectedTextColor(): Int = Color.WHITE

        override fun getTextResId(): Int = R.string.starred_and_reserved

        override fun getShortTextResId(): Int = R.string.starred_and_reserved_short

        override fun equals(other: Any?): Boolean = other is MyEventsFilter
    }

    /** Filter for event tags. */
    class TagFilter(val tag: Tag, isChecked: Boolean) : EventFilter(isChecked) {

        override fun getFilterCategory(): EventFilterCategory {
            return when (tag.category) {
                Tag.CATEGORY_TOPIC -> EventFilterCategory.TOPICS
                Tag.CATEGORY_TYPE -> EventFilterCategory.EVENT_TYPES
                else -> throw IllegalArgumentException("unsupported tag type in filters")
            }
        }

        override fun getColor(): Int = tag.color

        override fun getSelectedTextColor(): Int = tag.fontColor ?: super.getSelectedTextColor()

        override fun getTextResId(): Int = 0
        override fun getShortTextResId(): Int = 0

        override fun getText(): String = tag.name
        override fun getShortText(): String = tag.name

        /** Only the tag is used for equality. */
        override fun equals(other: Any?) =
            this === other || (other is TagFilter && other.tag == tag)

        /** Only the tag is used for equality. */
        override fun hashCode() = tag.hashCode()

        // for DiffCallback
        fun isUiContentEqual(other: TagFilter) =
            tag.isUiContentEqual(other.tag) && isChecked.hasSameValue(other.isChecked)
    }
}
