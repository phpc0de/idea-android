/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.quickfix

import com.android.tools.idea.lint.common.LintIdeQuickFix
import com.android.tools.idea.lint.common.LintIdeQuickFixProvider
import com.android.tools.lint.checks.ParcelDetector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.intellij.psi.PsiElement


class KotlinAndroidQuickFixProvider : LintIdeQuickFixProvider {
    override fun getQuickFixes(
            issue: Issue,
            startElement: PsiElement,
            endElement: PsiElement,
            message: String,
            fixData: LintFix?
    ): Array<LintIdeQuickFix> {
        val fixes: Array<LintIdeQuickFix> = when (issue) {
            ParcelDetector.ISSUE -> arrayOf(ParcelableQuickFix())
            else -> emptyArray()
        }

        return fixes
    }
}
