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
package com.android.emulator

import com.android.emulator.snapshot.SnapshotOuterClass.Snapshot
import com.intellij.util.text.nullize
import java.io.File
import java.io.FileInputStream

class SnapshotProtoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads an Emulator [Snapshot] protobuf and makes the fields available.
 *
 * Throws [SnapshotProtoException] if the protobuf does not exist or is invalid.
 */
class SnapshotProtoParser
@Throws(SnapshotProtoException::class)
constructor(snapshotProtobufFile: File, private val fileName: String) {
  private val snapshot: Snapshot

  val logicalName: String
    get() = snapshot.logicalName.nullize() ?: fileName

  val creationTime: Long
    get() = snapshot.creationTime

  init {
    if (!snapshotProtobufFile.isFile) {
      throw SnapshotProtoException(
        "Snapshot file " + snapshotProtobufFile.absolutePath + " does not exist.")
    }
    snapshot = FileInputStream(snapshotProtobufFile).use {
      Snapshot.parseFrom(it)
    }
    if (snapshot.imagesCount <= 0) {
      // Treat a degenerate protobuf as invalid
      throw SnapshotProtoException("Snapshot protobuf is empty.")
    }
  }
}
