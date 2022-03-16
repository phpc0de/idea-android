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

// ATTENTION: This file has been automatically generated from Aidl.bnf. Do not edit it manually.

package com.android.tools.idea.lang.aidl.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.android.tools.idea.lang.aidl.lexer.AidlTokenTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class AidlParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return document(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(CLASS_OR_INTERFACE_TYPE, PRIMITIVE_TYPE, TYPE),
  };

  /* ********************************************************** */
  // declaration*
  public static boolean body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "body")) return false;
    Marker m = enter_section_(b, l, _NONE_, BODY, "<body>");
    while (true) {
      int c = current_position_(b);
      if (!declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "body", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // qualifiedName typeArguments?
  public static boolean classOrInterfaceType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classOrInterfaceType")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CLASS_OR_INTERFACE_TYPE, null);
    r = qualifiedName(b, l + 1);
    p = r; // pin = 1
    r = r && classOrInterfaceType_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // typeArguments?
  private static boolean classOrInterfaceType_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classOrInterfaceType_1")) return false;
    typeArguments(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // parcelableDeclaration | interfaceDeclaration
  static boolean declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = parcelableDeclaration(b, l + 1);
    if (!r) r = interfaceDeclaration(b, l + 1);
    exit_section_(b, l, m, r, false, declarationRecover_parser_);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean declarationName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declarationName")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, DECLARATION_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // !(INTERFACE_KEYWORD | PARCELABLE_KEYWORD)
  static boolean declarationRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declarationRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !declarationRecover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // INTERFACE_KEYWORD | PARCELABLE_KEYWORD
  private static boolean declarationRecover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declarationRecover_0")) return false;
    boolean r;
    r = consumeToken(b, INTERFACE_KEYWORD);
    if (!r) r = consumeToken(b, PARCELABLE_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // IN_KEYWORD | OUT_KEYWORD | INOUT_KEYWORD
  public static boolean direction(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "direction")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DIRECTION, "<direction>");
    r = consumeToken(b, IN_KEYWORD);
    if (!r) r = consumeToken(b, OUT_KEYWORD);
    if (!r) r = consumeToken(b, INOUT_KEYWORD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // headers body
  static boolean document(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "document")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = headers(b, l + 1);
    p = r; // pin = 1
    r = r && body(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // packageStatement* importStatement*
  public static boolean headers(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "headers")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, HEADERS, "<headers>");
    r = headers_0(b, l + 1);
    r = r && headers_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // packageStatement*
  private static boolean headers_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "headers_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!packageStatement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "headers_0", c)) break;
    }
    return true;
  }

  // importStatement*
  private static boolean headers_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "headers_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!importStatement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "headers_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // IMPORT_KEYWORD qualifiedName ';'
  public static boolean importStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatement")) return false;
    if (!nextTokenIs(b, IMPORT_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_STATEMENT, null);
    r = consumeToken(b, IMPORT_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, qualifiedName(b, l + 1));
    r = p && consumeToken(b, SEMICOLON) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // interfaceHeader declarationName '{' methodDeclarations '}'
  public static boolean interfaceDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, INTERFACE_DECLARATION, "<interface declaration>");
    r = interfaceHeader(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, declarationName(b, l + 1));
    r = p && report_error_(b, consumeToken(b, LCURLY)) && r;
    r = p && report_error_(b, methodDeclarations(b, l + 1)) && r;
    r = p && consumeToken(b, RCURLY) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !('}')
  static boolean interfaceDeclarationRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclarationRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RCURLY);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // INTERFACE_KEYWORD | ONEWAY INTERFACE_KEYWORD | RPC_KEYWORD
  static boolean interfaceHeader(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceHeader")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INTERFACE_KEYWORD);
    if (!r) r = parseTokens(b, 0, ONEWAY, INTERFACE_KEYWORD);
    if (!r) r = consumeToken(b, RPC_KEYWORD);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ONEWAY_KEYWORD? type declarationName parameters ('=' IDVALUE)? ';'
  public static boolean methodDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, METHOD_DECLARATION, "<method declaration>");
    r = methodDeclaration_0(b, l + 1);
    r = r && type(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, declarationName(b, l + 1));
    r = p && report_error_(b, parameters(b, l + 1)) && r;
    r = p && report_error_(b, methodDeclaration_4(b, l + 1)) && r;
    r = p && consumeToken(b, SEMICOLON) && r;
    exit_section_(b, l, m, r, p, methodDeclarationRecover_parser_);
    return r || p;
  }

  // ONEWAY_KEYWORD?
  private static boolean methodDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclaration_0")) return false;
    consumeToken(b, ONEWAY_KEYWORD);
    return true;
  }

  // ('=' IDVALUE)?
  private static boolean methodDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclaration_4")) return false;
    methodDeclaration_4_0(b, l + 1);
    return true;
  }

  // '=' IDVALUE
  private static boolean methodDeclaration_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclaration_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, EQUALS, IDVALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(ONEWAY_KEYWORD | IDENTIFIER | primitiveType | VOID_KEYWORD | INTERFACE_KEYWORD | '}' )
  static boolean methodDeclarationRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclarationRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !methodDeclarationRecover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ONEWAY_KEYWORD | IDENTIFIER | primitiveType | VOID_KEYWORD | INTERFACE_KEYWORD | '}'
  private static boolean methodDeclarationRecover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclarationRecover_0")) return false;
    boolean r;
    r = consumeToken(b, ONEWAY_KEYWORD);
    if (!r) r = consumeToken(b, IDENTIFIER);
    if (!r) r = primitiveType(b, l + 1);
    if (!r) r = consumeToken(b, VOID_KEYWORD);
    if (!r) r = consumeToken(b, INTERFACE_KEYWORD);
    if (!r) r = consumeToken(b, RCURLY);
    return r;
  }

  /* ********************************************************** */
  // methodDeclaration*
  static boolean methodDeclarations(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "methodDeclarations")) return false;
    Marker m = enter_section_(b, l, _NONE_);
    while (true) {
      int c = current_position_(b);
      if (!methodDeclaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "methodDeclarations", c)) break;
    }
    exit_section_(b, l, m, true, false, interfaceDeclarationRecover_parser_);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean nameComponent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "nameComponent")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, NAME_COMPONENT, r);
    return r;
  }

  /* ********************************************************** */
  // PACKAGE_KEYWORD qualifiedName ';'
  public static boolean packageStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageStatement")) return false;
    if (!nextTokenIs(b, PACKAGE_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PACKAGE_STATEMENT, null);
    r = consumeToken(b, PACKAGE_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, qualifiedName(b, l + 1));
    r = p && consumeToken(b, SEMICOLON) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // direction? type IDENTIFIER
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER, "<parameter>");
    r = parameter_0(b, l + 1);
    r = r && type(b, l + 1);
    p = r; // pin = 2
    r = r && consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // direction?
  private static boolean parameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_0")) return false;
    direction(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // parameter (',' parameter)*
  static boolean parameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = parameter(b, l + 1);
    r = r && parameterList_1(b, l + 1);
    exit_section_(b, l, m, r, false, parameterListRecover_parser_);
    return r;
  }

  // (',' parameter)*
  private static boolean parameterList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameterList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameterList_1", c)) break;
    }
    return true;
  }

  // ',' parameter
  private static boolean parameterList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(')')
  static boolean parameterListRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterListRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RPARENTH);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '(' parameterList? ')'
  static boolean parameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters")) return false;
    if (!nextTokenIs(b, LPARENTH)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LPARENTH);
    p = r; // pin = 1
    r = r && report_error_(b, parameters_1(b, l + 1));
    r = p && consumeToken(b, RPARENTH) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parameterList?
  private static boolean parameters_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_1")) return false;
    parameterList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // PARCELABLE_KEYWORD declarationName ';' |  PARCELABLE_KEYWORD ';' |
  //                     FLATTENABLE_KEYWORD declarationName ';'  |   FLATTENABLE_KEYWORD ';'
  public static boolean parcelableDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelableDeclaration")) return false;
    if (!nextTokenIs(b, "<parcelable declaration>", FLATTENABLE_KEYWORD, PARCELABLE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARCELABLE_DECLARATION, "<parcelable declaration>");
    r = parcelableDeclaration_0(b, l + 1);
    if (!r) r = parseTokens(b, 0, PARCELABLE_KEYWORD, SEMICOLON);
    if (!r) r = parcelableDeclaration_2(b, l + 1);
    if (!r) r = parseTokens(b, 0, FLATTENABLE_KEYWORD, SEMICOLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PARCELABLE_KEYWORD declarationName ';'
  private static boolean parcelableDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelableDeclaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PARCELABLE_KEYWORD);
    r = r && declarationName(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // FLATTENABLE_KEYWORD declarationName ';'
  private static boolean parcelableDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelableDeclaration_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FLATTENABLE_KEYWORD);
    r = r && declarationName(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // BOOLEAN_KEYWORD
  //   |   BYTE_KEYWORD
  //   |   CHAR_KEYWORD
  //   |   SHORT_KEYWORD
  //   |   INT_KEYWORD
  //   |   LONG_KEYWORD
  //   |   FLOAT_KEYWORD
  //   |   DOUBLE_KEYWORD
  public static boolean primitiveType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primitiveType")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMITIVE_TYPE, "<primitive type>");
    r = consumeToken(b, BOOLEAN_KEYWORD);
    if (!r) r = consumeToken(b, BYTE_KEYWORD);
    if (!r) r = consumeToken(b, CHAR_KEYWORD);
    if (!r) r = consumeToken(b, SHORT_KEYWORD);
    if (!r) r = consumeToken(b, INT_KEYWORD);
    if (!r) r = consumeToken(b, LONG_KEYWORD);
    if (!r) r = consumeToken(b, FLOAT_KEYWORD);
    if (!r) r = consumeToken(b, DOUBLE_KEYWORD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // nameComponent ("." nameComponent)*
  public static boolean qualifiedName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedName")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = nameComponent(b, l + 1);
    r = r && qualifiedName_1(b, l + 1);
    exit_section_(b, m, QUALIFIED_NAME, r);
    return r;
  }

  // ("." nameComponent)*
  private static boolean qualifiedName_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedName_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!qualifiedName_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "qualifiedName_1", c)) break;
    }
    return true;
  }

  // "." nameComponent
  private static boolean qualifiedName_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedName_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ".");
    r = r && nameComponent(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // VOID_KEYWORD | ((primitiveType | classOrInterfaceType) ('[' ']')*)
  public static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, TYPE, "<type>");
    r = consumeToken(b, VOID_KEYWORD);
    if (!r) r = type_1(b, l + 1);
    exit_section_(b, l, m, r, false, type_recover_parser_);
    return r;
  }

  // (primitiveType | classOrInterfaceType) ('[' ']')*
  private static boolean type_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_1_0(b, l + 1);
    r = r && type_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // primitiveType | classOrInterfaceType
  private static boolean type_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_1_0")) return false;
    boolean r;
    r = primitiveType(b, l + 1);
    if (!r) r = classOrInterfaceType(b, l + 1);
    return r;
  }

  // ('[' ']')*
  private static boolean type_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!type_1_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "type_1_1", c)) break;
    }
    return true;
  }

  // '[' ']'
  private static boolean type_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACKET, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '<' type (',' type)* '>'
  public static boolean typeArguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE_ARGUMENTS, null);
    r = consumeToken(b, LT);
    p = r; // pin = 1
    r = r && report_error_(b, type(b, l + 1));
    r = p && report_error_(b, typeArguments_2(b, l + 1)) && r;
    r = p && consumeToken(b, GT) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' type)*
  private static boolean typeArguments_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!typeArguments_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "typeArguments_2", c)) break;
    }
    return true;
  }

  // ',' type
  private static boolean typeArguments_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && type(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(qualifiedName | '(' | ',' | '>' | '{')
  static boolean type_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !type_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // qualifiedName | '(' | ',' | '>' | '{'
  private static boolean type_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_recover_0")) return false;
    boolean r;
    r = qualifiedName(b, l + 1);
    if (!r) r = consumeToken(b, LPARENTH);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, GT);
    if (!r) r = consumeToken(b, LCURLY);
    return r;
  }

  static final Parser declarationRecover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return declarationRecover(b, l + 1);
    }
  };
  static final Parser interfaceDeclarationRecover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return interfaceDeclarationRecover(b, l + 1);
    }
  };
  static final Parser methodDeclarationRecover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return methodDeclarationRecover(b, l + 1);
    }
  };
  static final Parser parameterListRecover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return parameterListRecover(b, l + 1);
    }
  };
  static final Parser type_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return type_recover(b, l + 1);
    }
  };
}
