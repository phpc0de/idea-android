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
package com.android.tools.idea.gradle.project.sync.messages;

import static com.android.tools.idea.project.messages.MessageType.ERROR;
import static org.junit.Assert.assertSame;

import com.android.tools.idea.project.hyperlink.NotificationHyperlink;
import com.android.tools.idea.project.messages.SyncMessage;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.build.issue.BuildIssueQuickFix;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.ServiceContainerUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleSyncMessagesStub extends GradleSyncMessages {
  @NotNull private final List<SyncMessage> myMessages = new ArrayList<>();

  @NotNull private final List<NotificationData> myNotifications = new ArrayList<>();
  @Nullable private NotificationUpdate myNotificationUpdate;

  @NotNull
  public static GradleSyncMessagesStub replaceSyncMessagesService(@NotNull Project project, @NotNull Disposable parentDisposable) {
    GradleSyncMessagesStub syncMessages = new GradleSyncMessagesStub(project);
    ServiceContainerUtil.replaceService(project, GradleSyncMessages.class, syncMessages, parentDisposable);
    assertSame(syncMessages, GradleSyncMessages.getInstance(project));
    return syncMessages;
  }

  public GradleSyncMessagesStub(@NotNull Project project) {
    super(project);
    Disposer.register(project, this);
  }

  /**
   * Note: This can't override getErrorCount() since tests rely on syncs succeeding even with errors.
   */
  public int getFakeErrorCount() {
    return myMessages.stream().mapToInt(message -> message.getType() == ERROR ? 1 : 0).sum() +
           myNotifications.stream().mapToInt(notification -> notification.getNotificationCategory() == NotificationCategory.ERROR ? 1 : 0)
                          .sum();
  }

  @Override
  public void report(@NotNull SyncMessage message) {
    myMessages.add(message);
  }

  @Nullable
  public SyncMessage getFirstReportedMessage() {
    return myMessages.isEmpty() ? null : myMessages.get(0);
  }

  @NotNull
  public List<SyncMessage> getReportedMessages() {
    return ImmutableList.copyOf(myMessages);
  }

  @Override
  public void report(@NotNull NotificationData notification, @NotNull List<? extends BuildIssueQuickFix> quickFixes) {
    myNotifications.add(notification);
  }

  @NotNull
  public List<NotificationData> getNotifications() {
    return myNotifications;
  }

  @Override
  public void updateNotification(@NotNull NotificationData notification,
                                 @NotNull String text,
                                 @NotNull List<NotificationHyperlink> quickFixes) {
    myNotificationUpdate = new NotificationUpdate(text, quickFixes);
  }

  @Override
  public void addNotificationListener(@NotNull NotificationData notification,
                                      @NotNull List<NotificationHyperlink> quickFixes) {
    myNotificationUpdate = new NotificationUpdate(notification.getMessage(), quickFixes);
  }

  @Nullable
  public NotificationUpdate getNotificationUpdate() {
    return myNotificationUpdate;
  }

  @Override
  public void removeAllMessages() {
    myMessages.clear();
  }

  public static class NotificationUpdate {
    @NotNull private final String myText;
    @NotNull private final List<NotificationHyperlink> myFixes;

    NotificationUpdate(@NotNull String text, @NotNull List<NotificationHyperlink> quickFixes) {
      myText = text;
      myFixes = quickFixes;
    }

    @NotNull
    public String getText() {
      return myText;
    }

    @NotNull
    public List<NotificationHyperlink> getFixes() {
      return myFixes;
    }
  }
}
