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
package com.android.tools.idea.connection.assistant.actions

import com.android.tools.idea.assistant.AssistActionState
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.Icon

// Custom state that avoids colouring the text because we don't want all output to be green/red.
object CustomSuccessState : AssistActionState {

    override fun isButtonVisible(): Boolean = true

    override fun isButtonEnabled(): Boolean = true

    override fun isMessageVisible(): Boolean = true

    override fun getIcon(): Icon? = AllIcons.RunConfigurations.TestPassed

    override fun getForeground(): Color = JBColor.BLACK
}
