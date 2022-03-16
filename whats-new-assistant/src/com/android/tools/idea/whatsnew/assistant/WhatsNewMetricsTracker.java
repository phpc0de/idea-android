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
package com.android.tools.idea.whatsnew.assistant;

import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.assistant.PanelFactory;
import com.google.common.base.Stopwatch;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.google.wireless.android.sdk.stats.WhatsNewAssistantUpdateEvent;
import com.intellij.openapi.project.Project;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class WhatsNewMetricsTracker {
  @NotNull private final Map<Project, MetricsEventBuilder> myProjectToBuilderMap;
  @NotNull private final Map<Project, ActionButtonMetricsEventBuilder> myProjectToActionBuilderMap;

  WhatsNewMetricsTracker() {
    myProjectToBuilderMap = new HashMap<>();
    myProjectToActionBuilderMap = new HashMap<>();
  }

  @NotNull
  public static WhatsNewMetricsTracker getInstance() {
    return Objects.requireNonNull(PanelFactory.EP_NAME.findExtension(WhatsNewUpdateStatusPanelFactory.class)).getMetricsTracker();
  }

  void open(@NotNull Project project, boolean isAutoOpened) {
    // An extra "open" can fire when the window is already open and the user manually uses the WhatsNewSidePanelAction
    // again, so in this case just ignore the call and treat the original open as the actual beginning
    myProjectToBuilderMap.computeIfAbsent(project, p -> {
      MetricsEventBuilder eventBuilder = new MetricsEventBuilder();
      eventBuilder.myBuilder.setAutoOpened(isAutoOpened);
      getActionsMetricsBuilder(project).generateEventsForAllCreatedBeforeActions(project).forEach(eventBuilder::addActionButtonEvent);
      return eventBuilder;
    });
  }

  private ActionButtonMetricsEventBuilder getActionsMetricsBuilder(@NotNull Project project) {
    return myProjectToActionBuilderMap.computeIfAbsent(project, p -> new ActionButtonMetricsEventBuilder());
  }

  void clearCachedActionKeys(@NotNull Project project) {
    myProjectToActionBuilderMap.remove(project);
  }

  void updateFlow(@NotNull Project project) {
    myProjectToBuilderMap.get(project).myBuilder.setUpdateFlow(true);
  }

  void scrolledToBottom(@NotNull Project project) {
    MetricsEventBuilder metricsEventBuilder = myProjectToBuilderMap.get(project);
    if (metricsEventBuilder != null) {
      metricsEventBuilder.scrolledToBottom();
    }
  }

  public void actionButtonCreated(@NotNull Project project, @NotNull String actionKey) {
    actionButtonEvent(project, getActionsMetricsBuilder(project).actionCreated(project, actionKey));
  }

  public void clickActionButton(@NotNull Project project, @NotNull String actionKey) {
    actionButtonEvent(project, getActionsMetricsBuilder(project).clickAction(project, actionKey));
  }

  public void stateUpdateActionButton(@NotNull Project project, @NotNull String actionKey) {
    actionButtonEvent(project, getActionsMetricsBuilder(project).stateUpdateAction(project, actionKey));
  }

  private void actionButtonEvent(@NotNull Project project, @NotNull WhatsNewAssistantUpdateEvent.ActionButtonEvent.Builder actionButtonEvent) {
    MetricsEventBuilder metricsEventBuilder = myProjectToBuilderMap.get(project);
    if (metricsEventBuilder != null) {
      metricsEventBuilder.addActionButtonEvent(actionButtonEvent);
    }
  }

  public void dismissed(@NotNull Project project) {
    myProjectToBuilderMap.get(project).myBuilder.setDismissed(true);
  }

  public void setUpdateTime(@NotNull Project project) {
    myProjectToBuilderMap.get(project).setUpdateTime();
  }

  void close(@NotNull Project project) {
    myProjectToBuilderMap.remove(project).buildAndLog();
  }

  /**
   * Wrapper for WhatsNewAssistantUpdateEvent because we need to keep track of the time difference
   */
  private static class MetricsEventBuilder {
    @NotNull final WhatsNewAssistantUpdateEvent.Builder myBuilder;
    @NotNull final Stopwatch myStopwatch;

    private MetricsEventBuilder() {
      myBuilder = WhatsNewAssistantUpdateEvent.newBuilder();
      myStopwatch = Stopwatch.createStarted();
    }

    private void setUpdateTime() {
      myBuilder.setTimeToUpdateMs(myStopwatch.elapsed().toMillis());
    }

    private void scrolledToBottom() {
      myBuilder.setScrolledToBottom(true);
      myBuilder.setTimeToScrolledToBottom(myStopwatch.elapsed().toMillis());
    }

    private void buildAndLog() {
      myBuilder.setTimeToCloseMs(myStopwatch.elapsed().toMillis());

      UsageTracker.log(
        AndroidStudioEvent.newBuilder()
          .setKind(AndroidStudioEvent.EventKind.WHATS_NEW_ASSISTANT_UPDATE_EVENT)
          .setWhatsNewAssistantUpdateEvent(myBuilder)
      );
    }

    public void addActionButtonEvent(WhatsNewAssistantUpdateEvent.ActionButtonEvent.Builder actionButtonEvent) {
      myBuilder.addActionButtonEvents(actionButtonEvent.setTimeFromWnaOpen(myStopwatch.elapsed().toMillis()));
    }
  }
}
