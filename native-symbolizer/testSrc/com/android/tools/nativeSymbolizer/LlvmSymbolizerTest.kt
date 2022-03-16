/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.nativeSymbolizer

import com.android.testutils.TestUtils.resolveWorkspacePath
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Paths


class LlvmSymbolizerTest {

  val testDataDir = resolveWorkspacePath("tools/adt/idea/native-symbolizer/testData/bin/").toString()
  val LIB_FILE_NAME = "libnative-lib.so"
  val EXPECTED_SYMBOLS_FILE_NAME = "symbols.txt"
  val architectures = listOf("arm", "arm64", "x86", "x86_64")

  @Test
  fun testLlvmSymbolizerFound() {
    Assert.assertTrue(File(getLlvmSymbolizerPath()).exists())
  }

  @Test
  fun testSymbolizeAll() {
    val symbolizer = createSymbolizer()
    for (arch in architectures) {
      val expectedSymbolsFile = Paths.get(testDataDir, arch, EXPECTED_SYMBOLS_FILE_NAME).toFile()
      Assert.assertTrue(expectedSymbolsFile.exists())
      for (line in expectedSymbolsFile.readLines()) {
        val symParts = line.split('|')
        val offset = symParts[0].toLong(16)
        val name = symParts[1]
        val sourceFile = symParts[2]
        val lineNumber = symParts[3].toInt()
        val module = "/data/app/com.someapp.name-abcd09876abds==/lib/arm64/" + LIB_FILE_NAME

        // +1 to get an address within the function, rather than function start address
        val offsetWithinFunction = offset + 1
        val symbol = symbolizer.symbolize(arch, module, offsetWithinFunction)!!
        Assert.assertNotNull(symbol)
        Assert.assertEquals(name, symbol.name)
        Assert.assertEquals(sourceFile, symbol.sourceFile)
        Assert.assertTrue(symbol.lineNumber >= lineNumber)
      }
    }
  }

  @Test
  fun testSymbolizeBinariesBuiltOnWindows() {
    val arch = "arm64"
    val binDir = "win"
    val symLocator = SymbolFilesLocator(mapOf(Pair(arch, setOf(Paths.get(testDataDir, binDir).toFile()!!))))
    val symbolizer = LlvmSymbolizer(getLlvmSymbolizerPath(), symLocator)
    val expectedSymbolsFile = Paths.get(testDataDir, binDir, EXPECTED_SYMBOLS_FILE_NAME).toFile()
    Assert.assertTrue(expectedSymbolsFile.exists())
    for (line in expectedSymbolsFile.readLines()) {
      val symParts = line.split('|')
      val offset = symParts[0].toLong(16)
      val name = symParts[1]
      val sourceFile = symParts[2]
      val lineNumber = symParts[3].toInt()
      val module = "/data/app/com.someapp.name-abcd09876abds==/lib/arm64/" + LIB_FILE_NAME

      // +1 to get an address within the function, rather than function start address
      val offsetWithinFunction = offset + 1
      val symbol = symbolizer.symbolize(arch, module, offsetWithinFunction)!!
      Assert.assertNotNull(symbol)
      Assert.assertEquals(name, symbol.name)
      Assert.assertEquals(symbol.sourceFile.replace('\\', '/'), sourceFile)
      Assert.assertTrue(symbol.lineNumber >= lineNumber)
    }
  }

  @Test
  fun testSpaceInPath() {
    val tempDir = FileUtil.createTempDirectory("llvm-symbolizer", "space-test", true)
    val symbolDir = File(tempDir, "name with a space")
    FileUtil.copyDir(Paths.get(testDataDir, "win").toFile(), symbolDir)
    val arch = "arm64"
    val symLocator = SymbolFilesLocator(mapOf(Pair(arch, setOf(symbolDir))))
    val symbolizer = LlvmSymbolizer(getLlvmSymbolizerPath(), symLocator)
    val expectedSymbolsFile = File(symbolDir, EXPECTED_SYMBOLS_FILE_NAME)
    Assert.assertTrue(expectedSymbolsFile.exists())
    for (line in expectedSymbolsFile.readLines()) {
      val symParts = line.split('|')
      val offset = symParts[0].toLong(16)
      val name = symParts[1]
      val sourceFile = symParts[2]
      val lineNumber = symParts[3].toInt()
      val module = "/data/app/com.someapp.name-abcd09876abds==/lib/arm64/" + LIB_FILE_NAME

      // +1 to get an address within the function, rather than function start address
      val offsetWithinFunction = offset + 1
      val symbol = symbolizer.symbolize(arch, module, offsetWithinFunction)!!
      Assert.assertNotNull(symbol)
      Assert.assertEquals(name, symbol.name)
      Assert.assertEquals(symbol.sourceFile.replace('\\', '/'), sourceFile)
      Assert.assertTrue(symbol.lineNumber >= lineNumber)
    }
  }

  @Test
  fun testExeRestart() {
    val symbolizer = createSymbolizer()
    for (arch in architectures) {
      val expectedSymbolsFile = Paths.get(testDataDir, arch, EXPECTED_SYMBOLS_FILE_NAME).toFile()
      Assert.assertTrue(expectedSymbolsFile.exists())
      for (line in expectedSymbolsFile.readLines()) {
        val symParts = line.split('|')
        val offset = symParts[0].toLong(16)
        val name = symParts[1]
        val module = "/path/to/device/modules/" + LIB_FILE_NAME

        val offsetWithinFunction = offset + 1
        val symbol = symbolizer.symbolize(arch, module, offsetWithinFunction)!!
        Assert.assertNotNull(symbol)
        Assert.assertEquals(name, symbol.name)
        symbolizer.stop()
      }
    }
  }

  @Test(expected = IOException::class)
  fun testSymbolizerExeMissing() {
    val symLocator = SymbolFilesLocator(getSymDirMap())
    val notExistingPapth = getLlvmSymbolizerPath().replace("llvm-symbolizer", "not-llvm-symbolizer")
    val symbolizer = LlvmSymbolizer(notExistingPapth, symLocator)
    symbolizer.symbolize("x86", LIB_FILE_NAME, 11)
    Assert.fail("IOException is expected to be thrown by the line above")
  }

  @Test
  fun testLlvmSymbolizerProcFreeze() {
    Assume.assumeFalse(SystemInfo.isWindows) // Windows doesn't have 'yes'
    val symLocator = SymbolFilesLocator(getSymDirMap())
    val yesPath = "yes" // call 'yes' instead llvm-symbolizer to simulate freezing symbolizer
    val symbolizer = LlvmSymbolizer(yesPath, symLocator, 50)
    for (addr in 0L..6L) {
      Assert.assertNull(symbolizer.symbolize("x86", LIB_FILE_NAME, addr))
    }
  }

  @Test
  fun testUnknownSymbols() {
    val symbolizer = createSymbolizer()
    var sym = symbolizer.symbolize("arm", "/p/libnotexists.so", 12345)
    Assert.assertNull(sym)

    sym = symbolizer.symbolize("arm", LIB_FILE_NAME, 0xffffffffff)
    Assert.assertNull(sym)
  }

  fun getSymDirMap(): Map<String, Set<File>> {
    val result: MutableMap<String, Set<File>> = hashMapOf()
    for (arch in architectures) {
      result[arch] = setOf(File(testDataDir, arch))
    }
    return result
  }

  fun createSymbolizer(): NativeSymbolizer {
    val symLocator = SymbolFilesLocator(getSymDirMap())
    return LlvmSymbolizer(getLlvmSymbolizerPath(), symLocator)
  }


}
