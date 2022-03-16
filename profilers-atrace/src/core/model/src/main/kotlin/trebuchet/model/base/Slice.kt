/*
 * Copyright 2017 Google Inc.
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

package trebuchet.model.base

interface Slice {
    val startTime: Double
    val endTime: Double
    // CpuTime is the time this slice was scheduled on a core.
    // It is not the total time eg (endTime - startTime).
    val cpuTime: Double
    val name: String
    val didNotFinish: Boolean

    val duration: Double get() = endTime - startTime
}