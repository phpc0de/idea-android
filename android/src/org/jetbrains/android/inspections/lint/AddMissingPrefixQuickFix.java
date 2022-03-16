package org.jetbrains.android.inspections.lint;

import com.android.SdkConstants;
import com.android.tools.idea.lint.common.LintIdeQuickFix;
import com.android.tools.idea.lint.common.AndroidQuickfixContexts;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlNamespaceHelper;
import java.util.Collections;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

public class AddMissingPrefixQuickFix implements LintIdeQuickFix {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.inspections.lint.AddMissingPrefixQuickFix");

  @Override
  public void apply(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull AndroidQuickfixContexts.Context context) {
    final XmlAttribute attribute = PsiTreeUtil.getParentOfType(startElement, XmlAttribute.class, false);
    if (attribute == null) {
      return;
    }

    final XmlTag tag = attribute.getParent();
    if (tag == null) {
      LOG.debug("tag is null");
      return;
    }

    String androidNsPrefix = tag.getPrefixByNamespace(SdkConstants.ANDROID_URI);

    if (androidNsPrefix == null) {
      final PsiFile file = tag.getContainingFile();
      final XmlNamespaceHelper extension = XmlNamespaceHelper.getHelper(file);

      if (extension == null) {
        LOG.debug("Cannot get XmlNamespaceHelper for file + " + file);
        return;
      }

      if (!(file instanceof XmlFile)) {
        LOG.debug(file + " is not XmlFile");
        return;
      }

      final XmlFile xmlFile = (XmlFile)file;
      final String defaultPrefix = "android";
      extension.insertNamespaceDeclaration(xmlFile, null, Collections.singleton(SdkConstants.ANDROID_URI), defaultPrefix, null);
      androidNsPrefix = defaultPrefix;
    }
    String finalAndroidNsPrefix = androidNsPrefix;
    ApplicationManager.getApplication().runWriteAction(() -> {
      attribute.setName(finalAndroidNsPrefix + ':' + attribute.getLocalName());
    });
  }

  @Override
  public boolean isApplicable(@NotNull PsiElement startElement,
                              @NotNull PsiElement endElement,
                              @NotNull AndroidQuickfixContexts.ContextType contextType) {
    return PsiTreeUtil.getParentOfType(startElement, XmlAttribute.class, false) != null;
  }

  @NotNull
  @Override
  public String getName() {
    return AndroidBundle.message("android.lint.fix.add.android.prefix");
  }
}
