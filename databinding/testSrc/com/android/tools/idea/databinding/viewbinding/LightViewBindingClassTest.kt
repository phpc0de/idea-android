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
package com.android.tools.idea.databinding.viewbinding

import com.android.SdkConstants
import com.android.tools.idea.gradle.model.impl.IdeViewBindingOptionsImpl
import com.android.tools.idea.databinding.psiclass.LightBindingClass
import com.android.tools.idea.databinding.util.isViewBindingEnabled
import com.android.tools.idea.databinding.utils.assertExpected
import com.android.tools.idea.testing.AndroidProjectBuilder
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.findClass
import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.facet.FacetManager
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiParameter
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.android.facet.AndroidFacet
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@RunsInEdt
class LightViewBindingClassTest {
  private val projectRule =
    AndroidProjectRule.withAndroidModel(AndroidProjectBuilder(viewBindingOptions = { IdeViewBindingOptionsImpl(enabled = true) }))

  @get:Rule
  val ruleChain = RuleChain.outerRule(projectRule).around(EdtRule())!!

  private val facet
    get() = FacetManager.getInstance(projectRule.module).getFacetByType(AndroidFacet.ID)!!

  private val fixture
    get() = projectRule.fixture as JavaCodeInsightTestFixture

  @Before
  fun setUp() {
    assertThat(facet.isViewBindingEnabled()).isTrue()
    fixture.addFileToProject("src/main/AndroidManifest.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="test.db">
        <application />
      </manifest>
    """.trimIndent())
  }

  @Test
  fun lightClassGeneratedForViewBindingLayout() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:id="@+id/test_id"
          android:orientation="vertical"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </LinearLayout>
    """.trimIndent())
    val context = fixture.addClass("public class MainActivity {}")

    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context)
    assertThat(binding).isNotNull()
  }

  @Test
  fun avoidGeneratingBindingForViewBindingIgnoreLayout() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          tools:viewBindingIgnore="true" />
          android:id="@+id/test_id"
          android:orientation="vertical"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </LinearLayout>
    """.trimIndent())
    val context = fixture.addClass("public class MainActivity {}")

    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context)
    assertThat(binding).isNull()
  }

  @Test
  fun expectedStaticMethodsAreGenerated() {
    fixture.addFileToProject("src/main/res/layout/view_root_activity.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <view xmlns:android="http://schemas.android.com/apk/res/android" />
    """.trimIndent())
    fixture.addFileToProject("src/main/res/layout/merge_root_activity.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <merge xmlns:android="http://schemas.android.com/apk/res/android" />
    """.trimIndent())
    val context = fixture.addClass("public class ViewRootActivity {}")

    (fixture.findClass("test.db.databinding.ViewRootActivityBinding", context) as LightBindingClass).let { binding ->
      val methods = binding.methods.filter { it.hasModifier(JvmModifier.STATIC) }
      assertThat(methods.map { it.presentation!!.presentableText }).containsExactly(
        "inflate(LayoutInflater)",
        "inflate(LayoutInflater, ViewGroup, boolean)",
        "bind(View)"
      )
    }

    (fixture.findClass("test.db.databinding.MergeRootActivityBinding", context) as LightBindingClass).let { binding ->
      val methods = binding.methods.filter { it.hasModifier(JvmModifier.STATIC) }
      assertThat(methods.map { it.presentation!!.presentableText }).containsExactly(
        "inflate(LayoutInflater, ViewGroup)",
        "bind(View)"
      )
    }
  }

  // ViewBinding logic breaks from DataBinding logic around view stubs. See also: b/142533358
  @Test
  fun correctTypeGeneratedForViewStubs() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <ViewStub
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:id="@+id/test_id"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </ViewStub>
    """.trimIndent())
    val context = fixture.addClass("public class MainActivity {}")

    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context)!!
    assertThat(binding.findFieldByName("testId", false)!!.type.canonicalText).isEqualTo(SdkConstants.CLASS_VIEWSTUB)
  }

  // ViewBinding logic breaks from DataBinding logic around root type. See also: b/139732774
  @Test
  fun correctReturnTypeGeneratedForGetRootMethod() {
    fixture.addFileToProject("src/main/res/layout/activity_consistent_root.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </LinearLayout>
    """.trimIndent())

    fixture.addFileToProject("src/main/res/layout/activity_inconsistent_root.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <FrameLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </FrameLayout>
    """.trimIndent())

    fixture.addFileToProject("src/main/res/layout-land/activity_inconsistent_root.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <RelativeLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </RelativeLayout>
    """.trimIndent())

    val context = fixture.addClass("public class ConsistentRootActivity {}")

    run {
      val binding = fixture.findClass("test.db.databinding.ActivityConsistentRootBinding", context)!!
      assertThat(binding.findMethodsByName("getRoot", false)[0].returnType!!.canonicalText).isEqualTo("android.widget.LinearLayout")
    }

    run {
      val binding = fixture.findClass("test.db.databinding.ActivityInconsistentRootBinding", context)!!
      assertThat(binding.findMethodsByName("getRoot", false)[0].returnType!!.canonicalText).isEqualTo("android.view.View")
    }
  }

  @Test
  fun viewBindingDoesntGenerateImplClasses() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <FrameLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </FrameLayout>
    """.trimIndent())

    fixture.addFileToProject("src/main/res/layout-land/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <RelativeLayout
          xmlns:android="http://schemas.android.com/apk/res/android"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent">
      </RelativeLayout>
    """.trimIndent())

    val context = fixture.addClass("public class MainActivity {}")

    run {
      assertThat(fixture.findClass("test.db.databinding.ActivityMainBinding", context)).isNotNull()
      assertThat(fixture.findClass("test.db.databinding.ActivityMainBindingImpl", context)).isNull()
      assertThat(fixture.findClass("test.db.databinding.ActivityMainBindingLandImpl", context)).isNull()
    }
  }

  @Test
  fun fieldsAreAnnotatedNonNullAndNullableCorrectly() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:id="@+id/always_present">
        <TextView android:id="@+id/sometimes_present" />
      </LinearLayout>
    """.trimIndent())

    fixture.addFileToProject("src/main/res/layout-land/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/always_present" />
    """.trimIndent())

    val context = fixture.addClass("public class MainActivity {}")

    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context) as LightBindingClass
    assertThat(binding.fields).hasLength(2)
    val alwaysPresentField = binding.fields.first { it.name == "alwaysPresent" }
    val sometimesPresentField = binding.fields.first { it.name == "sometimesPresent" }

    val nullabilityManager = NullableNotNullManager.getInstance(fixture.project)
    assertThat(nullabilityManager.isNotNull(alwaysPresentField, false)).isTrue()
    assertThat(nullabilityManager.isNullable(sometimesPresentField, false)).isTrue()
  }

  @Test
  fun methodsAreAnnotatedNonNullAndNullableCorrectly_regularLayouts() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout />
    """.trimIndent())

    val project = fixture.project
    val context = fixture.addClass("public class MainActivity {}")
    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context) as LightBindingClass

    binding.methods.filter { it.name == "inflate" }.let { inflateMethods ->
      assertThat(inflateMethods).hasSize(2)
      inflateMethods.first { it.parameters.size == 3 }.let { inflateMethod ->
        (inflateMethod.parameters[0] as PsiParameter).assertExpected("LayoutInflater", "inflater")
        (inflateMethod.parameters[1] as PsiParameter).assertExpected("ViewGroup", "parent", isNullable = true)
        (inflateMethod.parameters[2] as PsiParameter).assertExpected("boolean", "attachToParent")
        inflateMethod.returnType!!.assertExpected(project, "ActivityMainBinding")
      }

      inflateMethods.first { it.parameters.size == 1 }.let { inflateMethod ->
        (inflateMethod.parameters[0] as PsiParameter).assertExpected("LayoutInflater", "inflater")
        inflateMethod.returnType!!.assertExpected(project, "ActivityMainBinding")
      }
    }

    binding.methods.filter { it.name == "bind" }.let { bindMethods ->
      assertThat(bindMethods).hasSize(1)
      bindMethods.first { it.parameters.size == 1 }.let { bindMethod ->
        (bindMethod.parameters[0] as PsiParameter).assertExpected("View", "view")
        bindMethod.returnType!!.assertExpected(project, "ActivityMainBinding")
      }
    }
  }

  @Test
  fun methodsAreAnnotatedNonNullAndNullableCorrectly_mergeLayouts() {
    fixture.addFileToProject("src/main/res/layout/activity_main.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <merge />
    """.trimIndent())

    val project = fixture.project
    val context = fixture.addClass("public class MainActivity {}")
    val binding = fixture.findClass("test.db.databinding.ActivityMainBinding", context) as LightBindingClass

    binding.methods.filter { it.name == "inflate" }.let { inflateMethods ->
      assertThat(inflateMethods).hasSize(1)
      inflateMethods.first { it.parameters.size == 2 }.let { inflateMethod ->
        (inflateMethod.parameters[0] as PsiParameter).assertExpected("LayoutInflater", "inflater")
        (inflateMethod.parameters[1] as PsiParameter).assertExpected("ViewGroup", "parent") // Not nullable due to <merge> root!
        inflateMethod.returnType!!.assertExpected(project, "ActivityMainBinding")
      }
    }

    binding.methods.filter { it.name == "bind" }.let { bindMethods ->
      assertThat(bindMethods).hasSize(1)
      bindMethods.first { it.parameters.size == 1 }.let { bindMethod ->
        (bindMethod.parameters[0] as PsiParameter).assertExpected("View", "view")
        bindMethod.returnType!!.assertExpected(project, "ActivityMainBinding")
      }
    }
  }
}