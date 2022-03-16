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
package com.android.tools.idea.uibuilder.scene;

import com.android.tools.idea.common.fixtures.ModelBuilder;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.uibuilder.scout.Scout;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.android.SdkConstants.*;

/**
 * Check that connections to parent if referenced by an id still works, also check the display list sorted result.
 */
public class ScoutTest3 extends SceneTest {
  @Override
  @NotNull
  public ModelBuilder createModel() {
    return model("constraint.xml",
                 component(CONSTRAINT_LAYOUT.defaultName())
                   .id("@+id/content_main")
                   .withBounds(0, 0, 2000, 2000)
                   .width("1000dp")
                   .height("1000dp")
                   .children(
                     component(TEXT_VIEW)
                       .id("@+id/textview1")
                       .withBounds(100, 750, 200, 40)
                       .width("100dp")
                       .height("40dp"),
                     component(CONSTRAINT_LAYOUT_GUIDELINE.defaultName())
                       .id("@+id/guide1")
                       .withBounds(100, 0, 1, 1000)
                       .withAttribute("android:orientation","vertical")
                       .withAttribute("app:layout_constraintGuide_begin","50dp")
                       .width("0dp")
                       .height("30dp"),
                     component(TEXT_VIEW)
                       .id("@+id/textview3")
                       .withBounds(650, 1150, 200, 50)
                       .width("200dp")
                       .height("50dp")
                   ));
  }

  public void testRTLScene() {
    myScreen.get("@+id/textview1")
      .expectXml("<TextView\n" +
                 "    android:id=\"@+id/textview1\"\n" +
                 "    android:layout_width=\"100dp\"\n" +
                 "    android:layout_height=\"40dp\"/>");
    List<NlComponent> list = myModel.getComponents().get(0).getChildren();
    Scout.arrangeWidgets(Scout.Arrange.AlignHorizontallyLeft, list,true);
    Scout.inferConstraintsAndCommit (myModel.getComponents());
    myScreen.get("@+id/textview1")
      .expectXml("<TextView\n" +
                 "        android:id=\"@+id/textview1\"\n" +
                 "        android:layout_width=\"100dp\"\n" +
                 "        android:layout_height=\"40dp\"\n" +
                 "        android:layout_marginTop=\"375dp\"\n" +
                 "        app:layout_constraintStart_toStartOf=\"@+id/guide1\"\n" +
                 "        app:layout_constraintTop_toTopOf=\"parent\" />");
  }
}