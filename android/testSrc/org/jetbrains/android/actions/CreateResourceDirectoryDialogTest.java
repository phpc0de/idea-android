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
package org.jetbrains.android.actions;

import com.google.common.collect.Iterables;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.android.actions.CreateResourceDirectoryDialogBase.ValidatorFactory;
import org.jetbrains.android.facet.ResourceFolderManager;
import org.mockito.Mockito;

public final class CreateResourceDirectoryDialogTest extends AndroidTestCase {
  private PsiDirectory myResDirectory;
  private CreateResourceDirectoryDialog myDialog;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    VirtualFile resVirtualFile = Iterables.getOnlyElement(ResourceFolderManager.getInstance(myFacet).getFolders());
    myResDirectory = PsiManager.getInstance(myModule.getProject()).findDirectory(resVirtualFile);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (myDialog != null) {
        ApplicationManager.getApplication().invokeAndWait(() -> Disposer.dispose(myDialog.getDisposable()));
      }
    }
    finally {
      super.tearDown();
    }
  }

  private void initDialog(boolean forceDirectoryDoesNotExist) {
    Project project = myModule.getProject();
    Application application = ApplicationManager.getApplication();
    ValidatorFactory factory = Mockito.mock(ValidatorFactory.class);

    application.invokeAndWait(() -> myDialog =
      new CreateResourceDirectoryDialog(project, myModule, null, myResDirectory, null, factory, forceDirectoryDoesNotExist));
  }

  public void testDoValidateWhenSubdirectoryDoesNotExist() {
    initDialog(false);

    myDialog.getDirectoryNameTextField().setText("layout");
    assertNull(myDialog.doValidate());
  }

  public void testDoValidateWhenSubdirectoryExists() {
    initDialog(false);

    Computable<PsiFileSystemItem> createLayoutSubdirectory = () -> myResDirectory.createSubdirectory("layout");
    PsiFileSystemItem subdirectory = ApplicationManager.getApplication().runWriteAction(createLayoutSubdirectory);

    myDialog.getDirectoryNameTextField().setText("layout");

    String expected = subdirectory.getVirtualFile().getPresentableUrl() + " already exists. Use a different qualifier.";
    assertEquals(expected, myDialog.doValidate().message);
  }

  public void testCanIgnoreSubdirectoryCreation() {
    initDialog(true);

    Computable<PsiFileSystemItem> createLayoutSubdirectory = () -> myResDirectory.createSubdirectory("layout");
    PsiFileSystemItem subdirectory = ApplicationManager.getApplication().runWriteAction(createLayoutSubdirectory);

    myDialog.getDirectoryNameTextField().setText("layout");
    assertNull(myDialog.doValidate());
  }
}
