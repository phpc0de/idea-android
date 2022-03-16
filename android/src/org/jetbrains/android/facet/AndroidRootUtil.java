/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package org.jetbrains.android.facet;

import com.android.SdkConstants;
import com.android.tools.idea.AndroidPsiUtils;
import com.android.tools.idea.projectsystem.AndroidProjectRootUtil;
import com.android.tools.idea.sdk.AndroidSdks;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.OrderedSet;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.jetbrains.annotations.SystemIndependent;

import static com.android.tools.idea.util.PropertiesFiles.getProperties;
import static com.intellij.openapi.util.io.FileUtil.getRelativePath;
import static com.intellij.openapi.util.io.FileUtil.*;
import static com.intellij.openapi.vfs.VfsUtilCore.isAncestor;
import static com.intellij.openapi.vfs.VfsUtilCore.*;
import static org.jetbrains.android.util.AndroidBuildCommonUtils.ANNOTATIONS_JAR_RELATIVE_PATH;
import static org.jetbrains.android.util.AndroidBuildCommonUtils.CLASSES_JAR_FILE_NAME;

public class AndroidRootUtil {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.facet.AndroidRootUtil");
  @NonNls public static final String DEFAULT_PROPERTIES_FILE_NAME = "default.properties";

  private AndroidRootUtil() {
  }

  /**
   * Returns the main manifest file of the module. Note that a module can have multiple
   * manifests so only use this if you really know you need to only look at the main manifests.
   * To look at all manifests, use  {@link IdeaSourceProvider#getManifestFiles(AndroidFacet)}.
   */
  @Nullable
  public static VirtualFile getPrimaryManifestFile(@NotNull AndroidFacet facet) {
    if (facet.isDisposed()) return null;
    return SourceProviderManager.getInstance(facet).getMainManifestFile();
  }

  @Nullable
  public static VirtualFile getCustomManifestFileForCompiler(@NotNull AndroidFacet facet) {
    return getFileByRelativeModulePath(facet.getModule(), facet.getProperties().CUSTOM_COMPILER_MANIFEST, false);
  }

  // DO NOT get PSI or DOM from this file, because it may be excluded (f.ex. it can be in /target/ directory)
  @Nullable
  public static VirtualFile getManifestFileForCompiler(@NotNull AndroidFacet facet) {
    return facet.getProperties().USE_CUSTOM_COMPILER_MANIFEST ? getCustomManifestFileForCompiler(facet) : getPrimaryManifestFile(facet);
  }

  /**
   * @deprecated You must use {@link ResourceFolderManager#getFolders()} instead
   */
  @Deprecated
  @Nullable
  public static VirtualFile getResourceDir(@NotNull AndroidFacet facet) {
    return ResourceFolderManager.getInstance(facet).getPrimaryFolder();
  }

