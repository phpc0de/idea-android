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
package com.android.tools.idea.uibuilder.mockup.editor.creators;

import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LISTITEM;
import static com.android.SdkConstants.ATTR_SHOW_IN;
import static com.android.SdkConstants.CLASS_RECYCLER_VIEW_V7;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.idea.common.command.NlWriteCommandActionUtil;
import com.android.tools.idea.common.model.AttributesTransaction;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.NlModel;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.idea.common.surface.SceneView;
import com.android.tools.idea.configurations.ConfigurationManager;
import com.android.tools.idea.uibuilder.mockup.Mockup;
import com.android.tools.idea.uibuilder.model.NlComponentHelperKt;
import com.android.tools.idea.uibuilder.scene.LayoutlibSceneManager;
import com.android.tools.idea.uibuilder.scene.RenderListener;
import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.android.utils.SdkUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import java.awt.Rectangle;
import org.jetbrains.android.actions.CreateResourceFileAction;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

/**
 * Create a new layout and an include tag referencing this layout
 * in the {@link NlModel} provided in the constructor
 */
public class IncludeTagCreator extends SimpleViewCreator {

  private String myNewLayoutResource;

  /**
   * Create a new layout and an include tag referencing this layout
   * in the {@link NlModel} provided in the constructor
   *
   * @param mockup     the mockup to extract the information from
   * @param model      the model to insert the new component into
   * @param screenView The currentScreen view displayed in the {@link NlDesignSurface}.
   *                   Used to convert the size of component from the mockup to the Android coordinates.
   * @param selection  The selection made in the {@link com.android.tools.idea.uibuilder.mockup.editor.MockupEditor}
   */
  public IncludeTagCreator(@NotNull Mockup mockup,
                           @NotNull NlModel model,
                           @NotNull SceneView screenView, @NotNull Rectangle selection) {
    super(mockup, model, screenView, selection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void addAttributes(@NotNull AttributesTransaction transaction) {
    myNewLayoutResource = createNewIncludedLayout();
    super.addAttributes(transaction);
    if (myNewLayoutResource != null) {
      addIncludeAttribute(transaction, myNewLayoutResource);
    }
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public String getAndroidViewTag() {
    return VIEW_INCLUDE;
  }

  @Nullable
  @Override
  public NlComponent addToModel() {
    NlComponent component = getMockup().getComponent();
    if (NlComponentHelperKt.isOrHasSuperclass(component, CLASS_RECYCLER_VIEW_V7)) {
      addListItemAttribute(component);
      return component;
    }
    else {
      return super.addToModel();
    }
  }

  private void addListItemAttribute(NlComponent component) {
    String newLayoutResource = createNewIncludedLayout();
    NlWriteCommandActionUtil.run(component, "Add listitem attribute", () ->
      component.setAttribute(TOOLS_URI, ATTR_LISTITEM, LAYOUT_RESOURCE_PREFIX + newLayoutResource));
  }

  /**
   * Add the include="@layout/resourceName" attribute
   *
   * @param transaction
   * @param resourceName
   */
  private static void addIncludeAttribute(@NotNull AttributesTransaction transaction, @NotNull String resourceName) {
    transaction.setAttribute(null, ATTR_LAYOUT, LAYOUT_RESOURCE_PREFIX + resourceName);
  }

  /**
   * Create a new layout that will be included as a child of the mockup component
   */
  private String createNewIncludedLayout() {
    AndroidFacet facet = getMockup().getComponent().getModel().getFacet();
    XmlFile newFile = CreateResourceFileAction.createFileResource(
      facet, ResourceFolderType.LAYOUT, null, null, null, true, null, null, null, false);

    if (newFile == null) {
      return null;
    }
    XmlTag rootTag = newFile.getRootTag();
    if (rootTag == null) {
      return null;
    }
    DesignSurface surface = getScreenView().getSurface();
    LayoutlibSceneManager manager = (LayoutlibSceneManager)surface.getSceneManager();

    if (manager != null) {
      VirtualFile virtualFile = newFile.getVirtualFile();
      NlModel model = NlModel.builder(facet, virtualFile, ConfigurationManager.getOrCreateInstance(facet).getConfiguration(virtualFile))
        .withParentDisposable(newFile.getProject())
        .withComponentRegistrar(surface.getComponentRegistrar())
        .build();
      manager.addRenderListener(new RenderListener() {
        @Override
        public void onRenderCompleted() {
          manager.removeRenderListener(this);
          if (model.getComponents().isEmpty()) {
            return;
          }
          NlComponent component = model.getComponents().get(0);
          final AttributesTransaction transaction = component.startAttributeTransaction();
          addShowInAttribute(transaction);
          addSizeAttributes(transaction, getAndroidBounds());
          addMockupAttributes(transaction, getSelectionBounds());
          NlWriteCommandActionUtil.run(component, "", transaction::commit);
        }
      });
    }

    return SdkUtils.fileNameToResourceName(newFile.getName());
  }

  /**
   * Add the attribute {@value SdkConstants#ATTR_SHOW_IN} to transaction
   *
   * @param transaction the transaction where the attributes will be added
   */
  private void addShowInAttribute(@NotNull AttributesTransaction transaction) {
    final String showInName = SdkUtils.fileNameToResourceName(getModel().getFile().getName());
    transaction.setAttribute(TOOLS_URI, ATTR_SHOW_IN, LAYOUT_RESOURCE_PREFIX + showInName);
  }
}
