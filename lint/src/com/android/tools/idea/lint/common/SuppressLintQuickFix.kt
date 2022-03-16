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
package com.android.tools.idea.lint.common

import com.android.SdkConstants
import com.android.SdkConstants.FQCN_SUPPRESS_LINT
import com.android.tools.idea.lint.common.AndroidLintInspectionBase.LINT_INSPECTION_PREFIX
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtilCore
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.plugins.groovy.GroovyLanguage
import java.util.ArrayList

class SuppressLintQuickFix(private val id: String, element: PsiElement? = null) : SuppressQuickFix {
  private val label = displayName(element, id)

  override fun isAvailable(project: Project, context: PsiElement): Boolean = true

  override fun isSuppressAll(): Boolean {
    return false
  }

  override fun getName(): String = label

  override fun getFamilyName(): String {
    return "Suppress"
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement ?: return
    applyFix(element)
  }

  fun applyFix(element: PsiElement) {
    when (element.language) {
      JavaLanguage.INSTANCE -> handleJava(element)
      XMLLanguage.INSTANCE -> handleXml(element)
      GroovyLanguage -> handleGroovy(element)
      KotlinLanguage.INSTANCE -> handleKotlin(element)
      else -> {
        // Suppressing lint checks tagged on things like icons
        val file = if (element is PsiFile) element else element.containingFile ?: return
        handleFile(file)
      }
    }
  }

