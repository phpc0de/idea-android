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
package com.android.tools.idea.actions;

import com.android.tools.idea.projectsystem.SourceProviders;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateFromTemplateAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.java.JavaBundle;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.util.IncorrectOperationException;
import java.util.List;
import java.util.Map;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.SourceProviderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

public final class CreateClassAction extends AnAction {
  private final JavaDirectoryService myJavaDirectoryService;

  public CreateClassAction() {
    super("Java Class");
    myJavaDirectoryService = JavaDirectoryService.getInstance();
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    DataContext context = event.getDataContext();
    IdeView view = LangDataKeys.IDE_VIEW.getData(context);

    if (view == null) {
      return;
    }

    Module module = PlatformCoreDataKeys.MODULE.getData(context);

    if (module == null) {
      return;
    }

    Project project = CommonDataKeys.PROJECT.getData(context);

    if (project == null) {
      return;
    }

    PsiDirectory directory = getDestinationDirectory(view, module);

    if (directory == null) {
      return;
    }

    CreateFileFromTemplateDialog dialog = new CreateFileFromTemplateDialog(project, directory);

    try {
      PsiClass createdElement = dialog.show(new CreateFileFromTemplateDialog.FileCreator() {
        @Override
        public PsiClass createFile(@NotNull String name, @NotNull Map<String, String> creationOptions, @NotNull String templateName) {
          String enteredPackageName = creationOptions.get(FileTemplate.ATTRIBUTE_PACKAGE_NAME);
          PsiDirectory packageSubdirectory = ApplicationManager.getApplication()
            .runWriteAction((Computable<PsiDirectory>)() -> createPackageSubdirectory(directory, enteredPackageName));
          return checkOrCreate(name, packageSubdirectory, templateName, creationOptions);
        }

        @Override
        @NotNull
        public String getActionName(@NotNull String name, @NotNull String templateName) {
          PsiQualifiedNamedElement p = myJavaDirectoryService.getPackage(directory);
          assert p != null;

          String packageDirectoryQualifiedName = p.getQualifiedName();
          return JavaBundle.message("progress.creating.class", StringUtil.getQualifiedName(packageDirectoryQualifiedName, name));
        }
      });

      view.selectElement(createdElement);
      CreateFromTemplateAction.moveCaretAfterNameIdentifier(createdElement);
      if (dialog.isShowSelectOverridesDialogCheckBoxSelected()) {
        showOverridesDialog(event);
      }
    }
    catch (CreateFileFromTemplateDialog.FailedToCreateFileException exception) {
      // do nothing; this is triggered, for example, by the user entering invalid values for class names and such, handled elsewhere
    }
  }

  @Nullable
  @VisibleForTesting
  static PsiDirectory getDestinationDirectory(@NotNull IdeView ide, @NotNull Module module) {
    PsiDirectory[] directories = ide.getDirectories();

    if (directories.length == 1) {
      return directories[0];
    }

    AndroidFacet facet = AndroidFacet.getInstance(module);

    if (facet == null) {
      return ide.getOrChooseDirectory();
    }

    SourceProviders sourceProviderManager = SourceProviderManager.getInstance(facet);
    List<VirtualFile> files = ImmutableList.copyOf(Iterables.concat(sourceProviderManager.getSources().getJavaDirectories(),
                                                                    sourceProviderManager.getSources().getKotlinDirectories(),
                                                                    sourceProviderManager.getUnitTestSources().getJavaDirectories(),
                                                                    sourceProviderManager.getUnitTestSources().getKotlinDirectories(),
                                                                    sourceProviderManager.getAndroidTestSources().getJavaDirectories(),
                                                                    sourceProviderManager.getAndroidTestSources().getKotlinDirectories()));

    if (files.size() != 1) {
      return ide.getOrChooseDirectory();
    }

    VirtualFile file = files.iterator().next();

    if (file == null) {
      return ide.getOrChooseDirectory();
    }

    return PsiManager.getInstance(module.getProject()).findDirectory(file);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = isAvailable(e.getDataContext());
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(enabled);
  }

  private static boolean isAvailable(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (project == null || view == null || view.getDirectories().length == 0) {
      return false;
    }

    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    for (PsiDirectory dir : view.getDirectories()) {
      if (projectFileIndex.isUnderSourceRootOfType(dir.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES) && checkPackageExists(dir)) {
        return true;
      }
    }

    return false;
  }

  private static boolean checkPackageExists(PsiDirectory directory) {
    PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (psiPackage == null) {
      return false;
    }

    String name = psiPackage.getQualifiedName();
    return StringUtil.isEmpty(name) || PsiNameHelper.getInstance(directory.getProject()).isQualifiedName(name);
  }

