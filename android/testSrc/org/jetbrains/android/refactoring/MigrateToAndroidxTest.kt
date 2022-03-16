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
package org.jetbrains.android.refactoring

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.migration.PsiMigrationManager
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.usageView.UsageInfo
import junit.framework.TestCase
import org.jetbrains.android.AndroidTestCase
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.*

private const val BASE_PATH = "refactoring/migrateToAndroidx/"

class MigrateToAndroidxTest : AndroidTestCase() {

  override fun setUp() {
    super.setUp()
    // This is needed for resolving framework classes
    myFixture.allowTreeAccessForAllFiles()
    replaceApplicationService(GradleSyncInvoker::class.java, GradleSyncInvoker.FakeInvoker())
  }

  /**
   * Test to ensure that a [ClassMigrationEntry] takes precedence over a [PackageMigrationEntry]
   * which in this case is the CoordinatorLayout.
   */
  fun testMigrateWithConflictingClassAndPackageNames() {
    AndroidxMigrationBuilder()
      .withFileInProject("MainActivity.java", "src/p1/p2/MainActivity.java")
      .withEntry(ClassMigrationEntry("android.support.design.widget.CoordinatorLayout", "androidx.widget.CoordinatorLayout"))
      .withEntry(PackageMigrationEntry("android.support.v7", "androidx"))
      .withEntry(PackageMigrationEntry("android.support.design", "androidx.design"))
      .run(myFixture, mapOf("MainActivity.java" to "MainActivity_after.java"))
  }

  /**
   * Test to ensure that longer [PackageMigrationEntry] take precedence.
   */
  fun testMigrateWithConflictingPackageNames() {
    AndroidxMigrationBuilder()
      .withFileInProject("MainActivity.java", "src/p1/p2/MainActivity.java")
      .withEntry(ClassMigrationEntry("android.support.design.widget.CoordinatorLayout", "androidx.widget.CoordinatorLayout"))
      .withEntry(PackageMigrationEntry("android.support.v7", "androidx"))
      .withEntry(PackageMigrationEntry("android.support.design", "androidx.design"))
      .withEntry(PackageMigrationEntry("android.support", "wrong"))
      .run(myFixture, mapOf("MainActivity.java" to "MainActivity_after.java"))
  }

  fun testMigrateLayoutXml() {
    AndroidxMigrationBuilder()
      .withEntry(ClassMigrationEntry("android.support.design.widget.CoordinatorLayout", "androidx.widget.CoordinatorLayout"))
      .withEntry(ClassMigrationEntry("android.support.v7.widget.GridLayoutManager", "androidx.widget.GridLayoutManager"))
      .withEntry(ClassMigrationEntry("android.support.design.bottomsheet.BottomSheetBehavior",
                                     "com.google.android.material.bottomsheet.GridLayoutManager"))
      .withEntry(PackageMigrationEntry("android.support.v7", "androidx"))
      .withEntry(PackageMigrationEntry("android.support", "androidx"))
      .withFileInProject("app_bar_main.xml", "res/layout/app_bar_main.xml")
      .run(myFixture, mapOf("app_bar_main.xml" to "app_bar_main_after.xml"))
  }

  fun testMigrateClassNamesInXmlAttributes() {
    AndroidxMigrationBuilder()
      .withEntry(PackageMigrationEntry("android.support.v7.widget", "androidx.appcompat.widget"))
      .withEntry(
        ClassMigrationEntry(
          "android.support.v4.media.session.MediaButtonReceiver",
          "androidx.media.session.MediaButtonReceiver"
        )
      )
      .withFileInProject("menu_main.xml", "res/menu/menu_main.xml")
      .withFileInProject("AndroidManifest.xml", "AndroidManifest.xml")
      .run(myFixture, mapOf("menu_main.xml" to "menu_main_after.xml", "AndroidManifest.xml" to "AndroidManifest_after.xml"))
  }

