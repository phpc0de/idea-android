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
package com.android.tools.idea.layoutinspector.pipeline.transport

import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.resource.data.Resource
import com.android.tools.idea.layoutinspector.util.TestStringTable
import com.android.tools.layoutinspector.proto.LayoutInspectorProto

class TreeBuilder(private val strings: TestStringTable) {

  fun makeViewTree(view: ViewNode): LayoutInspectorProto.View {
    val builder = LayoutInspectorProto.View.newBuilder().apply {
      drawId = view.drawId
      viewId = (strings.add(view.viewId) ?: Resource()).convert()
      layout = (strings.add(view.layout) ?: Resource()).convert()
      x = view.x
      y = view.y
      width = view.width
      height = view.height
      className = strings.add(view.unqualifiedName)
      packageName = strings.add(view.qualifiedName.substringBeforeLast(".", ""))
      textValue = strings.add(view.textValue)
      layoutFlags = view.layoutFlags
    }
    view.children.forEach { builder.addSubView(makeViewTree(it)) }
    return builder.build()
  }
}
