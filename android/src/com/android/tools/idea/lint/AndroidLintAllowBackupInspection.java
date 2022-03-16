/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.lint;

import static com.android.SdkConstants.ATTR_ALLOW_BACKUP;
import static com.android.SdkConstants.ATTR_FULL_BACKUP_CONTENT;

import com.android.resources.ResourceUrl;
import com.android.tools.idea.lint.common.AndroidLintInspectionBase;
import com.android.tools.idea.lint.common.AndroidQuickfixContexts;
import com.android.tools.idea.lint.common.LintIdeQuickFix;
import com.android.tools.idea.lint.common.SetAttributeQuickFix;
import com.android.tools.lint.checks.ManifestDetector;
import com.intellij.psi.PsiElement;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

public class AndroidLintAllowBackupInspection extends AndroidLintInspectionBase {
  public AndroidLintAllowBackupInspection() {
    super(AndroidBundle.message("android.lint.inspections.allow.backup"), ManifestDetector.ALLOW_BACKUP);
  }

  @NotNull
  @Override
  public LintIdeQuickFix[] getQuickFixes(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull String message) {
    ResourceUrl url;
    if (ManifestDetector.MISSING_FULL_BACKUP_CONTENT_RESOURCE.equals(message)
        && (url = ResourceUrl.parse(startElement.getText())) != null) {
      // Find the resource url
      return new LintIdeQuickFix[]{
        new GenerateBackupDescriptorFix(url)
      };
    }
    else {
      return new LintIdeQuickFix[]{
        new SetAttributeQuickFix("Set backup attribute", null, ATTR_ALLOW_BACKUP, null),
        // The fullBackupContent quick fix should only be visible if the attribute is not set
        // and the allowBackup attribute is set to true.
        new SetAttributeQuickFix("Set fullBackupContent attribute", null, ATTR_FULL_BACKUP_CONTENT, null) {
          @Override
          public boolean isApplicable(@NotNull PsiElement startElement,
                                      @NotNull PsiElement endElement,
                                      @NotNull AndroidQuickfixContexts.ContextType contextType) {
            return SetAndGenerateBackupDescriptor.isAllowBackupEnabled(startElement)
                   && super.isApplicable(startElement, endElement, contextType);
          }
        },
        // Set the attribute as well as generate the backup descriptor.
        new SetAndGenerateBackupDescriptor()
      };
    }
  }
}
