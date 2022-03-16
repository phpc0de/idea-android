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
package org.jetbrains.android.refactoring;

import static com.android.AndroidProjectTypes.PROJECT_TYPE_APP;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_SHOW_AS_ACTION;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.CLASS_APP_COMPAT_ACTIVITY;
import static com.android.SdkConstants.TAG_ITEM;
import static org.jetbrains.android.refactoring.AppCompatMigrationEntry.CHANGE_CUSTOM_VIEW_SUPERCLASS;
import static org.jetbrains.android.refactoring.AppCompatMigrationEntry.CHANGE_THEME_AND_STYLE;
import static org.jetbrains.android.refactoring.MigrateToAppCompatProcessor.CLASS_SUPPORT_FRAGMENT_ACTIVITY;
import static org.jetbrains.android.refactoring.MigrateToAppCompatProcessor.MIGRATION_ENTRY_SIZE;
import static org.jetbrains.android.refactoring.MigrateToAppCompatProcessor.buildMigrationMap;

import com.android.annotations.NonNull;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.migration.MigrationMapEntry;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.usageView.UsageInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.AttributeMigrationEntry;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.AttributeValueMigrationEntry;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.ClassMigrationEntry;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.MethodMigrationEntry;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.XmlElementMigration;
import org.jetbrains.android.refactoring.AppCompatMigrationEntry.XmlTagMigrationEntry;
import org.jetbrains.annotations.NotNull;

/**
 * This tests the MigrateToAppCompat refactoring for JPS projects.
 * Gradle scenario to be tested.
 */