  private fun doTestMigrateBuildDependencies(relativePath: String, targetPath: String, expectedPath:String) {
    // test both map notation as well as compact notation
    AndroidxMigrationBuilder()
      .withEntry(GradleDependencyMigrationEntry("com.android.support", "appcompat-v7",
                                                "androidx.appcompat", "base", "1.0.0-alpha1"))
      .withEntry(GradleDependencyMigrationEntry("com.android.support.constraint", "constraint-layout",
                                                "androidx.constraint", "base", "2.0.0-alpha1"))
      .withEntry(GradleDependencyMigrationEntry("com.android.support", "recyclerview-v7",
                                                "androidx.recyclerview", "recyclerview", "1.0.0-alpha1"))
      .withEntry(GradleDependencyMigrationEntry("com.android.support.test", "runner",
                                                "androidx.test", "runner", "1.1.0-alpha1"))
      .withEntry(GradleDependencyMigrationEntry("com.android.support.test.espresso", "espresso-core", "androidx.test.espresso", "espresso-core", "3.1.0-alpha1"))
      .withEntry(GradleDependencyMigrationEntry("com.android.support", "exifinterface",
                                                "androidx.exifinterface", "exifinterface", "1.0.0-alpha1"))
      .withEntry(
        UpdateGradleDependencyVersionMigrationEntry(
          "androidx.core", "core-ktx",
          "1.0.0-alpha1"))
      .withEntry(UpdateGradleDependencyVersionMigrationEntry(
        "androidx.core",
        "newer-core-ktx",
        "1.5.0"))
      .withEntry(UpdateGradleDependencyVersionMigrationEntry(
        "androidx.core",
        "variable-ktx",
        "1.0.0-alpha1"))
      .withEntry(UpdateGradleDependencyVersionMigrationEntry(
        "androidx.core",
        "newer-version-ktx",
        "1.1.0"))
      .withFileInProject(relativePath, targetPath)
      .withVersionProvider { _, _, defaultVersion -> defaultVersion }
      .run(myFixture, mapOf(relativePath to expectedPath))
  }

  fun testMigrateBuildDependenciesGroovy() {
    doTestMigrateBuildDependencies("buildDependencies.gradle", "build.gradle", "buildDependencies_after.gradle")
  }

  fun testMigrateBuildDependenciesKTS() {
    doTestMigrateBuildDependencies("buildDependencies.gradle.kts", "build.gradle.kts", "buildDependencies_after.gradle.kts")
  }

  /**
   * Helper class for testing [MigrateToAndroidxProcessor] that sets up and executes
   * a refactoring with specified [AppCompatMigrationEntry] on the given [paths]
   */
  internal class AndroidxMigrationBuilder {
    val paths = mutableMapOf<String, String>()
    val entries = mutableListOf<AppCompatMigrationEntry>()
    var versionProvider: ((String, String, String) -> String)? = null

    fun withFileInProject(pathRelativeToBase: String, targetPath: String) = apply {
      paths[pathRelativeToBase] = targetPath
    }

    fun withEntry(entry: AppCompatMigrationEntry) = apply {
      entries += entry
    }

    fun run(fixture: JavaCodeInsightTestFixture, expectedFile: Map<String, String>) {
      TestCase.assertTrue("Requires entries", !entries.isEmpty())
      for ((key, value) in paths.entries) {
        fixture.copyFileToProject(BASE_PATH + key, value)
      }

      PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()

      // Create the "old" classes since they are not available for tests
      val migration = PsiMigrationManager.getInstance(fixture.project).startMigration()
      entries.forEach { entry ->
        when (entry) {
          is ClassMigrationEntry -> findOrCreateClass(fixture.project, migration, entry.myOldName)
          is PackageMigrationEntry -> findOrCreatePackage(fixture.project, migration, entry.myOldName)
        }
      }

      object : MigrateToAndroidxProcessor(fixture.project, entries, versionProvider) {
        override fun findUsages(): Array<UsageInfo> {
          // Shuffle the findUsages result since the elements when executing the actual
          // refactoring, the elements might be in different order
          return super.findUsages().asList().shuffled().toTypedArray()
        }
      }.run()

      // validate results
      for ((key, value) in paths.entries) {
        val afterFile = expectedFile.get(key) ?: continue
        fixture.checkResultByFile(value, BASE_PATH + afterFile, true)
      }
    }

    fun withVersionProvider(versionProvider: (String, String, String) -> String): AndroidxMigrationBuilder {
      this.versionProvider = versionProvider
      return this
    }
  }
}
