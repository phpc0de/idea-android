/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.javadoc;

import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.android.tools.idea.testing.AndroidTestUtils;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.android.tools.idea.testing.TestProjectPaths.DEPENDENT_MODULES;
import static com.android.tools.idea.testing.TestProjectPaths.MULTIPLE_MODULE_DEPEND_ON_AAR;
import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;

public class AndroidJavaDocWithGradleTest extends AndroidGradleTestCase {
  @NotNull
  private VirtualFile findFile(@NotNull String path) {
    File filePath = new File(getProject().getBasePath(), FileUtil.toSystemDependentName(path));
    VirtualFile file = findFileByIoFile(filePath, true);
    assertNotNull("File '" + path + "' not found.", file);
    return file;
  }

  private void checkJavadoc(@NotNull String targetPath, @Nullable String expectedDoc) {
    VirtualFile f = findFile(targetPath);
    myFixture.configureFromExistingVirtualFile(f);
    PsiElement originalElement = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
    assert originalElement != null;
    final PsiElement docTargetElement = DocumentationManager.getInstance(getProject()).findTargetElement(
      myFixture.getEditor(), myFixture.getFile(), originalElement);
    assert docTargetElement != null;
    DocumentationProvider provider = DocumentationManager.getProviderFromElement(docTargetElement);
    assertEquals(expectedDoc, provider.generateDoc(docTargetElement, originalElement));
  }

  public void testResource() throws Exception {
    loadProject(DEPENDENT_MODULES);

    checkJavadoc("/app/src/main/res/values/colors.xml",
                 "<html><body><table>" +
                 "<tr><th valign=\"top\">Flavor/Library</th>" +
                 "<th valign=\"top\">Configuration</th>" +
                 "<th valign=\"top\">Value</th></tr><tr><td valign=\"top\"><b>main (testResource.app)</b></td>" +
                 "<td valign=\"top\">Default</td><td valign=\"top\">" +
                 "<table style=\"background-color:rgb(18,52,86);width:200px;text-align:center;vertical-align:middle;\" border=\"0\"><tr height=\"100\">" +
                 "<td align=\"center\" valign=\"middle\" height=\"100\" style=\"color:white\">#123456</td></tr></table><BR/>" +
                 "@color/libColor => #123456<BR/></td></tr>" +
                 "<tr><td valign=\"top\">paid (testResource.app)</td>" +
                 "<td valign=\"top\">Default</td>" +
                 "<td valign=\"top\"><s>" +
                 "<table style=\"background-color:rgb(101,67,33);width:200px;text-align:center;vertical-align:middle;\" border=\"0\">" +
                 "<tr height=\"100\"><td align=\"center\" valign=\"middle\" height=\"100\" style=\"color:white\">#654321</td></tr>" +
                 "</table></s></td></tr>" +
                 "<tr><td valign=\"top\"><b>main (testResource.lib)</b></td>" +
                 "<td valign=\"top\">Default</td>" +
                 "<td valign=\"top\"><s>" +
                 "<table style=\"background-color:rgb(0,0,0);width:200px;text-align:center;vertical-align:middle;\" border=\"0\">" +
                 "<tr height=\"100\"><td align=\"center\" valign=\"middle\" height=\"100\" style=\"color:white\">#000000</td></tr>" +
                 "</table></s></td></tr></table></body></html>");
  }

  public void testResourcesInAar() throws Exception {
    loadProject(MULTIPLE_MODULE_DEPEND_ON_AAR);

    String activityPath = "app/src/main/java/com/example/google/androidx/MainActivity.kt";
    VirtualFile virtualFile = ProjectUtil.guessProjectDir(getProject()).findFileByRelativePath(activityPath);
    myFixture.openFileInEditor(virtualFile);

    // Resource from Aar define in module R class.
    AndroidTestUtils.moveCaret(myFixture, "R.color.abc_tint_default|");
    myFixture.type("\n    R.attr.actionBarDivider");
    checkJavadoc(activityPath,"<html><body><B>actionBarDivider</B><br/>Custom divider drawable to use for elements in the " +
                              "action bar.<br/><hr/><BR/>@attr/actionBarDivider<BR/><BR/></body></html>");


    myFixture.type("\n    androidx.appcompat.R.attr.actionBarDivider");
    checkJavadoc(activityPath,"<html><body><B>actionBarDivider</B><br/>Custom divider drawable to use for elements in the " +
                              "action bar.<br/><hr/><BR/>@attr/actionBarDivider<BR/><BR/></body></html>");
  }
}
