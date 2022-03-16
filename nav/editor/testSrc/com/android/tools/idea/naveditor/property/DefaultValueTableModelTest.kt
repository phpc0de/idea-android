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
package com.android.tools.idea.naveditor.property

import com.android.tools.idea.naveditor.NavModelBuilderUtil.navigation
import com.android.tools.idea.naveditor.NavTestCase
import com.android.tools.idea.naveditor.model.argumentName
import com.android.tools.idea.naveditor.property.ui.DefaultValueModel
import com.android.tools.idea.naveditor.property.ui.DefaultValueTableModel

class DefaultValueTableModelTest : NavTestCase() {
  fun testDefaultValueTableModel() {
    val model = model("nav.xml") {
      navigation("root", startDestination = "fragment1") {
        fragment("fragment1", layout = "activity_main") {
          argument("argument1", "string", value = "foo")
          argument("argument2", "int", value = "10")
          argument("argument3", "bool", value = "true")
        }
        action("action1", "fragment1") {
          argument("argument2", value = "15")
        }
      }
    }

    val fragment1 = model.find("fragment1")!!
    val action1 = model.find("action1")!!

    val table = DefaultValueTableModel(fragment1.children
                                         .sortedBy { it.argumentName }
                                         .map { DefaultValueModel(it, action1) })

    assertEquals(table.rowCount, 3)
    assertEquals(table, 0, "argument1", "string", "")
    assertEquals(table, 1, "argument2", "int", "15")
    assertEquals(table, 2, "argument3", "bool", "")

    assertEquals(action1.children.size, 1)
    assertNull(action1.children.firstOrNull { it.argumentName == "argument1" })

    table.setValueAt("bar", 0, 2)

    assertEquals(table.rowCount, 3)
    assertEquals(table, 0, "argument1", "string", "bar")
    assertEquals(table, 1, "argument2", "int", "15")
    assertEquals(table, 2, "argument3", "bool", "")

    assertEquals(action1.children.size, 2)
    assertNotNull(action1.children.firstOrNull { it.argumentName == "argument1" })
  }

  private fun assertEquals(table: DefaultValueTableModel, index: Int, name: String, type: String, defaultValue: String) {
    assertEquals(table.getValueAt(index, 0), name)
    assertEquals(table.getValueAt(index, 1), type)
    assertEquals(table.getValueAt(index, 2), defaultValue)
  }
}