  @Nullable
  private static String suggestResourceDirPath(@NotNull AndroidFacet facet) {
    Module module = facet.getModule();

    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    if (contentRoots.length == 0) {
      return null;
    }

    VirtualFile root = contentRoots[0];

    if (contentRoots.length > 1) {
      String moduleFileParentDirPath = toSystemIndependentName(new File(module.getModuleFilePath()).getParent());
      VirtualFile moduleFileParentDir = LocalFileSystem.getInstance().findFileByPath(moduleFileParentDirPath);
      if (moduleFileParentDir != null) {
        for (VirtualFile contentRoot : contentRoots) {
          if (Objects.equals(contentRoot, moduleFileParentDir)) {
            root = contentRoot;
          }
        }
      }
    }
    return root.getPath() + facet.getProperties().RES_FOLDER_RELATIVE_PATH;
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static String getResourceDirPath(@NotNull AndroidFacet facet) {
    VirtualFile resourceDir = getResourceDir(facet);
    return resourceDir != null ? resourceDir.getPath() : suggestResourceDirPath(facet);
  }

  @Nullable
  public static VirtualFile getFileByRelativeModulePath(Module module, String relativePath, boolean lookInContentRoot) {
    return AndroidProjectRootUtil.getFileByRelativeModulePath(module, relativePath, lookInContentRoot);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getAssetsDir(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getAssetsDir(facet);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getLibsDir(@NotNull AndroidFacet facet) {
    return getFileByRelativeModulePath(facet.getModule(), facet.getProperties().LIBS_FOLDER_RELATIVE_PATH, false);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getAidlGenDir(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getAidlGenDir(facet);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getAaptGenDir(@NotNull AndroidFacet facet) {
    String genPath = getAptGenSourceRootPath(facet);
    return genPath != null ? LocalFileSystem.getInstance().findFileByPath(genPath) : null;
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getRenderscriptGenDir(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getRenderscriptGenDir(facet);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static VirtualFile getBuildconfigGenDir(@NotNull AndroidFacet facet) {
    String path = getBuildconfigGenSourceRootPath(facet);
    return path != null ? LocalFileSystem.getInstance().findFileByPath(path) : null;
  }

  private static void collectClassFilesAndJars(@NotNull VirtualFile root,
                                               @NotNull Set<VirtualFile> result,
                                               @NotNull Set<VirtualFile> visited) {
    if (!visited.add(root)) {
      return;
    }
    for (VirtualFile child : root.getChildren()) {
      if (child.exists()) {
        if (child.isDirectory()) {
          collectClassFilesAndJars(child, result, visited);
        }
        else if ("jar".equals(child.getExtension()) || "class".equals(child.getExtension())) {
          if (child.getFileSystem() instanceof JarFileSystem) {
            VirtualFile localFile = JarFileSystem.getInstance().getVirtualFileForJar(child);
            if (localFile != null) {
              result.add(localFile);
            }
          }
          else {
            result.add(child);
          }
        }
      }
    }
  }

  private static void fillExternalLibrariesAndModules(@NotNull final Module module,
                                                      @NotNull final Set<VirtualFile> outputDirs,
                                                      @NotNull final Set<Module> visited,
                                                      @Nullable final Set<VirtualFile> libraries,
                                                      final boolean exportedLibrariesOnly,
                                                      final boolean recursive) {
    if (!visited.add(module)) {
      return;
    }
    ApplicationManager.getApplication().runReadAction(() -> {
      ModuleRootManager manager = ModuleRootManager.getInstance(module);
      for (OrderEntry entry : manager.getOrderEntries()) {
        if (!(entry instanceof ExportableOrderEntry) || ((ExportableOrderEntry)entry).getScope() != DependencyScope.COMPILE) {
          continue;
        }
        if (libraries != null && entry instanceof LibraryOrderEntry) {
          LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)entry;
          Library library = libraryOrderEntry.getLibrary();
          if (library != null && (!exportedLibrariesOnly || libraryOrderEntry.isExported())) {
            for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
              if (!file.exists()) {
                continue;
              }

              if (file.getFileType() instanceof ArchiveFileType) {
                if (file.getFileSystem() instanceof JarFileSystem) {
                  VirtualFile localFile = JarFileSystem.getInstance().getVirtualFileForJar(file);
                  if (localFile != null) {
                    libraries.add(localFile);
                  }
                }
                else {
                  libraries.add(file);
                }
              }
              else if (file.isDirectory() && !(file.getFileSystem() instanceof JarFileSystem)) {
                collectClassFilesAndJars(file, libraries, new HashSet<>());
              }
            }
          }
        }
        else if (entry instanceof ModuleOrderEntry) {
          Module depModule = ((ModuleOrderEntry)entry).getModule();
          if (depModule == null) {
            continue;
          }
          AndroidFacet facet = AndroidFacet.getInstance(depModule);
          boolean libraryProject = facet != null && facet.getConfiguration().isLibraryProject();

          CompilerModuleExtension extension = CompilerModuleExtension.getInstance(depModule);
          if (extension != null) {
            VirtualFile classDir = extension.getCompilerOutputPath();

            if (libraryProject) {
              VirtualFile tmpArtifactsDir = getOutputDirectoryForDex(depModule);

              if (tmpArtifactsDir != null) {
                VirtualFile packedClassesJar = tmpArtifactsDir.findChild(CLASSES_JAR_FILE_NAME);
                if (packedClassesJar != null) {
                  outputDirs.add(packedClassesJar);
                }
              }
            }
            // do not support android-app->android-app compile dependencies
            else if (facet == null && !outputDirs.contains(classDir) && classDir != null && classDir.exists()) {
              outputDirs.add(classDir);
            }
          }
          if (recursive) {
            fillExternalLibrariesAndModules(depModule, outputDirs, visited, libraries, !libraryProject || exportedLibrariesOnly, true);
          }
        }
      }
    });
  }

  private static VirtualFile getOutputDirectoryForDex(@NotNull Module module) {
    return CompilerModuleExtension.getInstance(module).getCompilerOutputPath();
  }

  @NotNull
  public static List<VirtualFile> getExternalLibraries(Module module) {
    Set<VirtualFile> files = new HashSet<>();
    OrderedSet<VirtualFile> libs = new OrderedSet<>();
    fillExternalLibrariesAndModules(module, files, new HashSet<>(), libs, false, true);

    addAnnotationsJar(module, libs);
    return libs;
  }

  private static void addAnnotationsJar(@NotNull Module module, @NotNull OrderedSet<VirtualFile> libs) {
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    if (sdk == null || !AndroidSdks.getInstance().isAndroidSdk(sdk)) {
      return;
    }
    String sdkHomePath = sdk.getHomePath();
    if (sdkHomePath == null) {
      return;
    }
    AndroidPlatform platform = AndroidPlatform.getInstance(module);

    if (platform != null && platform.needToAddAnnotationsJarToClasspath()) {
      String annotationsJarPath = toSystemIndependentName(sdkHomePath) + ANNOTATIONS_JAR_RELATIVE_PATH;
      VirtualFile annotationsJar = LocalFileSystem.getInstance().findFileByPath(annotationsJarPath);

      if (annotationsJar != null) {
        libs.add(annotationsJar);
      }
    }
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @NotNull
  public static Set<VirtualFile> getDependentModules(@NotNull Module module, @NotNull VirtualFile moduleOutputDir) {
    Set<VirtualFile> files = new HashSet<>();
    fillExternalLibrariesAndModules(module, files, new HashSet<>(), null, false, true);
    files.remove(moduleOutputDir);
    return files;
  }

  @NotNull
  public static VirtualFile[] getResourceOverlayDirs(@NotNull AndroidFacet facet) {
    List<String> overlayFolders = facet.getProperties().RES_OVERLAY_FOLDERS;
    List<VirtualFile> result = new ArrayList<>();
    for (String overlayFolder : overlayFolders) {
      VirtualFile overlayDir = getFileByRelativeModulePath(facet.getModule(), overlayFolder, true);
      if (overlayDir != null) {
        result.add(overlayDir);
      }
    }
    return toVirtualFileArray(result);
  }

  @Nullable
  @SystemIndependent
  public static String getModuleDirPath(@NotNull Module module) {
    return AndroidProjectRootUtil.getModuleDirPath(module);
  }

  @Nullable
  public static File findModuleRootFolderPath(@NotNull Module module) {
    @SystemIndependent String path = getModuleDirPath(module);
    if (path == null) return null;
    return new File(PathUtil.toSystemDependentName(path));
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static String getRenderscriptGenSourceRootPath(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getRenderscriptGenSourceRootPath(facet);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static String getBuildconfigGenSourceRootPath(@NotNull AndroidFacet facet) {
    // todo: return correct path for mavenized module when it'll be supported
    return getAptGenSourceRootPath(facet);
  }

  @Nullable
  public static VirtualFile getMainContentRoot(@NotNull AndroidFacet facet) {
    Module module = facet.getModule();

    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    if (contentRoots.length == 0) {
      return null;
    }

    if (contentRoots.length == 1) {
      return contentRoots[0];
    }
    VirtualFile manifestFile = getPrimaryManifestFile(facet);
    if (manifestFile != null) {
      for (VirtualFile root : contentRoots) {
        if (isAncestor(root, manifestFile, true)) {
          return root;
        }
      }
    }
    return contentRoots[0];
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static Pair<PropertiesFile, VirtualFile> findPropertyFile(@NotNull Module module, @NotNull String propertyFileName) {
    for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
      VirtualFile vFile = contentRoot.findChild(propertyFileName);
      if (vFile != null) {
        PsiFile psiFile = AndroidPsiUtils.getPsiFileSafely(module.getProject(), vFile);
        if (psiFile instanceof PropertiesFile) {
          return Pair.create((PropertiesFile)psiFile, vFile);
        }
      }
    }
    return null;
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  @Nullable
  public static Pair<Properties, VirtualFile> readPropertyFile(@NotNull Module module, @NotNull String propertyFileName) {
    for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
      Pair<Properties, VirtualFile> result = readPropertyFile(contentRoot, propertyFileName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static Pair<Properties, VirtualFile> readProjectPropertyFile(@NotNull Module module) {
    Pair<Properties, VirtualFile> pair = readPropertyFile(module, SdkConstants.FN_PROJECT_PROPERTIES);
    return pair != null ? pair : readPropertyFile(module, DEFAULT_PROPERTIES_FILE_NAME);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  @Nullable
  private static Pair<Properties, VirtualFile> readPropertyFile(@NotNull VirtualFile contentRoot, @NotNull String propertyFileName) {
    VirtualFile vFile = contentRoot.findChild(propertyFileName);
    if (vFile != null) {
      try {
        File file = virtualToIoFile(vFile);
        Properties properties = getProperties(file);
        return Pair.create(properties, vFile);
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }
    return null;
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  public static Pair<Properties, VirtualFile> readProjectPropertyFile(@NotNull VirtualFile contentRoot) {
    Pair<Properties, VirtualFile> pair = readPropertyFile(contentRoot, SdkConstants.FN_PROJECT_PROPERTIES);
    return pair != null ? pair : readPropertyFile(contentRoot, DEFAULT_PROPERTIES_FILE_NAME);
  }

  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  @Nullable
  public static Pair<String, VirtualFile> getPropertyValue(@NotNull Module module,
                                                           @NotNull String propertyFileName,
                                                           @NotNull String propertyKey) {
    Pair<Properties, VirtualFile> pair = readPropertyFile(module, propertyFileName);
    if (pair != null) {
      String value = pair.first.getProperty(propertyKey);
      if (value != null) {
        return Pair.create(value, pair.second);
      }
    }
    return null;
  }

  @Nullable
  public static Pair<String, VirtualFile> getProjectPropertyValue(@NotNull Module module, @NotNull String propertyName) {
    Pair<String, VirtualFile> result = getPropertyValue(module, SdkConstants.FN_PROJECT_PROPERTIES, propertyName);
    return result != null ? result : getPropertyValue(module, DEFAULT_PROPERTIES_FILE_NAME, propertyName);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  @SystemIndependent
  public static String getAptGenSourceRootPath(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getAptGenSourceRootPath(facet);
  }

  /**
   * @deprecated Do not use. JPS project specific.
   */
  @Deprecated
  @Nullable
  @SystemIndependent
  public static String getAidlGenSourceRootPath(@NotNull AndroidFacet facet) {
    return AndroidProjectRootUtil.getAidlGenSourceRootPath(facet);
  }

  @Nullable
  @SystemIndependent
  public static String getPathRelativeToModuleDir(@NotNull Module module, @NotNull String path) {
    @SystemIndependent String moduleDirPath = getModuleDirPath(module);
    if (moduleDirPath == null) {
      return null;
    }
    if (moduleDirPath.equals(path)) {
      return "";
    }
    return getRelativePath(moduleDirPath, path, '/');
  }
}
