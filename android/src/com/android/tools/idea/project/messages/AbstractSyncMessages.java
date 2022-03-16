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
package com.android.tools.idea.project.messages;

import static com.intellij.openapi.externalSystem.service.notification.NotificationSource.PROJECT_SYNC;
import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;

import com.android.annotations.concurrency.GuardedBy;
import com.android.tools.idea.gradle.project.build.events.AndroidSyncIssueEvent;
import com.android.tools.idea.gradle.project.build.events.AndroidSyncIssueEventResult;
import com.android.tools.idea.gradle.project.build.events.AndroidSyncIssueFileEvent;
import com.android.tools.idea.gradle.project.build.events.AndroidSyncIssueQuickFix;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.android.tools.idea.project.hyperlink.NotificationHyperlink;
import com.android.tools.idea.ui.QuickFixNotificationListener;
import com.android.tools.idea.util.PositionInFile;
import com.intellij.build.SyncViewManager;
import com.intellij.build.events.Failure;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.issue.BuildIssueQuickFix;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.util.containers.ContainerUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSyncMessages implements Disposable {

  private Project myProject;

  @NotNull
  private final Object myLock = new Object();

  @GuardedBy("myLock")
  @NotNull
  private final HashMap<Object, List<NotificationData>> myCurrentNotifications = new HashMap<>();
  @GuardedBy("myLock")
  @NotNull
  private final HashMap<Object, List<Failure>> myShownFailures = new HashMap<>();
  @NotNull private static final String PENDING_TASK_ID = "Pending taskId";

  protected AbstractSyncMessages(@NotNull Project project) {
    myProject = project;
  }

  public int getErrorCount() {
    return countNotifications(notification -> notification.getNotificationCategory() == NotificationCategory.ERROR);
  }

  public int getMessageCount(@NotNull String groupName) {
    return countNotifications(notification -> notification.getTitle().equals(groupName));
  }

  /**
   * @return A string description containing sync issue groups, for example, "Unresolved dependencies".
   */
  @NotNull
  public String getErrorDescription() {
    Set<String> errorGroups = new LinkedHashSet<>();
    synchronized (myLock) {
      for (List<NotificationData> notificationDataList : myCurrentNotifications.values()) {
        for (NotificationData notificationData : notificationDataList) {
          if (notificationData.getNotificationCategory() == NotificationCategory.ERROR) {
            errorGroups.add(notificationData.getTitle());
          }
        }
      }
    }
    return String.join(", ", errorGroups);
  }

  private int countNotifications(@NotNull Predicate<NotificationData> condition) {
    int total = 0;

    synchronized (myLock) {
      for (List<NotificationData> notificationDataList : myCurrentNotifications.values()) {
        for (NotificationData notificationData : notificationDataList) {
          if (condition.test(notificationData)) {
            total++;
          }
        }
      }
    }
    return total;
  }

  public boolean isEmpty() {
    synchronized (myLock) {
      return myCurrentNotifications.isEmpty();
    }
  }

  public void removeAllMessages() {
    synchronized (myLock) {
      myCurrentNotifications.clear();
    }
  }

  public void report(@NotNull SyncMessage message) {
    String title = message.getGroup();
    String text = join(message.getText(), "\n");
    NotificationCategory category = message.getType().convertToCategory();
    PositionInFile position = message.getPosition();

    NotificationData notification = createNotification(title, text, category, position);

    Navigatable navigatable = message.getNavigatable();
    notification.setNavigatable(navigatable);

    List<NotificationHyperlink> quickFixes = message.getQuickFixes();
    if (!quickFixes.isEmpty()) {
      updateNotification(notification, text, quickFixes);
    }

    report(notification, ContainerUtil.map(quickFixes, it -> new AndroidSyncIssueQuickFix(it)));
  }

  @NotNull
  public NotificationData createNotification(@NotNull String title,
                                             @NotNull String text,
                                             @NotNull NotificationCategory category,
                                             @Nullable PositionInFile position) {
    NotificationSource source = PROJECT_SYNC;
    if (position != null) {
      String filePath = virtualToIoFile(position.file).getPath();
      return new NotificationData(title, text, category, source, filePath, position.line, position.column, false);
    }
    return new NotificationData(title, text, category, source);
  }

  public void updateNotification(@NotNull NotificationData notification,
                                 @NotNull String text,
                                 @NotNull List<NotificationHyperlink> quickFixes) {
    String message = text;
    int hyperlinkCount = quickFixes.size();
    if (hyperlinkCount > 0) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < hyperlinkCount; i++) {
        b.append(quickFixes.get(i).toHtml());
        if (i < hyperlinkCount - 1) {
          b.append("<br>");
        }
      }
      message += ('\n' + b.toString());
    }
    notification.setMessage(message);

    addNotificationListener(notification, quickFixes);
  }

  // Call this method only if notification contains detailed text message with hyperlinks
  // Use updateNotification otherwise
  public void addNotificationListener(@NotNull NotificationData notification, @NotNull List<NotificationHyperlink> quickFixes) {
    for (NotificationHyperlink quickFix : quickFixes) {
      notification.setListener(quickFix.getUrl(), new QuickFixNotificationListener(myProject, quickFix));
    }
  }

  public void report(@NotNull NotificationData notification, @NotNull List<? extends BuildIssueQuickFix> quickFixes) {
    // Save on array to be shown by build view later.
    Object taskId = GradleSyncState.getInstance(myProject).getExternalSystemTaskId();
    if (taskId == null) {
      taskId = PENDING_TASK_ID;
    }
    else {
      showNotification(notification, taskId, quickFixes);
    }
    synchronized (myLock) {
      myCurrentNotifications.computeIfAbsent(taskId, key -> new ArrayList<>()).add(notification);
    }
  }

  private void showNotification(@NotNull NotificationData notification,
                                @NotNull Object taskId,
                                @NotNull List<? extends BuildIssueQuickFix> quickFixes) {
    String title = notification.getTitle();
    // Since the title of the notification data is the group, it is better to display the first line of the message
    String[] lines = notification.getMessage().split(System.lineSeparator());
    if (lines.length > 0) {
      title = lines[0];
    }

    AndroidSyncIssueEvent issueEvent;
    if (notification.getFilePath() != null) {
      issueEvent = new AndroidSyncIssueFileEvent(taskId, notification, title, quickFixes);
    }
    else {
      issueEvent = new AndroidSyncIssueEvent(taskId, notification, title, quickFixes);
    }

    // Only include errors in the summary text output
    // This has the side effect of not opening the right hand bar if there are no failures
    if (issueEvent.getKind() == MessageEvent.Kind.ERROR) {
      synchronized (myLock) {
        myShownFailures.computeIfAbsent(taskId, key -> new ArrayList<>())
          .addAll(((AndroidSyncIssueEventResult)issueEvent.getResult()).getFailures());
      }
    }
    else {
      // Only emit the event if it's not an error. Errors are saved in myShownFailures and will be emitted at the end of sync as part of FinishBuildEvent.
      // FinishBuildEvent is better to emit errors since the failures in FinishBuildEvent has better format of simplified node titles and clickable hyperlinks.
      myProject.getService(SyncViewManager.class).onEvent(taskId, issueEvent);
    }
  }

  @NotNull
  protected abstract ProjectSystemId getProjectSystemId();

  @NotNull
  protected Project getProject() {
    return myProject;
  }

  @Override
  public void dispose() {
    myProject = null;
  }
}
