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
package com.android.tools.idea.gradle.structure.configurables.issues

import com.android.tools.idea.gradle.structure.model.PsIssue
import com.android.tools.idea.gradle.structure.model.PsPath

interface IssueRenderer {
  fun renderIssue(buffer: StringBuilder, issue: PsIssue, scope: PsPath?)
  fun renderIssue(issue: PsIssue, scope: PsPath?): String = buildString { renderIssue(this, issue, scope) }
  /** Replaces '/' with "/&zero-width-space;" unless "/>" to make long paths wrappable. */
  fun String.makeTextWrappable() = replace("(?<=\\<)/", "/&#x200b;")
}
