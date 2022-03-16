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
import com.siyeh.ig.migration.TryWithIdenticalCatchesInspection;
import org.jetbrains.annotations.Nullable;

public class AndroidTryWithIdenticalCatchesInspectionTest extends AndroidInspectionTestCase {

  public void testNoIdenticalBranchWarningPre19() {
    addManifest(17);
    doTest("" +
           "package test.pkg;\n" +
           "\n" +
           "@SuppressWarnings(\"unused\")\n" +
           "public class X {\n" +
           "    public void test() {\n" +
           "        try {\n" +
           "            Class.forName(\"name\").newInstance();\n" +
           "        } catch (ClassNotFoundException e) {\n" +
           "            e.printStackTrace();\n" +
           "        } catch (InstantiationException e) {\n" +
           "            e.printStackTrace();\n" +
           "        } catch (IllegalAccessException e) {\n" +
           "            e.printStackTrace();\n" +
           "        }        \n" +
           "    }\n" +
           "}\n");
  }

  public void testIdenticalBranchWarningPost19() {
    addManifest(20);
    doTest("" +
           "package test.pkg;\n" +
           "\n" +
           "@SuppressWarnings(\"unused\")\n" +
           "public class X {\n" +
           "    public void test() {\n" +
           "        try {\n" +
           "            Class.forName(\"name\").newInstance();\n" +
           "        } catch (ClassNotFoundException e) {\n" +
           "            e.printStackTrace();\n" +
           "        } /*'catch' branch identical to 'ClassNotFoundException' branch*/catch (InstantiationException e)/**/ {\n" +
           "            e.printStackTrace();\n" +
           "        } /*'catch' branch identical to 'ClassNotFoundException' branch*/catch (IllegalAccessException e)/**/ {\n" +
           "            e.printStackTrace();\n" +
           "        }        \n" +
           "    }\n" +
           "}\n");
  }

  public void testIsEnabled() {
    // We used to have a fork of this inspection, but now we're using the upstream version combined with AndroidLanguageFeatureProvider.
    // When migrating from one to the other, this got removed from Studio altogether, so here we check it's in at least one XML file.
    assertTrue(getInspection().isEnabledByDefault());
  }

  @Nullable
  @Override
  protected InspectionProfileEntry getInspection() {
    return new TryWithIdenticalCatchesInspection();
  }
}