  @Throws(IncorrectOperationException::class)
  private fun handleXml(element: PsiElement) {
    val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false) ?: return
    if (!FileModificationService.getInstance().preparePsiElementForWrite(tag)) {
      return
    }
    val file = if (tag is XmlFile) tag else tag.containingFile as? XmlFile ?: return
    val lintId = getLintId(id)
    addSuppressAttribute(file, tag, lintId)
  }

  @Throws(IncorrectOperationException::class)
  private fun handleJava(element: PsiElement) {
    val container = findJavaAnnotationTarget(element) ?: return
    if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) {
      return
    }
    val project = element.project
    val lintId = id.removePrefix(LINT_INSPECTION_PREFIX)
    addSuppressAnnotation(project, container, container, lintId)
  }

  @Throws(IncorrectOperationException::class)
  private fun handleGroovy(element: PsiElement) {
    val file = if (element is PsiFile) element else element.containingFile ?: return
    if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) {
      return
    }
    val project = file.project
    val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
    val offset = element.textOffset
    val line = document.getLineNumber(offset)
    val lineStart = document.getLineStartOffset(line)
    if (lineStart > 0) {
      val prevLineStart = document.getLineStartOffset(line - 1)
      val prevLineEnd = document.getLineEndOffset(line - 1)
      val prevLine = document.getText(TextRange(prevLineStart, prevLineEnd))
      val index = prevLine.indexOf(NO_INSPECTION_PREFIX)
      if (index != -1) {
        document.insertString(prevLineStart + index + NO_INSPECTION_PREFIX.length,
                              getLintId(id) + ",")
        return
      }
    }
    val linePrefix = document.getText(TextRange(lineStart, offset))
    var nonSpace = 0
    while (nonSpace < linePrefix.length) {
      if (!Character.isWhitespace(linePrefix[nonSpace])) {
        break
      }
      nonSpace++
    }
    ApplicationManager.getApplication().assertWriteAccessAllowed()
    document.insertString(lineStart + nonSpace, NO_INSPECTION_PREFIX + getLintId(id) + "\n" + linePrefix.substring(0, nonSpace))
  }

  @Throws(IncorrectOperationException::class)
  private fun handleFile(file: PsiFile) {
    val virtualFile = file.virtualFile
    if (virtualFile != null) {
      val binaryFile = VfsUtilCore.virtualToIoFile(virtualFile)
      // Can't suppress lint checks inside a binary file (typically an icon): use
      // the lint XML facility instead
      val module = ModuleUtilCore.findModuleForPsiElement(file)
      if (module != null) {
        val dir = LintIdeProject.getLintProjectDirectory(module)
        if (dir != null) {
          val project = file.project
          val client = LintIdeSupport.get().createClient(project)
          val lintProject = client.getProject(dir, dir)
          val configuration = client.getConfiguration(lintProject, null)
          configuration.ignore(id, binaryFile)
        }
      }
    }
  }

  @Throws(IncorrectOperationException::class)
  private fun handleKotlin(element: PsiElement) {
    val annotationContainer = PsiTreeUtil.findFirstParent(element, true) { it.isSuppressLintTarget() } ?: return
    if (!FileModificationService.getInstance().preparePsiElementForWrite(annotationContainer)) {
      return
    }

    val argument = "\"${getLintId(id)}\""

    when (annotationContainer) {
      is KtModifierListOwner -> {
        annotationContainer.addAnnotation(
          FqName(getAnnotationClass(element)),
          argument,
          whiteSpaceText = if (annotationContainer.isNewLineNeededForAnnotation()) "\n" else " ",
          addToExistingAnnotation = { entry -> addArgumentToAnnotation(entry, argument) })
      }
    }
  }

  private fun PsiElement.isSuppressLintTarget(): Boolean {
    return this is KtDeclaration &&
           (this as? KtProperty)?.hasBackingField() ?: true &&
           this !is KtFunctionLiteral &&
           this !is KtDestructuringDeclaration &&
           this !is KtClassInitializer
  }

  override fun startInWriteAction(): Boolean {
    return true
  }

  companion object {
    private const val NO_INSPECTION_PREFIX = "//" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME + " "

    private fun getAnnotationClass(context: PsiElement): String {
      val project = context.project

      val module = ModuleUtilCore.findModuleForPsiElement(context)
      val scope = module?.getModuleWithDependenciesAndLibrariesScope(false)
                  ?: GlobalSearchScope.allScope(project)
      return when {
        JavaPsiFacade.getInstance(project).findClass(FQCN_SUPPRESS_LINT, scope) != null -> FQCN_SUPPRESS_LINT
        context.language == KotlinLanguage.INSTANCE -> "kotlin.Suppress"
        else -> "java.lang.SuppressWarnings"
      }
    }

    fun getLintId(intentionId: String): String {
      return intentionId.removePrefix(LINT_INSPECTION_PREFIX)
    }

    @Throws(IncorrectOperationException::class)
    private fun addSuppressAttribute(file: XmlFile, element: XmlTag, id: String) {
      val attribute = element.getAttribute(SdkConstants.ATTR_IGNORE, SdkConstants.TOOLS_URI)
      val value: String
      if (attribute == null || attribute.value == null) {
        value = id
      }
      else {
        val ids = ArrayList<String>()
        for (existing in Splitter.on(',').trimResults().split(attribute.value!!)) {
          if (existing != id) {
            ids.add(existing)
          }
        }
        ids.add(id)
        ids.sort()
        value = Joiner.on(',').join(ids)
      }
      LintIdeSupport.get().ensureNamespaceImported(file, SdkConstants.TOOLS_URI, null)
      element.setAttribute(SdkConstants.ATTR_IGNORE, SdkConstants.TOOLS_URI, value)
    }

    // Based on the equivalent code in com.intellij.codeInsight.daemon.impl.actions.SuppressFix
    // to add @SuppressWarnings annotations

    @Throws(IncorrectOperationException::class)
    private fun addSuppressAnnotation(project: Project,
                                      container: PsiElement,
                                      modifierOwner: PsiModifierListOwner,
                                      id: String) {
      val annotationName = getAnnotationClass(container)
      val annotation = AnnotationUtil.findAnnotation(modifierOwner, annotationName)
      val newAnnotation = createNewAnnotation(project, container, annotation, id)
      if (newAnnotation != null) {
        if (annotation != null && annotation.isPhysical) {
          annotation.replace(newAnnotation)
        }
        else {
          val attributes = newAnnotation.parameterList.attributes
          AddAnnotationFix(annotationName, modifierOwner, attributes).invoke(project, null,
                                                                             container.containingFile)/*editor*/
        }
      }
    }

    private fun createNewAnnotation(project: Project,
                                    container: PsiElement,
                                    annotation: PsiAnnotation?,
                                    id: String): PsiAnnotation? {
      if (annotation != null) {
        val currentSuppressedId = "\"" + id + "\""
        val annotationText = annotation.text
        if (!annotationText.contains("{")) {
          val attributes = annotation.parameterList.attributes
          if (attributes.size == 1) {
            val suppressedWarnings = attributes[0].text
            return if (suppressedWarnings.contains(currentSuppressedId)) null
            else JavaPsiFacade.getInstance(
              project).elementFactory.createAnnotationFromText(
              "@${getAnnotationClass(container)}({$suppressedWarnings, $currentSuppressedId})", container)

          }
        }
        else {
          val curlyBraceIndex = annotationText.lastIndexOf('}')
          if (curlyBraceIndex > 0) {
            val oldSuppressWarning = annotationText.substring(0, curlyBraceIndex)
            return if (oldSuppressWarning.contains(currentSuppressedId)) null
            else JavaPsiFacade.getInstance(
              project).elementFactory.createAnnotationFromText(
              "$oldSuppressWarning, $currentSuppressedId})", container)
          }
        }
      }
      else {
        return JavaPsiFacade.getInstance(project).elementFactory
          .createAnnotationFromText("@${getAnnotationClass(container)}(\"$id\")", container)
      }
      return null
    }

    private fun addArgumentToAnnotation(entry: KtAnnotationEntry, argument: String): Boolean {
      // add new arguments to an existing entry
      val args = entry.valueArgumentList
      val psiFactory = KtPsiFactory(entry)
      val newArgList = psiFactory.createCallArguments("($argument)")
      when {
        args == null -> // new argument list
          entry.addAfter(newArgList, entry.lastChild)
        args.arguments.isEmpty() -> // replace '()' with a new argument list
          args.replace(newArgList)
        args.arguments.none { it.textMatches(argument) } ->
          args.addArgument(newArgList.arguments[0])
      }

      return true
    }

    fun displayName(element: PsiElement?, inspectionId: String): String {
      val id = getLintId(inspectionId)
      return when (element?.language) {
        XMLLanguage.INSTANCE -> LintBundle.message("android.lint.fix.suppress.lint.api.attr", id)
        JavaLanguage.INSTANCE, KotlinLanguage.INSTANCE -> LintBundle.message("android.lint.fix.suppress.lint.api.annotation", id)
        GroovyLanguage -> "Suppress: Add //noinspection $id"
        else -> "Suppress $id"
      }
    }
  }
}
