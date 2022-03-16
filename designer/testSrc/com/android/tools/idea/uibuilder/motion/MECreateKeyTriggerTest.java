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
package com.android.tools.idea.uibuilder.motion;

import com.android.tools.idea.uibuilder.handlers.motion.editor.adapters.MTag;
import com.android.tools.idea.uibuilder.handlers.motion.editor.createDialogs.CreateKeyTrigger;
import com.android.tools.idea.uibuilder.handlers.motion.editor.ui.MeModel;
import com.android.tools.idea.uibuilder.handlers.motion.editor.ui.MotionEditor;
import com.android.tools.idea.uibuilder.handlers.motion.editor.ui.MotionEditorSelector;
import com.android.tools.idea.uibuilder.motion.adapters.BaseMotionEditorTest;
import java.awt.*;

public class MECreateKeyTriggerTest extends BaseMotionEditorTest {
  static class CreatorAccess extends CreateKeyTrigger {
    void access_populateDialog() {
      populateDialog();
    }

    @Override
    public void dismissPopup() { // override dismiss
    }

    void fillAttributes() {
      mMatchTag.setText("test32");
    }
  }

  public void testCreateKeyTriggerLayout() {
    CreatorAccess panel = new CreatorAccess();
    String layout = "0,CreatorAccess      ,0,0,99,99\n" +
                    "1,JLabel             ,4,2,90,4\n" +
                    "1,JSeparator         ,0,8,99,0\n" +
                    "1,JRadioButton       ,4,11,42,6\n" +
                    "1,JRadioButton       ,57,11,37,6\n" +
                    "1,JPanel             ,5,19,89,6\n" +
                    "2,PromptedTextField  ,0,0,0,0\n" +
                    "2,MEComboBox         ,0,0,0,0\n" +
                    "1,JSeparator         ,0,28,99,0\n" +
                    "1,JLabel             ,4,31,90,4\n" +
                    "1,MEComboBox         ,5,37,89,6\n" +
                    "1,JLabel             ,4,46,90,4\n" +
                    "1,PromptedTextField  ,5,52,89,5\n" +
                    "1,JLabel             ,4,60,90,4\n" +
                    "1,PromptedTextField  ,5,65,89,5\n" +
                    "1,JLabel             ,4,73,90,4\n" +
                    "1,PromptedTextField  ,5,78,89,5\n" +
                    "1,JButton            ,4,88,90,7\n";
    Dimension size = panel.getPreferredSize();
    panel.setBounds(0, 0, size.width, size.height);
    panel.doLayout();
    panel.validate();
    String actual = componentTreeToString(panel, 0, null);
    if (!similar(layout, actual,4)) {
      assertEquals(layout, actual);
    }
  }

  public void testCreateKeyTriggerAction() {
    CreatorAccess panel = new CreatorAccess();
    MotionEditor motionSceneUi = new MotionEditor();
    MeModel model = getModel();
    MTag[] trans = model.motionScene.getChildTags("Transition");
    model.setSelected(MotionEditorSelector.Type.TRANSITION, trans);
    motionSceneUi.setMTag(model);
    panel.getAction(motionSceneUi, motionSceneUi);
    panel.access_populateDialog();

    String info = "0,CreatorAccess,\n" +
                  "1,JLabel,CREATE KEY TRIGGER\n" +
                  "1,JSeparator,\n" +
                  "1,JRadioButton,\n" +
                  "1,JRadioButton,\n" +
                  "1,JPanel,\n" +
                  "2,PromptedTextField,tag or regex\n" +
                  "2,MEComboBox,number,dial_pad,dialtitle,button1,button2,button3,button4,button5,button6,button7,button8,button9,button10,button11,button12,people_pad,people_title,people1,people2,people3,people4,people5,people6,people7,people8\n" +
                  "1,JSeparator,\n" +
                  "1,JLabel,Type\n" +
                  "1,MEComboBox,Position,Collision\n" +
                  "1,JLabel,Position\n" +
                  "1,PromptedTextField,0-100\n" +
                  "1,JLabel,Collide width\n" +
                  "1,PromptedTextField,triggerId\n" +
                  "1,JLabel,onCross\n" +
                  "1,PromptedTextField,method\n" +
                  "1,JButton,Add\n";
    assertEquals(info, componentFieldsString(panel, 0));
    panel.fillAttributes();
    MTag tag = panel.create();
    String created = "\n" +
                     "<KeyTrigger\n" +
                     "   motion:framePosition=\"0\"\n" +
                     "   motion:motionTarget=\"@+id/number\"\n" +
                     "   motion:onCross=\"method\" />\n";
    assertEquals(created, tag.toFormalXmlString(""));
  }
}
