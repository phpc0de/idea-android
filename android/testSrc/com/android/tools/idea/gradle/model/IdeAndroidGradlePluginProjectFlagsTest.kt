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
package com.android.tools.idea.gradle.model

import com.android.builder.model.AndroidGradlePluginProjectFlags
import com.android.tools.idea.gradle.project.sync.ModelCache
import com.android.tools.idea.gradle.model.stubs.AndroidGradlePluginProjectFlagsStub
import com.google.common.truth.Truth
import org.junit.Test

class IdeAndroidGradlePluginProjectFlagsTest {

    val modelCache = ModelCache.createForTesting()

    @Test
    fun testDefaults() {
        val flags = parse()
        Truth.assertThat(flags.applicationRClassConstantIds).isTrue()
        Truth.assertThat(flags.testRClassConstantIds).isTrue()
        Truth.assertThat(flags.transitiveRClasses).isTrue()
    }

    @Test
    fun testLegacyDefaults() {
        val flags = parse()
        Truth.assertThat(flags.applicationRClassConstantIds).isTrue()
        Truth.assertThat(flags.testRClassConstantIds).isTrue()
        Truth.assertThat(flags.transitiveRClasses).isTrue()
    }

    @Test
    fun applicationRClassConstantIds() {
        val flags = parse((AndroidGradlePluginProjectFlags.BooleanFlag.APPLICATION_R_CLASS_CONSTANT_IDS to true))
        Truth.assertThat(flags.applicationRClassConstantIds).isTrue()
    }

    @Test
    fun applicationRClassNonConstantIds() {
        val flags = parse((AndroidGradlePluginProjectFlags.BooleanFlag.APPLICATION_R_CLASS_CONSTANT_IDS to false))
        Truth.assertThat(flags.applicationRClassConstantIds).isFalse()
    }

    @Test
    fun testRClassConstantIds() {
        val flags = parse((AndroidGradlePluginProjectFlags.BooleanFlag.TEST_R_CLASS_CONSTANT_IDS to true))
        Truth.assertThat(flags.testRClassConstantIds).isTrue()
    }

    @Test
    fun testRClassNonConstantIds() {
        val flags = parse((AndroidGradlePluginProjectFlags.BooleanFlag.TEST_R_CLASS_CONSTANT_IDS to false))
        Truth.assertThat(flags.testRClassConstantIds).isFalse()
    }

    @Test
    fun transitiveRClass() {
        val flags = parse((AndroidGradlePluginProjectFlags.BooleanFlag.TRANSITIVE_R_CLASS to true))
        Truth.assertThat(flags.transitiveRClasses).isTrue()
    }

    @Test
    fun nonTransitiveRClass() {
        val flags = parse(AndroidGradlePluginProjectFlags.BooleanFlag.TRANSITIVE_R_CLASS to false)
        Truth.assertThat(flags.transitiveRClasses).isFalse()
    }

    private fun parse(vararg flags: Pair<AndroidGradlePluginProjectFlags.BooleanFlag, Boolean>): IdeAndroidGradlePluginProjectFlags {
        return modelCache.androidGradlePluginProjectFlagsFrom(AndroidGradlePluginProjectFlagsStub(mapOf(*flags)))
    }
}