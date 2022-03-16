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
package com.android.tools.idea.diagnostics.hprof.navigator

import com.android.tools.idea.diagnostics.hprof.classstore.ClassDefinition
import com.android.tools.idea.diagnostics.hprof.classstore.ClassStore
import com.android.tools.idea.diagnostics.hprof.classstore.HProfMetadata
import com.android.tools.idea.diagnostics.hprof.parser.HProfEventBasedParser
import com.android.tools.idea.diagnostics.hprof.visitors.CreateAuxiliaryFilesVisitor
import gnu.trove.TLongArrayList
import java.lang.RuntimeException
import java.nio.channels.FileChannel

abstract class ObjectNavigator(val classStore: ClassStore, val instanceCount: Long) {

  enum class ReferenceResolution {
    ALL_REFERENCES,
    ONLY_STRONG_REFERENCES,
    NO_REFERENCES
  }

  data class RootObject(val id: Long, val reason: RootReason)

  class NavigationException(message: String) : RuntimeException(message)

  abstract val id: Long

  abstract fun createRootsIterator(): Iterator<RootObject>

  abstract fun goTo(id: Long, referenceResolution: ReferenceResolution = ReferenceResolution.ONLY_STRONG_REFERENCES)

  abstract fun getClass(): ClassDefinition

  abstract fun getReferencesCopy(): TLongArrayList
  abstract fun copyReferencesTo(outReferences: TLongArrayList)

  abstract fun getClassForObjectId(id: Long): ClassDefinition
  abstract fun getRootReasonForObjectId(id: Long): RootReason?

  abstract fun getObjectSize(): Int

  abstract fun getSoftReferenceId(): Long
  abstract fun getWeakReferenceId(): Long
  abstract fun getSoftWeakReferenceIndex(): Int

  fun goToInstanceField(className: String?, fieldName: String) {
    val objectId = getInstanceFieldObjectId(className, fieldName)
    goTo(objectId, ReferenceResolution.ALL_REFERENCES)
  }

  fun getInstanceFieldObjectId(className: String?, name: String): Long {
    val refs = getReferencesCopy()
    if (className != null && className != getClass().undecoratedName) {
      throw NavigationException("Expected $className, got ${getClass().undecoratedName}")
    }
    val indexOfField = getClass().allRefFieldNames(classStore).indexOfFirst { it == name }
    if (indexOfField == -1) {
      throw NavigationException("Missing field $name in ${getClass().name}")
    }
    return refs[indexOfField]
  }

  fun goToStaticField(className: String, fieldName: String) {
    val objectId = getStaticFieldObjectId(className, fieldName)
    goTo(objectId, ReferenceResolution.ALL_REFERENCES)
  }

  private fun getStaticFieldObjectId(className: String, fieldName: String): Long {
    val staticField =
      classStore[className].staticFields.firstOrNull { it.name == fieldName }
      ?: throw NavigationException("Missing static field $fieldName in class $className")
    return staticField.objectId
  }

  companion object {
    fun createOnAuxiliaryFiles(parser: HProfEventBasedParser,
                               auxOffsetsChannel: FileChannel,
                               auxChannel: FileChannel,
                               hprofMetadata: HProfMetadata,
                               instanceCount: Long): ObjectNavigator {
      val createAuxiliaryFilesVisitor = CreateAuxiliaryFilesVisitor(auxOffsetsChannel, auxChannel, hprofMetadata.classStore, parser)

      parser.accept(createAuxiliaryFilesVisitor, "auxFiles")

      val auxBuffer = auxChannel.map(FileChannel.MapMode.READ_ONLY, 0, auxChannel.size())
      val auxOffsetsBuffer =
        auxOffsetsChannel.map(FileChannel.MapMode.READ_ONLY, 0, auxOffsetsChannel.size())

      return ObjectNavigatorOnAuxFiles(hprofMetadata.roots, auxOffsetsBuffer, auxBuffer, hprofMetadata.classStore, instanceCount,
                                       parser.idSize)
    }
  }

  abstract fun isNull(): Boolean

  // Some objects may have additional data (varies by type). Only available when referenceResolution != NO_REFERENCES.
  abstract fun getExtraData(): Int
}