public class MigrateToAppCompatTest extends AndroidTestCase {
  private static final String BASE_PATH = "refactoring/migrateToAppCompat/";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // This is needed for resolving framework classes
    myFixture.allowTreeAccessForAllFiles();
  }

  public void testMigrationMapSize() throws Exception {
    // Inspired by Lint registry tests!
    assertEquals(MIGRATION_ENTRY_SIZE, buildMigrationMap(getProject()).size());
  }

  public void testMigrateActivity2AppCompatActivity() throws Exception {
    new MigrationBuilder()
      .withFileInProject("SimpleActivity.java", "src/p1/p2/SimpleActivity.java")
      .withEntry(new ClassMigrationEntry(CLASS_ACTIVITY, CLASS_APP_COMPAT_ACTIVITY.defaultName()))
      .run(myFixture);
  }

  public void testMigrateFragmentActivity() throws Exception {
    myFixture.addClass("" +
                       "package android.support.v4.app;\n" +
                       "import android.app.Activity;\n" +
                       "public class FragmentActivity extends Activity {\n" +
                       "}\n");
    new MigrationBuilder()
      .withFileInProject("BaseFragmentActivity.java", "src/p1/p2/BaseFragmentActivity.java")
      .withEntry(new ClassMigrationEntry(CLASS_SUPPORT_FRAGMENT_ACTIVITY, CLASS_APP_COMPAT_ACTIVITY.defaultName()))
      .run(myFixture);
  }

  public void testMenuNamespaceMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("menu.xml", "res/menu/menu.xml")
      .withEntry(new AttributeMigrationEntry(ATTR_SHOW_AS_ACTION, ANDROID_URI,
                                             ATTR_SHOW_AS_ACTION, AUTO_URI,
                                             XmlElementMigration.FLAG_MENU, TAG_ITEM))

      .withEntry(new AttributeMigrationEntry(MigrateToAppCompatProcessor.ATTR_ACTION_VIEW_CLASS, ANDROID_URI,
                                             MigrateToAppCompatProcessor.ATTR_ACTION_VIEW_CLASS, AUTO_URI,
                                             XmlElementMigration.FLAG_MENU, TAG_ITEM))

      .withEntry(new AttributeValueMigrationEntry(MigrateToAppCompatProcessor.ANDROID_WIDGET_SEARCH_VIEW_CLASS,
                                                  MigrateToAppCompatProcessor.SUPPORT_V7_WIDGET_SEARCH_VIEW_CLASS,
                                                  MigrateToAppCompatProcessor.ATTR_ACTION_VIEW_CLASS, ANDROID_URI,
                                                  XmlElementMigration.FLAG_MENU,
                                                  TAG_ITEM))

      .withEntry(new AttributeMigrationEntry(MigrateToAppCompatProcessor.ATTR_ACTION_PROVIDER_CLASS, ANDROID_URI,
                                             MigrateToAppCompatProcessor.ATTR_ACTION_PROVIDER_CLASS, AUTO_URI,
                                             XmlElementMigration.FLAG_MENU, TAG_ITEM))

      .withEntry(new AttributeValueMigrationEntry(MigrateToAppCompatProcessor.ANDROID_WIDGET_SHARE_PROVIDER_CLASS,
                                                  "android.support.v7.widget.ShareActionProvider",
                                                  MigrateToAppCompatProcessor.ATTR_ACTION_PROVIDER_CLASS, ANDROID_URI,
                                                  XmlElementMigration.FLAG_MENU, TAG_ITEM))
      .run(myFixture);
  }

  public void testMenuItem2MenuItemCompatMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("MenuItemUtil.java", "src/p1/p2/MenuItemUtil.java")
      .withAllMapEntries()
      .run(myFixture);
  }

  public void testCustomViewChange2AppCompat() throws Exception {
    new MigrationBuilder()
      .withFileInProject("CustomView.java", "src/p1/p2/CustomView.java")
      .withEntry(new AppCompatMigrationEntry(CHANGE_CUSTOM_VIEW_SUPERCLASS))
      .run(myFixture);
  }

  // Also tests method name changes from getActionBar to getSupportActionBar
  // as well as getFragmentManager => getSupportFragmentManager
  public void testFragmentAndActivityMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("MainActivity.java", "src/p1/p2/MainActivity.java")
      .withAllMapEntries()
      .run(myFixture);
  }

  /**
   * Test to ensure that there are 0 usages on an already migrated class.
   */
  public void testNoUsagesWithMigratedClass() throws Exception {
    // Add the Fragment.java file so that that the super method onAttach() can be resolved in
    // the test.
    myFixture.addFileToProject("src/android/support/v4/app/Fragment.java",
                               "package android.support.v4.app;\n" +
                               "\n" +
                               "import android.app.Activity;\n" +
                               "public class Fragment {\n" +
                               "\n" +
                               "    public Fragment() {}\n" +
                               "    public void onAttach(Activity activity) {\n" +
                               "        mCalled = true;\n" +
                               "    }\n" +
                               "}\n");

    MigrationBuilder builder = new MigrationBuilder()
      .withFileInProject("MigratedActivity.java", "src/p1/p2/MigratedActivity.java")
      .withAllMapEntries();

    MigrateToAppCompatProcessor processor = builder.setUpProcessor(myFixture);

    // Ensure that the usages = 0
    UsageInfo[] usages = processor.findUsages();
    assertEquals(StringUtil.join(Arrays.stream(usages).map(UsageInfo::toString)
                                   .collect(Collectors.toList()), "\n"),
                 0, usages.length);

    processor.refreshElements(PsiElement.EMPTY_ARRAY);
    builder.runMigration(myFixture, processor);
  }

  public void testShareActionProviderMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("MyShareActionProvider.java", "src/p1/p2/MyShareActionProvider.java")
      .withEntry(new ClassMigrationEntry("android.widget.ShareActionProvider",
                                         "android.support.v7.widget.ShareActionProvider"))
      .run(myFixture);
  }

  public void testAppCompatStyleMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("theme_holo.xml", "res/values/styles.xml")
      .withAppCompatStyles("Theme.AppCompat.Light.DarkActionBar")
      .withEntry(new AppCompatMigrationEntry(CHANGE_THEME_AND_STYLE))
      .run(myFixture);
  }

  public void testAppCompatStyleMigrationWithLayoutAndManifest() throws Exception {
    new MigrationBuilder()
      // validate that attrs and themes in styles.xml are processed.
      .withFileInProject("theme_material_styles.xml", "res/values/styles.xml")
      // validate that styles and android:theme in layouts are migrated.
      .withFileInProject("layout_material_style.xml", "res/layout/activity_main.xml")
      // validate that styles in AndroidManifest.xml are migrated.
      .withFileInProject("theme_material_manifest.xml", "AndroidManifest.xml")
      .withAppCompatStyles("Theme.AppCompat.Light.DarkActionBar",
                           "Theme.AppCompat",
                           "ThemeOverlay.AppCompat",
                           "TextAppearance.AppCompat.Widget.Button",
                           "ThemeOverlay.AppCompat.Dark.ActionBar",
                           "ThemeOverlay.AppCompat.Light")
      .withAppCompatAttrs("colorAccent",
                          "colorPrimary",
                          "colorPrimaryDark",
                          "background",
                          "selectableItemBackground",
                          "actionBarSize")
      .withEntry(new AppCompatMigrationEntry(CHANGE_THEME_AND_STYLE))
      .withEntry(new XmlTagMigrationEntry("android.widget.Toolbar", "",
                                          "android.support.v7.widget.Toolbar", "",
                                          XmlElementMigration.FLAG_LAYOUT))
      .run(myFixture);
  }

  public void testNoFindUsagesForNonFrameworkStyleAttrs() throws Exception {
    MigrateToAppCompatProcessor processor = new MigrationBuilder()
      // validate that attrs and themes in styles.xml are processed.
      .withFileInProject("theme_material_styles_after.xml", "res/values/styles.xml")
      .withAppCompatStyles("Theme.AppCompat.Light.DarkActionBar",
                           "Theme.AppCompat",
                           "ThemeOverlay.AppCompat",
                           "TextAppearance.AppCompat.Widget.Button",
                           "ThemeOverlay.AppCompat.Dark.ActionBar",
                           "ThemeOverlay.AppCompat.Light")
      .withAppCompatAttrs("colorAccent",
                          "colorPrimary",
                          "colorPrimaryDark",
                          "background",
                          "selectableItemBackground",
                          "actionBarSize")
      .withEntry(new AppCompatMigrationEntry(CHANGE_THEME_AND_STYLE))
      .setUpProcessor(myFixture);

    UsageInfo[] usages = processor.findUsages();
    assertEquals(0, usages.length);
  }

  public void testToolbarMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("ToolbarTestActivity.java", "src/p1/p2/ToolbarTestActivity.java")
      .withEntry(new ClassMigrationEntry("android.widget.Toolbar",
                                         "android.support.v7.widget.Toolbar"))
      .withEntry(new MethodMigrationEntry(
        "android.app.Activity", "setActionBar",
        CLASS_APP_COMPAT_ACTIVITY.defaultName(), "setSupportActionBar"))
      .withEntry(new ClassMigrationEntry(CLASS_ACTIVITY, CLASS_APP_COMPAT_ACTIVITY.defaultName()))
      .run(myFixture);
  }

  public void testImageViewAttributeMigration() throws Exception {
    new MigrationBuilder()
      .withFileInProject("layout_with_imageview.xml", "res/layout/activity_main.xml")
      .withEntry(new AttributeMigrationEntry(
        "src", ANDROID_URI,
        "srcCompat", AUTO_URI, XmlElementMigration.FLAG_LAYOUT,
        "ImageView", "ImageButton"))
      .run(myFixture);
  }

  @Override
  protected void configureAdditionalModules(@NotNull TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder,
                                            @NotNull List<MyAdditionalModuleData> modules) {
    if ("testMigrationQuickFix".equals(getName())) {
      // This is needed to make the jps project believe that it depends on appcompat.
      // note that we also add an AndroidManifest.xml with the appcompat package.
      addModuleWithAndroidFacet(projectBuilder, modules, "appcompat", PROJECT_TYPE_APP);
    }
  }

  /**
   * Helper/Infrastructure in fluent/builder style api for running {@link MigrateToAppCompatProcessor} and checking
   * that the results match the expected results stored in filename_after.ext
   */
  static final class MigrationBuilder {
    private final Map<String, String> myPaths = new HashMap<>();
    private final List<AppCompatMigrationEntry> myEntries = new ArrayList<>();
    private final Set<String> myAppCompatAttrs = Sets.newHashSetWithExpectedSize(2);
    private final Set<String> myAppCompatStyles = Sets.newHashSetWithExpectedSize(2);
    private boolean myUseAllMapEntries = false;

    /**
     * Files that need to be added to the project.
     *
     * @param pathRelativeToBase Path such as "Foo.java" relative to {@link BASE_PATH}
     * @param targetPath         Expected path in the project such as src/p1/p2/Foo.java
     * @return current instance
     */
    public MigrationBuilder withFileInProject(String pathRelativeToBase, String targetPath) {
      myPaths.put(pathRelativeToBase, targetPath);
      return this;
    }

    /**
     * Add an entry to the current migration.
     *
     * @param entry
     * @return this
     */
    public MigrationBuilder withEntry(AppCompatMigrationEntry entry) {
      myEntries.add(entry);
      return this;
    }

    /**
     * Resulting {@link MigrateToAppCompatProcessor} should all the {@link MigrationMapEntry}s
     * to process this test run.
     *
     * @return current instance
     */
    public MigrationBuilder withAllMapEntries() {
      myUseAllMapEntries = true;
      return this;
    }

    /**
     * Facility for providing appCompat specific styles
     *
     * @param styles list of styles
     * @return this
     */
    public MigrationBuilder withAppCompatStyles(@NonNull String... styles) {
      myAppCompatStyles.addAll(Arrays.asList(styles));
      return this;
    }

    public MigrationBuilder withAppCompatAttrs(@NonNull String... attrs) {
      myAppCompatAttrs.addAll(Arrays.asList(attrs));
      return this;
    }

    public MigrateToAppCompatProcessor setUpProcessor(JavaCodeInsightTestFixture fixture) {
      if (!myUseAllMapEntries) {
        assertTrue(!myEntries.isEmpty());
      }
      assertTrue(!myPaths.isEmpty());

      for (Map.Entry<String, String> entry : myPaths.entrySet()) {
        fixture.copyFileToProject(BASE_PATH + entry.getKey(), entry.getValue());
      }

      return makeProcessor(fixture.getProject(), myUseAllMapEntries, myEntries);
    }

    private void runMigration(JavaCodeInsightTestFixture fixture, MigrateToAppCompatProcessor processor) {
      processor.run();

      for (Map.Entry<String, String> entry : myPaths.entrySet()) {
        String key = entry.getKey();
        String ext = key.substring(key.lastIndexOf("."));
        String afterFile = key.substring(0, key.indexOf(ext)) + "_after" + ext;
        fixture.checkResultByFile(entry.getValue(),
                                  BASE_PATH + afterFile, true);
      }
    }

    public void run(JavaCodeInsightTestFixture fixture) {
      MigrateToAppCompatProcessor processor = setUpProcessor(fixture);
/* b/146019491
      runMigration(fixture, processor);
b/146019491 */
    }

    public MigrateToAppCompatProcessor makeProcessor(Project project, boolean allEntries,
                                                     List<AppCompatMigrationEntry> migrationMap) {
      AppCompatStyleMigration styleMigration = new AppCompatStyleMigration(myAppCompatAttrs, myAppCompatStyles);
      return new MigrateToAppCompatProcessor(project,
                                             allEntries ? buildMigrationMap(project) : migrationMap,
                                             (artifact, version) -> styleMigration);
    }
  }
}
