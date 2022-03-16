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
package com.android.tools.idea.common.editor;

import com.android.tools.idea.common.SyncNlModel;
import com.android.tools.idea.common.error.IssueModel;
import com.android.tools.idea.common.error.IssuePanel;
import com.android.tools.idea.common.fixtures.ModelBuilder;
import com.android.tools.idea.uibuilder.LayoutTestCase;
import com.android.tools.idea.uibuilder.editor.NlActionManager;
import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.android.tools.idea.uibuilder.type.LayoutFileType;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ActionsToolbarTest extends LayoutTestCase {

  public void testPresentationFactoryCacheDoesNotGrowOnUpdate() throws Exception {
    // Regression test for b/79110899
    ActionsToolbar toolbar = createToolbar();
    ActionToolbarImpl centerToolBar = toolbar.getCenterToolbar();
    Map<?, ?> cache = getPresentationCache(centerToolBar);
    centerToolBar.updateActionsImmediately();
    int initialSize = cache.size();

    toolbar.updateActions();
    centerToolBar.updateActionsImmediately();
    assertThat(cache.size()).isAtMost(initialSize);

    toolbar.updateActions();
    centerToolBar.updateActionsImmediately();
    assertThat(cache.size()).isAtMost(initialSize);

    toolbar.updateActions();
    centerToolBar.updateActionsImmediately();
    assertThat(cache.size()).isAtMost(initialSize);

    toolbar.updateActions();
    centerToolBar.updateActionsImmediately();
    assertThat(cache.size()).isAtMost(initialSize);
  }

  private ActionsToolbar createToolbar() {
    SyncNlModel model = createModel().build();
    NlDesignSurface surface = (NlDesignSurface)model.getSurface();
    NlActionManager actionManager = new NlActionManager(surface);
    IssueModel issueModel = new IssueModel();
    IssuePanel issuePanel = new IssuePanel(surface, issueModel);
    Disposer.register(getTestRootDisposable(), issuePanel);
    doReturn(actionManager).when(surface).getActionManager();
    doReturn(LayoutFileType.INSTANCE).when(surface).getLayoutType();
    when(surface.getIssuePanel()).thenReturn(issuePanel);
    when(surface.getIssueModel()).thenReturn(issueModel);
    return new ActionsToolbar(getTestRootDisposable(), surface);
  }

  // Lookup some private fields via reflections.
  // The aim is to test that the action to presentation cache doesn't grow indefinitely.
  private static Map<?,?> getPresentationCache(@NotNull ActionToolbarImpl actionToolbar) throws Exception {
    Field factoryField = ActionToolbarImpl.class.getDeclaredField("myPresentationFactory");
    factoryField.setAccessible(true);
    PresentationFactory factory = (PresentationFactory)factoryField.get(actionToolbar);
    Field cacheField = PresentationFactory.class.getDeclaredField("myAction2Presentation");
    cacheField.setAccessible(true);
    return (Map<?,?>)cacheField.get(factory);
  }

  @NotNull
  private ModelBuilder createModel() {
    return model("linear.xml",
                 component(LINEAR_LAYOUT)
                   .withBounds(0, 0, 2000, 2000)
                   .matchParentWidth()
                   .matchParentHeight()
                   .children(
                     component(TEXT_VIEW)
                       .withBounds(200, 200, 200, 200)
                       .id("@id/myText")
                       .width("100dp")
                       .height("100dp")
                       .withAttribute("android:layout_x", "100dp")
                       .withAttribute("android:layout_y", "100dp")
                   ));
  }
}
