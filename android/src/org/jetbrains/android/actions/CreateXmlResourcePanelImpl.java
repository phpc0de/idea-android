/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.actions;

import com.android.ide.common.resources.ValueXmlHelper;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.adtui.font.FontUtil;
import com.android.tools.idea.res.IdeResourcesUtil;
import com.android.tools.idea.res.IdeResourceNameValidator;
import com.android.tools.idea.ui.TextFieldWithBooleanBoxKt;
import com.android.tools.idea.ui.TextFieldWithColorPickerKt;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Embeddable UI for selecting how to create a new resource value (which XML file and directories to place it).
 */
public class CreateXmlResourcePanelImpl implements CreateXmlResourcePanel,
                                                   CreateXmlResourceSubdirPanel.Parent {
  private JPanel myPanel;
  private JTextField myNameField;
  private ModulesComboBox myModuleCombo;
  private JBLabel myModuleLabel;

  /**
   * The panel for the 'value' TextField Component. This container is abstracted since we may wrap {@link #myValueField} in a custom
   * Component for some resource values. E.g: We use a TextField with a ColorPicker Icon when creating a {@link ResourceType#COLOR} value.
   */
  private JPanel myValueFieldContainer;
  private JBLabel myValueLabel;
  private JBLabel myNameLabel;
  private JComboBox myFileNameCombo;
  private JBLabel mySourceSetLabel;
  private JComboBox mySourceSetCombo;
  private JBLabel myFileNameLabel;

  private final @Nullable Module myModule;
  private final @NotNull ResourceType myResourceType;
  private final @NotNull ResourceFolderType myFolderType;

  private JPanel myDirectoriesPanel;
  private JBLabel myDirectoriesLabel;

  private JTextField myValueField;

  private CreateXmlResourceSubdirPanel mySubdirPanel;

  private IdeResourceNameValidator myResourceNameValidator;

  public CreateXmlResourcePanelImpl(@NotNull Module module,
                                    @NotNull ResourceType resourceType,
                                    @NotNull ResourceFolderType folderType,
                                    @Nullable String resourceName,
                                    @Nullable String resourceValue,
                                    boolean chooseName,
                                    boolean chooseValue,
                                    boolean chooseFilename,
                                    @Nullable VirtualFile defaultFile,
                                    @Nullable VirtualFile contextFile,
                                    @NotNull final Function<Module, IdeResourceNameValidator> nameValidatorFactory) {
    setChangeNameVisible(false);
    setChangeValueVisible(false);
    setChangeFileNameVisible(chooseFilename);
    myFolderType = folderType;
    if (chooseName) {
      setChangeNameVisible(true);
      resourceName = IdeResourcesUtil.prependResourcePrefix(module, resourceName, folderType);
    }
    if (!StringUtil.isEmpty(resourceName)) {
      myNameField.setText(resourceName);
    }

    if (resourceType == ResourceType.COLOR) {
      // For Color values, we want a TextField with a ColorPicker so we wrap the TextField with a custom component.
      Color defaultColor = IdeResourcesUtil.parseColor(resourceValue);
      myValueFieldContainer.removeAll();
      myValueFieldContainer.add(TextFieldWithColorPickerKt.wrapWithColorPickerIcon(myValueField, defaultColor));
    }
    else if (resourceType == ResourceType.BOOL) {
      myValueFieldContainer.removeAll();
      myValueFieldContainer.add(TextFieldWithBooleanBoxKt.wrapWithBooleanCheckBox(myValueField, Boolean.parseBoolean(resourceValue)));
    }

    if (chooseValue) {
      myValueField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent event) {
          myValueField.setFont(FontUtil.getFontAbleToDisplay(myValueField.getText(), myValueField.getFont()));
        }
      });

      setChangeValueVisible(true);
      if (!StringUtil.isEmpty(resourceValue)) {
        // Need to escape the string to properly represent it in the JTextField.
        // E.g: If the string is "foo \n bar" we need to pass "foo \\n bar" to properly see it in the JTextField.
        String value = (resourceType == ResourceType.STRING) ? ValueXmlHelper.escapeResourceString(resourceValue, false) : resourceValue;
        myValueField.setText(value);
      }
    }

    myResourceType = resourceType;
    final Set<Module> modulesSet = new HashSet<>();
    modulesSet.add(module);

    for (AndroidFacet depFacet : AndroidUtils.getAllAndroidDependencies(module, true)) {
      modulesSet.add(depFacet.getModule());
    }

    assert !modulesSet.isEmpty();
    myModuleCombo.setModules(modulesSet);
    myModuleCombo.setSelectedModule(module);
    if (modulesSet.size() == 1) {
      // Don't show the module ComboBox when there can only be one option.
      myModule = module;
      setChangeModuleVisible(false);
    }
    else {
      // The module will have to be obtained form the ComboBox.
      myModule = null;
    }
    myModuleCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        CreateResourceDialogUtils.updateSourceSetCombo(mySourceSetLabel, mySourceSetCombo, AndroidFacet.getInstance(getModule()), null);
      }
    });

    ApplicationManager.getApplication().assertReadAccessAllowed();
    CreateResourceDialogUtils.updateSourceSetCombo(mySourceSetLabel, mySourceSetCombo, AndroidFacet.getInstance(getModule()), null);

    if (defaultFile == null) {
      final String defaultFileName = IdeResourcesUtil.getDefaultResourceFileName(myResourceType);

      if (defaultFileName != null) {
        myFileNameCombo.getEditor().setItem(defaultFileName);
      }
    }

    myDirectoriesLabel.setLabelFor(myDirectoriesPanel);
    mySubdirPanel = new CreateXmlResourceSubdirPanel(module.getProject(), folderType, myDirectoriesPanel, this);

    myResourceNameValidator = nameValidatorFactory.apply(getModule());

    myModuleCombo.addActionListener(e -> {
      mySubdirPanel.updateDirectories(true, getResourceDirectory());
      myResourceNameValidator = nameValidatorFactory.apply(getModule());
    });

    if (defaultFile != null) {
      resetFromFile(defaultFile, module.getProject());
    }
  }

  private void createUIComponents() {
    myValueField = new JTextField();
    // this panel just holds the value field component within the swing form, so we strip any UI from it and use a very simple LayoutManager
    myValueFieldContainer = new JPanel();
    myValueFieldContainer.setFocusable(false);
    myValueFieldContainer.setLayout(new BoxLayout(myValueFieldContainer, BoxLayout.Y_AXIS));
    myValueFieldContainer.setUI(null);
    myValueFieldContainer.setBorder(JBUI.Borders.empty());
    myValueFieldContainer.setOpaque(false);
    myValueFieldContainer.add(myValueField);
  }

  @Override
  public void resetToDefault() {
    if (myModule == null) {
      myModuleCombo.setSelectedModule(getRootModule());
    }
    String defaultFileName = IdeResourcesUtil.getDefaultResourceFileName(myResourceType);
    if (defaultFileName != null) {
      myFileNameCombo.getEditor().setItem(defaultFileName);
    }
    mySubdirPanel.resetToDefault();
  }

  /**
   * Finds the root modules of all the modules in the myModuleCombo.
   */
  @NotNull
  private Module getRootModule() {
    assert myModule == null; // this method should ONLY be called if myModule == null, otherwise myModule IS the root.
    ComboBoxModel model = myModuleCombo.getModel();
    Module root = null;
    int moduleDependencyCount = -1;
    // we go through all the modules, and find the one with the most dependencies.
    for (int c = 0; c < model.getSize(); c++) {
      Module otherModule = (Module)model.getElementAt(c);
      // getAllAndroidDependencies returns all transitive dependencies
      int otherModuleDependencyCount = AndroidUtils.getAllAndroidDependencies(otherModule, true).size();
      if (otherModuleDependencyCount > moduleDependencyCount) {
        moduleDependencyCount = otherModuleDependencyCount;
        root = otherModule;
      }
    }
    assert root != null;
    return root;
  }

  @Override
  public void resetFromFile(@NotNull VirtualFile file, @NotNull Project project) {
    final Module moduleForFile = ModuleUtilCore.findModuleForFile(file, project);
    if (moduleForFile == null) {
      return;
    }

    final VirtualFile parent = file.getParent();
    if (parent == null) {
      return;
    }

    if (myModule == null) {
      final Module prev = myModuleCombo.getSelectedModule();
      myModuleCombo.setSelectedItem(moduleForFile);

      if (!moduleForFile.equals(myModuleCombo.getSelectedItem())) {
        myModuleCombo.setSelectedModule(prev);
        return;
      }
    }
    else if (!myModule.equals(moduleForFile)) {
      return;
    }

    mySubdirPanel.resetFromFile(parent);
    myFileNameCombo.getEditor().setItem(file.getName());
    myPanel.repaint();
  }

  /**
   * @see CreateXmlResourceDialog#doValidate()
   */
  @Override
  public ValidationInfo doValidate() {
    String resourceName = getResourceName();
    String resourceValue = getValue();
    Module selectedModule = getModule();
    VirtualFile resourceDir = getResourceDirectory();
    List<String> directoryNames = getDirNames();
    String fileName = getFileName();

    if (myNameField.isVisible() && resourceName.isEmpty()) {
      return new ValidationInfo("specify resource name", myNameField);
    }
    else if (myNameField.isVisible() && !IdeResourcesUtil.isCorrectAndroidResourceName(resourceName)) {
      return new ValidationInfo(resourceName + " is not correct resource name", myNameField);
    }
    else if (fileName.isEmpty()) {
      return new ValidationInfo("specify file name", myFileNameCombo);
    }
    else if (selectedModule == null) {
      return new ValidationInfo("specify module", myModuleCombo);
    }
    else if (resourceDir == null) {
      return new ValidationInfo("specify a module with resources", myModuleCombo);
    }
    else if (directoryNames.isEmpty()) {
      return new ValidationInfo("choose directories", myDirectoriesPanel);
    }
    else if (resourceName.equals(IdeResourcesUtil.prependResourcePrefix(myModule, null, myFolderType))) {
      return new ValidationInfo("specify more than resource prefix", myNameField);
    }

    return CreateXmlResourceDialog.checkIfResourceAlreadyExists(selectedModule.getProject(), resourceDir, resourceName,
                                                                resourceValue, myResourceType, directoryNames, fileName);
  }

  @Override
  @NotNull
  public IdeResourceNameValidator getResourceNameValidator() {
    return myResourceNameValidator;
  }

  /**
   * @see CreateXmlResourceDialog#getPreferredFocusedComponent()
   */
  @Override
  public JComponent getPreferredFocusedComponent() {
    String name = myNameField.getText();
    if (name.isEmpty() ||
        // If the value is already populated, the user probably don't want to change it
        // (e.g extracting a string resources), so we focus the name field
        myValueField.isVisible() && !myValueField.getText().isEmpty()
        || name.equals(IdeResourcesUtil.prependResourcePrefix(myModule, null, myFolderType))) {
      return myNameField;
    }
    else if (myValueField.isVisible()) {
      return myValueField;
    }
    else if (myModuleCombo.isVisible()) {
      return myModuleCombo;
    }
    else {
      return myFileNameCombo;
    }
  }

  @Override
  @NotNull
  public String getResourceName() {
    return myNameField.getText().trim();
  }

  @Override
  @NotNull
  public ResourceType getType() {
    return myResourceType;
  }

  @Override
  @NotNull
  public List<String> getDirNames() {
    return mySubdirPanel.getDirNames();
  }

  @Override
  @NotNull
  public String getFileName() {
    return ((String)myFileNameCombo.getEditor().getItem()).trim();
  }

  @Override
  @NotNull
  public String getValue() {
    String value = myValueField.getText();
    // When we need to get the desired value for a string resource, the text has to be unescaped.
    // E.g: If the user types "foo \n bar" JTextField.getText will return "foo \\n bar" so it has to be unescaped.
    return (myResourceType == ResourceType.STRING) ? ValueXmlHelper.unescapeResourceString(value, false, false) : value;
  }

  @Override
  @Nullable
  public Module getModule() {
    return myModule != null ? myModule : myModuleCombo.getSelectedModule();
  }

  @Override
  @Nullable
  public VirtualFile getResourceDirectory() {
    Module module = getModule();
    if (module == null) {
      return null;
    }
    PsiDirectory resDirectory = CreateResourceDialogUtils.getOrCreateResourceDirectory(mySourceSetCombo, module);
    return resDirectory != null ? resDirectory.getVirtualFile() : null;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  private void setChangeFileNameVisible(boolean isVisible) {
    myFileNameLabel.setVisible(isVisible);
    myFileNameCombo.setVisible(isVisible);
  }

  private void setChangeValueVisible(boolean isVisible) {
    myValueFieldContainer.setVisible(isVisible);
    myValueLabel.setVisible(isVisible);
  }

  private void setChangeNameVisible(boolean isVisible) {
    myNameField.setVisible(isVisible);
    myNameLabel.setVisible(isVisible);
  }

  private void setChangeModuleVisible(boolean isVisible) {
    myModuleLabel.setVisible(isVisible);
    myModuleCombo.setVisible(isVisible);
  }

  // Only public for CreateXmlResourceSubdirPanel.Parent
  @Override
  public void updateFilesCombo(List<VirtualFile> directories) {
    final Object oldItem = myFileNameCombo.getEditor().getItem();
    final Set<String> fileNameSet = new HashSet<>();

    for (VirtualFile dir : directories) {
      for (VirtualFile file : dir.getChildren()) {
        fileNameSet.add(file.getName());
      }
    }
    final List<String> fileNames = new ArrayList<>(fileNameSet);
    Collections.sort(fileNames);
    myFileNameCombo.setModel(new DefaultComboBoxModel(fileNames.toArray()));
    myFileNameCombo.getEditor().setItem(oldItem);
  }
}
