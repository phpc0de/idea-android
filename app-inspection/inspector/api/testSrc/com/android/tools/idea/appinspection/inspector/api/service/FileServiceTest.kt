package com.android.tools.idea.appinspection.inspector.api.service

import org.junit.Test
import com.google.common.truth.Truth.assertThat
import com.intellij.util.io.isDirectory

class FileServiceTest {
  private val fileService = TestFileService()

  @Test
  fun testDirCreation() {
    val cachePath = fileService.getOrCreateCacheDir("x")
    val tmpPath = fileService.getOrCreateTempDir("x")

    assertThat(cachePath.isDirectory()).isTrue()
    assertThat(tmpPath.isDirectory()).isTrue()

    assertThat(cachePath.fileName).isEqualTo(tmpPath.fileName)
    assertThat(cachePath).isNotEqualTo(tmpPath)
    assertThat(cachePath).isEqualTo(fileService.getOrCreateCacheDir("x"))
    assertThat(cachePath).isNotEqualTo(fileService.getOrCreateCacheDir("y"))
  }
}