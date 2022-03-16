/*
 * Copyright (C) 2017 The Android Open Source Project
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
package org.jetbrains.android.formatter;

import com.android.tools.idea.io.TestFileUtils;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.arrangement.engine.ArrangementEngine;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.intellij.lang.annotations.Language;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.annotations.NotNull;

public final class AndroidXmlArrangementTest extends AndroidTestCase {
  public void testArrangement() throws IOException {
    Path path = Paths.get(myModule.getProject().getBasePath(), "app", "src", "main", "res", "layout", "relative_layout.xml");

    @Language("XML")
    String shuffledContents = "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                              "    android:layout_width=\"\"\n" +
                              "    android:layout_height=\"\">\n" +
                              "\n" +
                              "    <TextView\n" +
                              "        android:layout_height=\"\"\n" +
                              "        android:layout_toLeftOf=\"\"\n" +
                              "        android:layout_toRightOf=\"\"\n" +
                              "        android:layout_toEndOf=\"\"\n" +
                              "        android:layout_c=\"\"\n" +
                              "        android:layout_marginVertical=\"\"\n" +
                              "        android:layout_width=\"\"\n" +
                              "        android:layout_marginTop=\"\"\n" +
                              "        android:layout_b=\"\"\n" +
                              "        android:layout_a=\"\"\n" +
                              "        android:layout_toStartOf=\"\" />\n" +
                              "</RelativeLayout>\n";

    VirtualFile virtualFile = TestFileUtils.writeFileAndRefreshVfs(path, shuffledContents);

    arrange(virtualFile);

    @Language("XML")
    String arrangedContents = "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                              "    android:layout_width=\"\"\n" +
                              "    android:layout_height=\"\">\n" +
                              "\n" +
                              "    <TextView\n" +
                              "        android:layout_width=\"\"\n" +
                              "        android:layout_height=\"\"\n" +
                              "        android:layout_a=\"\"\n" +
                              "        android:layout_b=\"\"\n" +
                              "        android:layout_c=\"\"\n" +
                              "        android:layout_marginVertical=\"\"\n" +
                              "        android:layout_marginTop=\"\"\n" +
                              "        android:layout_toStartOf=\"\"\n" +
                              "        android:layout_toLeftOf=\"\"\n" +
                              "        android:layout_toEndOf=\"\"\n" +
                              "        android:layout_toRightOf=\"\" />\n" +
                              "</RelativeLayout>\n";

    assertEquals(arrangedContents, FileDocumentManager.getInstance().getDocument(virtualFile).getText());
  }

  public void testArrangeDoesntRearrangeElements() throws IOException {
    // Arrange
    Path path = Paths.get(myModule.getProject().getBasePath(), "app", "src", "main", "res", "layout", "frame_layout.xml");

    @Language("XML")
    String contents = "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:orientation=\"vertical\"\n" +
                      "    android:layout_width=\"match_parent\"\n" +
                      "    android:layout_height=\"match_parent\">\n" +
                      "\n" +
                      "    <View\n" +
                      "        android:layout_width=\"match_parent\"\n" +
                      "        android:background=\"#000\"\n" +
                      "        android:layout_height=\"match_parent\" />\n" +
                      "\n" +
                      "    <TextView\n" +
                      "        android:text=\"Blabla\"\n" +
                      "        android:layout_gravity=\"center\"\n" +
                      "        android:textColor=\"#FFF\"\n" +
                      "        android:layout_width=\"wrap_content\"\n" +
                      "        android:layout_height=\"wrap_content\" />\n" +
                      "</FrameLayout>\n";

    VirtualFile virtualFile = TestFileUtils.writeFileAndRefreshVfs(path, contents);

    // Act
    arrange(virtualFile);

    // Assert
    @Language("XML")
    String expectedContents = "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                              "    android:layout_width=\"match_parent\"\n" +
                              "    android:layout_height=\"match_parent\"\n" +
                              "    android:orientation=\"vertical\">\n" +
                              "\n" +
                              "    <View\n" +
                              "        android:layout_width=\"match_parent\"\n" +
                              "        android:layout_height=\"match_parent\"\n" +
                              "        android:background=\"#000\" />\n" +
                              "\n" +
                              "    <TextView\n" +
                              "        android:layout_width=\"wrap_content\"\n" +
                              "        android:layout_height=\"wrap_content\"\n" +
                              "        android:layout_gravity=\"center\"\n" +
                              "        android:text=\"Blabla\"\n" +
                              "        android:textColor=\"#FFF\" />\n" +
                              "</FrameLayout>\n";

    assertEquals(expectedContents, FileDocumentManager.getInstance().getDocument(virtualFile).getText());
  }

  private void arrange(@NotNull VirtualFile virtualFile) {
    Project project = myModule.getProject();

    WriteCommandAction.runWriteCommandAction(project, () -> {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
      ArrangementEngine.getInstance().arrange(psiFile, Collections.singleton(psiFile.getTextRange()));
    });
  }
}
