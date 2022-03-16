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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.intellij.lang.java.lexer.JavaLexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiNameHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreateNewClassDialogValidatorExImpl implements CreateNewClassDialogValidatorEx {
  public static final String INVALID_PACKAGE_MESSAGE = "This is not a valid Java package name";
  public static final String INVALID_QUALIFIED_NAME_FOR_NEW_NAME = "New class name is not a valid Java qualified name";
  public static final String INVALID_QUALIFIED_NAME_FOR_INTERFACE = "Interface is not a valid Java qualified name";
  public static final String INVALID_QUALIFIED_NAME_FOR_SUPERCLASS = "Superclass is not a valid Java qualified name";
  public static final String INVALID_QUALIFIED_NAME = "This is not a valid Java qualified name";
  public static final String NOT_A_VALID_INTERFACE = "Not a valid interface: ";
  public static final String NOT_A_VALID_CLASS = "Not a valid class: ";

  private Project myProject;

  public CreateNewClassDialogValidatorExImpl(Project project) {
    myProject = project;
  }

  public static boolean isValidJavaIdentifier(String identifier) {
    return StringUtil.isJavaIdentifier(identifier) && !JavaLexer.isKeyword(identifier, LanguageLevel.HIGHEST);
  }

  @Override
  public boolean checkSuperclass(@NotNull String inputString) {
    // TODO Check for self-inheritance or final inheritance.
    return checkQualifiedName(inputString);
  }

  @Override
  public boolean checkInterface(@NotNull String inputString) {
    // TODO Check for self-inheritance.
    return checkQualifiedName(inputString);
  }

  @Override
  public boolean checkPackage(@NotNull String inputString) {
    return CharMatcher.whitespace().matchesAllOf(inputString) || checkList(inputString, ".");
  }

  @Nullable
  @Override
  public String getErrorText(String inputString) {
    if (!inputString.isEmpty() && !PsiNameHelper.getInstance(myProject).isQualifiedName(inputString)) {
      return INVALID_QUALIFIED_NAME;
    }
    return null;
  }

  @NotNull
  @Override
  public String getNameErrorText(@Nullable String inputString) {
    return INVALID_QUALIFIED_NAME_FOR_NEW_NAME;
  }

  @NotNull
  @Override
  public String getSuperclassErrorText(@Nullable String inputString) {
    return INVALID_QUALIFIED_NAME_FOR_SUPERCLASS;
  }

  @NotNull
  @Override
  public String getInterfacesErrorText(@Nullable String inputString) {
    return INVALID_QUALIFIED_NAME_FOR_INTERFACE;
  }

  @NotNull
  @Override
  public String getNotAClassErrorText(@Nullable String inputString) {
    return NOT_A_VALID_CLASS + inputString;
  }

  @NotNull
  @Override
  public String getNotAnInterfaceErrorText(@Nullable String inputString) {
    return NOT_A_VALID_INTERFACE + inputString;
  }

  @NotNull
  @Override
  public String getPackageErrorText(@Nullable String inputString) {
    return INVALID_PACKAGE_MESSAGE;
  }

  @Override
  public boolean checkInput(String inputString) {
    String name = inputString.trim();
    return !CharMatcher.whitespace().matchesAllOf(name) && getErrorText(name) == null;
  }

  @Override
  public boolean canClose(String inputString) {
    return !CharMatcher.whitespace().matchesAllOf(inputString) && getErrorText(inputString) == null;
  }

  private static boolean checkQualifiedName(@NotNull String qualifiedName) {
    return CharMatcher.whitespace().matchesAllOf(qualifiedName) || checkList(qualifiedName, ".");
  }

  private static boolean checkList(@NotNull String inputString, @NotNull String delimiter) {
    for (String identifier : Splitter.on(delimiter).trimResults().split(inputString)) {
      if (!isValidJavaIdentifier(identifier)) {
        return false;
      }
    }

    return true;
  }
}
