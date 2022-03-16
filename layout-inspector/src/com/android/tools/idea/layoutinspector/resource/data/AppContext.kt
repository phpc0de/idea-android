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
package com.android.tools.idea.layoutinspector.resource.data

/**
 * Misc. context about the current running app.
 */
class AppContext(
  val apiLevel: Int = 0, // Key for a StringTable
  val apiCodeName: Int = 0, // Key for a StringTable
  val appPackageName: Int = 0, // Key for a StringTable
  val theme: Resource = Resource(),
  val configuration: Configuration = Configuration()
)