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

import com.android.annotations.concurrency.GuardedBy
import com.android.annotations.concurrency.Slow
import com.android.tools.idea.compose.preview.util.PreviewElement
import com.android.tools.idea.compose.preview.util.PreviewElementInstance
import com.intellij.openapi.util.ModificationTracker
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Interface to be implemented by classes providing a list of [PreviewElement]
 */
interface PreviewElementProvider<P: PreviewElement> {
  val previewElements: Sequence<P>
}

interface PreviewElementInstanceProvider: PreviewElementProvider<PreviewElementInstance> {
  override val previewElements: Sequence<PreviewElementInstance>
}

/**
 * [PreviewElementProvider] that does not contain [PreviewElement]s.
 */
object EmptyPreviewElementInstanceProvider : PreviewElementInstanceProvider {
  override val previewElements: Sequence<PreviewElementInstance> = emptySequence()
}

/**
 * Returns the list of available group names in this [PreviewElementProvider]. If this provider does any filtering, the groups
 * returned here will be the ones after the filtering is applied.
 */
val Sequence<PreviewElement>.groupNames: Set<String>
  get() = mapNotNull { it.displaySettings.group }
    .filter { it.isNotBlank() }
    .toSet()

/**
 * A [PreviewElementProvider] that applies a filter to the result.
 */
class FilteredPreviewElementProvider<P: PreviewElement>(private val delegate: PreviewElementProvider<P>,
                                     private val filter: (P) -> Boolean) : PreviewElementProvider<P> {
  override val previewElements: Sequence<P>
    get() = delegate.previewElements.filter(filter)
}

/**
 * A [PreviewElementProvider] for dealing with [PreviewElementProvider] that might be @[Slow]. This [PreviewElementProvider] contents
 * will only be updated when the given [modificationTracker] updates.
 */
class MemoizedPreviewElementProvider<P: PreviewElement>(private val delegate: PreviewElementProvider<P>,
                                     private val modificationTracker: ModificationTracker) : PreviewElementProvider<P> {
  private var savedModificationStamp = -1L
  private val cachedPreviewElementLock = ReentrantReadWriteLock()
  @GuardedBy("cachedPreviewElementLock")
  private var cachedPreviewElements: Collection<P> = emptyList()

  /**
   * Refreshes the [previewElements]. Do not call on the UI thread.
   */
  @Synchronized
  @Slow
  private fun refreshIfNeeded() {
    val newModificationStamp = modificationTracker.modificationCount

    if (newModificationStamp != savedModificationStamp) {
      cachedPreviewElementLock.write {
        cachedPreviewElements = delegate.previewElements.toList()
      }
      savedModificationStamp = newModificationStamp
    }
  }

  /**
   * Returns the latest value of the [PreviewElement]s contained in the [delegate]. If the [modificationTracker] has not changed,
   * this property will return a cached value.
   *
   * _This call might be [Slow]. Do not call on the UI thread._
   */
  override val previewElements: Sequence<P>
    @Slow get() {
      refreshIfNeeded()
      cachedPreviewElementLock.read {
        return cachedPreviewElements.asSequence()
      }
    }
}

/**
 * A [PreviewElementProvider] that allows combining multiple [PreviewElementProvider]s into one.
 */
class CombinedPreviewElementProvider<P: PreviewElement>(private val previewElementProviders: Collection<PreviewElementProvider<P>>): PreviewElementProvider<P> {
  override val previewElements: Sequence<P>
    get() = previewElementProviders.asSequence().flatMap { it.previewElements }
}