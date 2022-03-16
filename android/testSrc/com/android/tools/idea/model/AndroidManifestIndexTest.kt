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
package com.android.tools.idea.model

import com.google.common.truth.Truth.assertThat
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.util.indexing.FileContent
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AndroidManifestIndexTest {
  @Test
  fun indexer_reallyShortManifest() {
    val manifestMap = AndroidManifestIndex.Indexer.map(FakeXmlFileContent("<"))
    assertThat(manifestMap).isEmpty()
  }

  @Test
  fun indexer_wellFormedManifest() {
    @Language("xml")
    val manifestContent = """
<?xml version='1.0' encoding='utf-8'?>
<manifest xmlns:android='http://schemas.android.com/apk/res/android'
  package='com.example' android:enabled='true'>
  <application android:theme='@style/Theme.AppCompat' android:debuggable='true'>
    <activity android:name='.EnabledActivity' android:enabled='true' android:exported='true' android:theme='@style/AppTheme.NoActionBar'>
      <intent-filter>
        <action android:name='android.intent.action.MAIN'/>
        <category android:name='android.intent.category.DEFAULT'/>
      </intent-filter>
    </activity>
    <activity android:name='.DisabledActivity' android:enabled='false' android:exported='true'>
    </activity>
    <activity-alias android:name='.EnabledAlias' android:enabled='true' android:exported='true' android:targetActivity='.DisabledActivity'>
    </activity-alias>
    <activity-alias android:name='.DisabledAlias' android:enabled='false' android:exported='true' android:targetActivity='.EnabledActivity'>
    </activity-alias>
  </application>
  <uses-feature android:name="android.hardware.type.watch" android:required="true" android:glEsVersion="integer" />
  <uses-permission android:name='android.permission.SEND_SMS'/>
  <uses-permission-sdk-23 android:name='custom.permissions.NO_GROUP'/>
  <permission-group android:name='custom.permissions.CUSTOM_GROUP'/>
  <permission android:name='custom.permissions.IN_CUSTOM_GROUP' android:permissionGroup='custom.permissions.CUSTOM_GROUP'/>
  <permission android:name='custom.permissions.NO_GROUP'/>
  <uses-sdk android:minSdkVersion='22' android:targetSdkVersion='28'/>
</manifest>
    """.trimIndent()
    val manifestMap = AndroidManifestIndex.Indexer.map(FakeXmlFileContent(manifestContent))
    assertThat(manifestMap).containsExactly("com.example", AndroidManifestRawText(
      activities = setOf(
        ActivityRawText(
          name = ".EnabledActivity",
          enabled = "true",
          exported = "true",
          theme = "@style/AppTheme.NoActionBar",
          intentFilters = setOf(
            IntentFilterRawText(actionNames = setOf("android.intent.action.MAIN"),
                                categoryNames = setOf("android.intent.category.DEFAULT"))
          )
        ),
        ActivityRawText(name = ".DisabledActivity", enabled = "false", exported = "true", theme = null, intentFilters = setOf())
      ),
      activityAliases = setOf(
        ActivityAliasRawText(name = ".EnabledAlias", targetActivity = ".DisabledActivity",
                             enabled = "true", exported = "true", intentFilters = setOf()),
        ActivityAliasRawText(name = ".DisabledAlias", targetActivity = ".EnabledActivity",
                             enabled = "false", exported = "true", intentFilters = setOf())
      ),
      customPermissionGroupNames = setOf("custom.permissions.CUSTOM_GROUP"),
      customPermissionNames = setOf("custom.permissions.IN_CUSTOM_GROUP",
                                    "custom.permissions.NO_GROUP"),
      debuggable = "true",
      enabled = "true",
      minSdkLevel = "22",
      packageName = "com.example",
      usedPermissionNames = setOf("android.permission.SEND_SMS", "custom.permissions.NO_GROUP"),
      usedFeatures = setOf(UsedFeatureRawText(name = "android.hardware.type.watch", required = "true")),
      targetSdkLevel = "28",
      theme = "@style/Theme.AppCompat")
    )
  }

  @Test
  fun indexer_malformedManifest() {
    @Language("xml")
    val manifestContent = """
<?xml version='1.0' encoding='utf-8'?>
<manifest xmlns:android='http://schemas.android.com/apk/res/android'
  package='com.example' android:enabled='true'>
  <application android:theme='@style/Theme.AppCompat' android:debuggable='true'>
    <activity android:name='.EnabledActivity' android:enabled='true' android:theme="@style/AppTheme.NoActionBar">
      <intent-filter>
        <action android:name='android.intent.action.MAIN'/>
        <category android:name='android.intent.category.DEFAULT'/>
        
        <!-- Recovery case1: Though Attr.value missing errors, no more other siblings(child tags of <intent-filter>)
        need to be processed, we can go to the next END_TAG and then return to its parent tag, <intent-filter> -->
        <action android:name
        
      </intent-filter>
    </activity>
    <activity android:name='.DisabledActivity' android:enabled='false'>
    </activity>
    <activity-alias android:name='.EnabledAlias' android:enabled='true' android:targetActivity='.DisabledActivity'>
    </activity-alias>
    <activity-alias android:name='.DisabledAlias' android:enabled='false' android:targetActivity='.EnabledActivity'>
    </activity-alias>
  </application>

  <uses-feature android:name="android.hardware.type.watch" android:required="true" android:glEsVersion="integer" />

  <uses-permission-sdk-23 android:name='custom.permissions.NO_GROUP'/>
  <permission-group android:name='custom.permissions.CUSTOM_GROUP'/>
  <permission android:name='custom.permissions.IN_CUSTOM_GROUP' android:permissionGroup='custom.permissions.CUSTOM_GROUP'/>
  <permission android:name='custom.permissions.NO_GROUP'/>
  
  <!-- Recovery case2: though Attr.value missing errors, the next sibling, child tag of <manifest> can be processed successfully -->
  <permission android:nam
    
  <uses-sdk android:minSdkVersion='22' android:targetSdkVersion='28'/>
  
  <!-- No recovery case1: though no end tag of uses-permission, info of this tag is retrieved still. However for the rest of the file,
  parsing won't be recovered because no matching end tag after skipping sub tree (based on the level matching). And eventually, it hits
  the end of document. -->
  <uses-permission android:name='android.permission.SEND_SMS'>
  
  <uses-permission-sdk-23 android:name='custom.permissions.NO_GROUP1'/>
</manifest>
      """.trimIndent()
    val manifestMap = AndroidManifestIndex.Indexer.map(FakeXmlFileContent(manifestContent))
    assertThat(manifestMap).containsExactly("com.example", AndroidManifestRawText(
      activities = setOf(
        ActivityRawText(
          name = ".EnabledActivity",
          enabled = "true",
          exported = null,
          theme = "@style/AppTheme.NoActionBar",
          intentFilters = setOf(
            IntentFilterRawText(actionNames = setOf("android.intent.action.MAIN"),
                                categoryNames = setOf("android.intent.category.DEFAULT"))
          )
        ),
        ActivityRawText(name = ".DisabledActivity", enabled = "false", exported = null, theme = null, intentFilters = setOf())
      ),
      activityAliases = setOf(
        ActivityAliasRawText(name = ".EnabledAlias", targetActivity = ".DisabledActivity",
                             enabled = "true", exported = null, intentFilters = setOf()),
        ActivityAliasRawText(name = ".DisabledAlias", targetActivity = ".EnabledActivity",
                             enabled = "false", exported = null, intentFilters = setOf())
      ),
      customPermissionGroupNames = setOf("custom.permissions.CUSTOM_GROUP"),
      customPermissionNames = setOf("custom.permissions.IN_CUSTOM_GROUP",
                                    "custom.permissions.NO_GROUP"),
      debuggable = "true",
      enabled = "true",
      minSdkLevel = "22",
      packageName = "com.example",
      usedPermissionNames = setOf("android.permission.SEND_SMS", "custom.permissions.NO_GROUP"),
      usedFeatures = setOf(UsedFeatureRawText(name = "android.hardware.type.watch", required = "true")),
      targetSdkLevel = "28",
      theme = "@style/Theme.AppCompat")
    )
  }
}

private class FakeXmlFileContent(private val content: String) : FileContent {
  private val file = MockVirtualFile("", content)

  override fun getContentAsText() = content
  override fun getContent() = content.toByteArray()
  override fun <T : Any?> getUserData(key: Key<T>): T? = throw UnsupportedOperationException()
  override fun getFileType(): FileType = XmlFileType.INSTANCE
  override fun getFile() = file
  override fun getFileName() = ""
  override fun <T : Any?> putUserData(key: Key<T>, value: T?) = throw UnsupportedOperationException()
  override fun getProject() = throw UnsupportedOperationException()
  override fun getPsiFile() = throw UnsupportedOperationException()
}