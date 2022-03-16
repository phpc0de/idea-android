/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.build.output;

import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FileMessageEvent;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.output.BuildOutputInstantReader;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GradleBuildOutputParserTest {
  @Mock private BuildOutputInstantReader myReader;
  @Mock private Consumer<BuildEvent> myConsumer;
  @Nullable private GradleBuildOutputParser myParser;

  @Before
  public void setUp() {
    initMocks(this);
    myParser = new GradleBuildOutputParser();
  }

  @Test
  public void parseWithError() {
    String line =
      "AGPBI: {\"kind\":\"error\",\"text\":\"Error message.\",\"sources\":[{\"file\":\"/app/src/main/res/layout/activity_main.xml\",\"position\":{\"startLine\":10,\"startColumn\":31,\"startOffset\":456,\"endColumn\":44,\"endOffset\":469}}],\"tool\":\"AAPT\"}";
    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK");

    ArgumentCaptor<MessageEvent> messageCaptor = ArgumentCaptor.forClass(MessageEvent.class);
    String detailLine = "This is a detail line";
    assertTrue(myParser.parse(line, myReader, myConsumer));
    assertFalse(myParser.parse(detailLine, myReader, myConsumer));

    verify(myConsumer).accept(messageCaptor.capture());

    List<MessageEvent> generatedMessages = messageCaptor.getAllValues();
    assertThat(generatedMessages).hasSize(1);
    assertThat(generatedMessages.get(0)).isInstanceOf(FileMessageEvent.class);
    FileMessageEvent fileMessageEvent = (FileMessageEvent)generatedMessages.get(0);

    assertThat(fileMessageEvent.getGroup()).isEqualTo("AAPT errors");
    assertThat(fileMessageEvent.getKind()).isEqualTo(MessageEvent.Kind.ERROR);
    assertThat(fileMessageEvent.getResult().getDetails()).isEqualTo("Error message.");
  }

  @Test
  public void parseWithWarningNoSource() {
    String line = "AGPBI: {\"kind\":\"warning\",\"text\":\"Warning message.\",\"sources\":[{}],\"tool\":\"D8\"}";
    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK");

    ArgumentCaptor<MessageEvent> messageCaptor = ArgumentCaptor.forClass(MessageEvent.class);
    String detailLine = "This is a detail line";
    assertTrue(myParser.parse(line, myReader, myConsumer));
    assertFalse(myParser.parse(detailLine, myReader, myConsumer));
    verify(myConsumer).accept(messageCaptor.capture());

    List<MessageEvent> generatedMessages = messageCaptor.getAllValues();
    assertThat(generatedMessages).hasSize(1);
    assertThat(generatedMessages.get(0)).isNotInstanceOf(FileMessageEvent.class);

    MessageEvent messageEvent = generatedMessages.get(0);

    assertThat(messageEvent.getGroup()).isEqualTo("D8 warnings");
    assertThat(messageEvent.getKind()).isEqualTo(MessageEvent.Kind.WARNING);
    assertThat(messageEvent.getResult().getDetails()).isEqualTo("Warning message.");
  }

  @Test
  public void parseWithoutError() {
    String line = "Non AGBPI error";
    assertFalse(myParser.parse(line, myReader, myConsumer));
  }

  @Test
  public void parseWithErrorIgnoreOutput() {
    String line =
      "AGPBI: {\"kind\":\"error\",\"text\":\"Error message.\",\"sources\":[{}],\"original\":\"Error line 1\\nError line 2\\nError line 3\",\"tool\":\"Dex\"}";

    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK");

    ArgumentCaptor<MessageEvent> messageCaptor = ArgumentCaptor.forClass(MessageEvent.class);
    String[] detailLines = new String[5];
    detailLines[0] = "Error line 1";
    detailLines[1] = "Error line 2";
    detailLines[2] = "Error line 3";
    detailLines[3] = "Unrelated output";
    detailLines[4] = "Error line 1";

    assertTrue(myParser.parse(line, myReader, myConsumer));
    assertTrue(myParser.parse(detailLines[0], myReader, myConsumer));
    assertTrue(myParser.parse(detailLines[1], myReader, myConsumer));
    assertTrue(myParser.parse(detailLines[2], myReader, myConsumer));
    assertFalse(myParser.parse(detailLines[3], myReader, myConsumer));
    assertFalse(myParser.parse(detailLines[4], myReader, myConsumer));

    verify(myConsumer).accept(messageCaptor.capture());

    List<MessageEvent> generatedMessages = messageCaptor.getAllValues();
    assertThat(generatedMessages).hasSize(1);
    assertThat(generatedMessages.get(0)).isNotInstanceOf(FileMessageEvent.class);

    MessageEvent messageEvent = generatedMessages.get(0);

    assertThat(messageEvent.getGroup()).isEqualTo("Dex errors");
    assertThat(messageEvent.getKind()).isEqualTo(MessageEvent.Kind.ERROR);
    assertThat(messageEvent.getResult().getDetails()).isEqualTo("Error line 1\nError line 2\nError line 3");
  }

  @Test
  public void parseChangeBuildId() {
    String line =
      "AGPBI: {\"kind\":\"error\",\"text\":\"Error message.\",\"sources\":[{}],\"original\":\"Error line 1\\nError line 2\\nError line 3\",\"tool\":\"Dex\"}";

    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK_1");

    String[] detailLines = new String[4];
    detailLines[0] = "Error line 1";
    detailLines[1] = "Error line 2";
    detailLines[2] = "Error line 3";
    detailLines[3] = "Unrelated output";

    assertTrue(myParser.parse(line, myReader, myConsumer));

    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK_2");
    assertFalse(myParser.parse(detailLines[0], myReader, myConsumer));

    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK_3");
    assertFalse(myParser.parse(detailLines[1], myReader, myConsumer));

    when(myReader.getParentEventId()).thenReturn("BUILD_ID_MOCK_1");
    assertTrue(myParser.parse(detailLines[2], myReader, myConsumer));

    assertFalse(myParser.parse(detailLines[3], myReader, myConsumer));
  }
}
