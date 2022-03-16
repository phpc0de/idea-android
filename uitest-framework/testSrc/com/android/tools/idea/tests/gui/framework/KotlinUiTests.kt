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
package com.android.tools.idea.tests.gui.framework

import com.intellij.openapi.util.Ref
import org.fest.swing.core.ComponentFinder
import org.fest.swing.core.GenericTypeMatcher
import org.fest.swing.edt.GuiQuery
import org.fest.swing.exception.ComponentLookupException
import org.fest.swing.fixture.ContainerFixture
import org.fest.swing.timing.Wait
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JDialog

fun ContainerFixture<*>.finder() = this.robot().finder()

interface DialogContainerFixture : ContainerFixture<JDialog> {
  fun maybeRestoreLostFocus()
}

inline fun <reified T : JComponent> matcher(requireShowing: Boolean = true, crossinline predicate: (T) -> Boolean): GenericTypeMatcher<T> =
    object : GenericTypeMatcher<T>(T::class.java, requireShowing) {
      override fun isMatching(component: T): Boolean = predicate(component)
    }

inline fun <reified T : JComponent> ComponentFinder.findByType(root: Container) = findByType(root, T::class.java, true)
inline fun <reified T : JComponent> ComponentFinder.findByLabel(root: Container, label: String) =
    findByLabel(root, label, T::class.java, true)

inline fun <reified T : JComponent> ComponentFinder.find(
    root: Container, requireShowing: Boolean = true,
    crossinline predicate: (T) -> Boolean
) =
    find(root, matcher<T>(requireShowing, predicate))

fun <T> tryFind(finder: () -> T): T? = try {
  finder()
}
catch (_: ComponentLookupException) {
  null
}

fun <T> waitUntilFound(message: String, finder: () -> T?, secondsToWait: Long = 1L): T {
  val reference = Ref<T>()
  Wait.seconds(secondsToWait).expecting(message).until { GuiQuery.get { finder()?.let { reference.set(it) } } != null }
  return reference.get()
}
