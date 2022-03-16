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
package org.jetbrains.android.inspections;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.java18api.Java8ListSortInspection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidJava8ListSortInspectionTest extends AndroidInspectionTestCase {

  public void testNoWarningsPre24() {
    addManifest(23);
    //noinspection all // Sample code
    doTest("" +
           "package test.pkg;\n" +
           "\n" +
           "import java.util.Collections;\n" +
           "import java.util.Comparator;\n" +
           "import java.util.List;\n" +
           "\n" +
           "@SuppressWarnings(\"unused\")\n" +
           "public class X {\n" +
           "\n" +
           "    public void test(List<String> strings, Comparator<String> comparator) {\n" +
           "        Collections.sort(strings, comparator);\n" +
           "    }\n" +
           "}\n");
  }

  public void testWarningsPost24() {
    addManifest(24);
    //noinspection all // Sample code
    doTest("" +
           "package test.pkg;\n" +
           "\n" +
           "import java.util.Collections;\n" +
           "import java.util.Comparator;\n" +
           "import java.util.List;\n" +
           "\n" +
           "@SuppressWarnings(\"unused\")\n" +
           "public class X {\n" +
           "\n" +
           "    public void test(List<String> strings, Comparator<String> comparator) {\n" +
           "        Collections./*Collections.sort could be replaced with List.sort*/sort/**/(strings, comparator);\n" +
           "    }\n" +
           "}\n");
  }

  @Nullable
  @Override
  protected InspectionProfileEntry getInspection() {
    return new Java8ListSortInspection() {
      @NotNull
      @Override
      public String getDisplayName() {
        return "Collections.sort() can be replaced with List.sort()";
      }
    };
  }
}
