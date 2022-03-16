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
package com.android.tools.idea.gradle.project.model;

import static com.android.tools.idea.gradle.project.model.IdeModelTestUtils.assertEqualsOrSimilar;
import static com.android.tools.idea.gradle.project.model.IdeModelTestUtils.verifyUsageOfImmutableCollections;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.android.tools.idea.gradle.model.impl.IdeFilterDataImpl;
import com.android.tools.idea.gradle.model.stubs.FilterDataStub;
import com.android.testutils.Serialization;
import com.android.tools.idea.gradle.project.sync.ModelCache;
import com.android.tools.idea.gradle.project.sync.ModelCacheTesting;
import java.io.Serializable;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link IdeFilterDataImpl}. */
public class IdeFilterDataTest {
    private ModelCacheTesting myModelCache;

    @Before
    public void setUp() throws Exception {
        myModelCache = ModelCache.createForTesting();
    }

    @Test
    public void serializable() {
        assertThat(IdeFilterDataImpl.class).isAssignableTo(Serializable.class);
    }

    @Test
    public void serialization() throws Exception {
        IdeFilterDataImpl filterData = myModelCache.filterDataFrom(new FilterDataStub());
        byte[] bytes = Serialization.serialize(filterData);
        Object o = Serialization.deserialize(bytes);
        assertEquals(filterData, o);
    }

    @Test
    public void constructor() throws Throwable {
        FilterDataStub original = new FilterDataStub();
        IdeFilterDataImpl copy = myModelCache.filterDataFrom(original);
        assertEqualsOrSimilar(original, copy);
        verifyUsageOfImmutableCollections(copy);
    }
}