  /**
   * Using the given directory, steps up the directory tree (if needed) to find a common ancestor with the desired package name.
   * Then it builds the subdirectories (if needed) to create a directory for that package name.
   * Example:
   * directory: PsiDirectory "/home/username/androidStudio/myProject/com/example/widget/ui/buttons"
   * packageName: com.example.widget.io.net
   * <ol>
   * <li>com.example.widget.ui.buttons != com.example.widget.io.net && com.example.widget.ui.buttons ⊄ com.example.widget.io.net
   * So remove ".buttons" and step up one directory</li>
   *
   * <li>com.example.widget.ui != com.example.widget.io.net && com.example.widget.ui ⊄ com.example.widget.io.net
   * So remove ".ui" and step up one directory</li>
   *
   * <li>com.example.widget != com.example.widget.io.net BUT com.example.widget ⊂ com.example.widget.io.net
   * So append ".io" and create and enter that directory</li>
   *
   * <li>com.example.widget.io != com.example.widget.io.net BUT com.example.widget.io ⊂ com.example.widget.io.net
   * So append ".net" and create and enter that directory</li>
   *
   * <li>com.example.widget.io.net == com.example.widget.io.net</li>
   * Complete
   * </ol>
   *
   * Why not just start with the com and move down the hierarchy? It requires a PsiDirectory object to build the directories in IJ and
   * on disk and then to pass back to the caller. This requires starting from the known PsiDirectory.
   *
   * @param directory   The directory to start in. Usually the directory of the currently open file or package the user clicked on.
   * @param packageName The name of the package to create a matching directory for.
   * @return A PsiDirectory representing the new directory matching the package.
   */
  @NotNull
  private static PsiDirectory createPackageSubdirectory(@NotNull PsiDirectory directory, @NotNull String packageName) {
    PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
    String startPackagePath = psiPackage != null ? psiPackage.getQualifiedName() : null;

    // If the start directory is the same as the desired one, no work required.
    if (startPackagePath == null || startPackagePath.equals(packageName)) {
      return directory;
    }


    PsiPackage baseName = JavaDirectoryService.getInstance().getPackage(directory);
    assert baseName != null;

    PsiDirectory dir = directory;

    if (!packageName.startsWith(baseName.getQualifiedName())) {
      // This means that baseName is not an ancestor of packageName (like in the example above).
      // We need to walk up the package tree from baseName until we get to a common ancestor.
      while (baseName.getParentPackage() != null) {
        if (packageName.equals(baseName.getQualifiedName())) {
          // We've stepped back in baseName and discovered packageName is an ancestor.
          // (E.g. baseName started as com.example.widget.io and packageName is com.example.widget).
          assert dir != null;
          return dir;
        }
        else if (packageName.startsWith(baseName.getQualifiedName())) {
          // We've traversed up the tree from baseName to the point where baseName is now an ancestor of packageName.
          // (E.g. baseName started as com.example.widget.io, but is now com.example.widget and packageName is com.example.widget.ui).
          break;
        }
        else {
          // We still haven't found the common ancestor, so go up one level in the tree.
          baseName = baseName.getParentPackage();
          assert baseName != null;

          assert dir != null;
          dir = dir.getParentDirectory();
        }
      }
    }

    // baseName is now an ancestor of packageName (or empty), so find the intervening nodes and make directories for them if needed.
    String newPackageName = baseName.getQualifiedName().isEmpty() ? packageName
                                                                  : packageName.substring(baseName.getQualifiedName().length() + 1);
    for (String component : Splitter.on('.').split(newPackageName)) {
      assert dir != null;
      PsiDirectory d = dir.findSubdirectory(component);

      dir = d == null ? dir.createSubdirectory(component) : d;
    }

    assert dir != null;
    return dir;
  }

  private static void showOverridesDialog(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    assert project != null;

    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      AnActionEvent newEvent =
        new AnActionEvent(event.getInputEvent(), EditorUtil.getEditorDataContext(editor),
                          ActionPlaces.UNKNOWN, event.getPresentation(), event.getActionManager(), 0);
      ActionManager.getInstance().getAction("OverrideMethods").actionPerformed(newEvent);
    }
  }

  @Nullable
  private PsiClass checkOrCreate(String newName, PsiDirectory directory, String templateName, Map<String, String> creationOptions)
    throws IncorrectOperationException {
    PsiDirectory dir = directory;
    newName = StringUtil.trimEnd(newName, ".java");

    if (newName.contains(".")) {
      List<String> names = Splitter.on(".").splitToList(newName);

      for (String name : names.subList(0, names.size() - 1)) {
        PsiDirectory subDir = dir.findSubdirectory(name);

        if (subDir == null) {
          subDir = dir.createSubdirectory(name);
        }

        dir = subDir;
      }

      newName = names.get(names.size() - 1);
    }

    return myJavaDirectoryService.createClass(dir, newName, templateName, true, creationOptions);
  }
}