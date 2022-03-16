/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.compose.debug

import com.intellij.debugger.PositionManager
import com.intellij.debugger.PositionManagerFactory
import com.intellij.debugger.engine.DebugProcess
import org.jetbrains.kotlin.idea.debugger.KotlinPositionManager
import org.jetbrains.kotlin.idea.debugger.KotlinPositionManagerFactory

class ComposePositionManagerFactory : PositionManagerFactory() {
  override fun createPositionManager(process: DebugProcess): PositionManager {
    return ComposePositionManager(process, KotlinPositionManager(process))
  }
}
