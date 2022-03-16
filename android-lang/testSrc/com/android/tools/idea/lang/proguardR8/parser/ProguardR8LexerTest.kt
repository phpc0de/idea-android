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
package com.android.tools.idea.lang.proguardR8.parser

import com.android.tools.idea.lang.AndroidLexerTestCase
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.ANY_TYPE_
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.ANY_TYPE_AND_NUM_OF_ARGS
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.ASTERISK
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.AT
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.CLASS
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.CLOSE_BRACE
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.COLON
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.COMMA
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.DOT
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.DOUBLE_ASTERISK
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.DOUBLE_QUOTED_STRING
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.FILE_NAME
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.FLAG_TOKEN
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.IMPLEMENTS
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.INT
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.JAVA_IDENTIFIER
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.JAVA_IDENTIFIER_WITH_WILDCARDS
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.LPAREN
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.OPEN_BRACE
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.PUBLIC
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.RPAREN
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.SEMICOLON
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.SINGLE_QUOTED_STRING
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.STATIC
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes.UNTERMINATED_DOUBLE_QUOTED_STRING
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes._INIT_
import com.android.tools.idea.lang.proguardR8.psi.ProguardR8PsiTypes._METHODS_
import com.google.common.truth.Truth.assertThat

class ProguardR8LexerTest : AndroidLexerTestCase(ProguardR8Lexer()) {

  fun testOneRule() {
    assertTokenTypes(
      "-android",
      "-android" to FLAG_TOKEN
    )
  }

  fun testSimpleRule() {
    assertTokenTypes(
      "-android -dontpreverify",
      "-android" to FLAG_TOKEN,
      SPACE,
      "-dontpreverify" to FLAG_TOKEN
    )

    assertTokenTypes(
      "-android\n-dontpreverify",
      "-android" to FLAG_TOKEN,
      NEWLINE,
      "-dontpreverify" to FLAG_TOKEN
    )
  }

  fun testSimpleRuleWithArg() {
    assertTokenTypes(
      """
        -outjars bin/application.apk
        -libraryjars /usr/local/android-sdk/platforms/android-28/android.jar
      """.trimIndent(),

      "-outjars" to FLAG_TOKEN,
      SPACE,
      "bin/application.apk" to FILE_NAME,
      NEWLINE,
      "-libraryjars" to FLAG_TOKEN,
      SPACE,
      "/usr/local/android-sdk/platforms/android-28/android.jar" to FILE_NAME
    )
  }

  fun testRuleWithClassSpecification() {
    assertTokenTypes(
      """
        -keepclasseswithmembers class * {
        public <init>(androi_d.content.Context, android.util.AttributeSet, int);
        }
      """.trimIndent(),
      "-keepclasseswithmembers" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "*" to ASTERISK,
      SPACE,
      "{" to OPEN_BRACE,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "<init>" to _INIT_,
      "(" to LPAREN,
      "androi_d" to JAVA_IDENTIFIER,
      "." to DOT,
      "content" to JAVA_IDENTIFIER,
      "." to DOT,
      "Context" to JAVA_IDENTIFIER,
      "," to COMMA,
      SPACE,
      "android" to JAVA_IDENTIFIER,
      "." to DOT,
      "util" to JAVA_IDENTIFIER,
      "." to DOT,
      "AttributeSet" to JAVA_IDENTIFIER,
      "," to COMMA,
      SPACE,
      "int" to INT,
      ")" to RPAREN,
      ";" to SEMICOLON,
      NEWLINE,
      "}" to CLOSE_BRACE
    )
  }

  fun testDistinguishAnnotationAndFilename() {
    assertTokenTypes(
      """
        -keepclassmembers class * implements @javax.annotation.Resource java.io.Serializable
      """.trimIndent(),
      "-keepclassmembers" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "*" to ASTERISK,
      SPACE,
      "implements" to IMPLEMENTS,
      SPACE,
      "@" to AT,
      "javax" to JAVA_IDENTIFIER,
      "." to DOT,
      "annotation" to JAVA_IDENTIFIER,
      "." to DOT,
      "Resource" to JAVA_IDENTIFIER,
      SPACE,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "io" to JAVA_IDENTIFIER,
      "." to DOT,
      "Serializable" to JAVA_IDENTIFIER
    )
  }

  fun testDifferentJavaArgumentTypes() {
    assertTokenTypes(
      """
        -assumenoexternalsideeffects class **java.lang.StringBuilder {
        public java.lang.StringBuilder();
        public java.lang.StringBuilder(...);
        public java.lang.StringBuilder(int);
        public java.lang.StringBuilder append(java.lang.StringBuffer);
        }
      """.trimIndent(),
      "-assumenoexternalsideeffects" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "**java" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      SPACE,
      "{" to OPEN_BRACE,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      "(" to LPAREN,
      ")" to RPAREN,
      ";" to SEMICOLON,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      "(" to LPAREN,
      "..." to ANY_TYPE_AND_NUM_OF_ARGS,
      ")" to RPAREN,
      ";" to SEMICOLON,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      "(" to LPAREN,
      "int" to INT,
      ")" to RPAREN,
      ";" to SEMICOLON,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      SPACE,
      "append" to JAVA_IDENTIFIER,
      "(" to LPAREN,
      "java" to JAVA_IDENTIFIER,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuffer" to JAVA_IDENTIFIER,
      ")" to RPAREN,
      ";" to SEMICOLON,
      NEWLINE,
      "}" to CLOSE_BRACE
    )
  }

  fun testFileList() {
    assertTokenTypes(
      """-injars "my program.jar":'/your directory/your program.jar';<java.home>/lib/rt.jar""".trimIndent(),
      "-injars" to FLAG_TOKEN,
      SPACE,
      "\"my program.jar\"" to DOUBLE_QUOTED_STRING,
      ":" to COLON,
      "'/your directory/your program.jar'" to SINGLE_QUOTED_STRING,
      ";" to SEMICOLON,
      "<java.home>/lib/rt.jar" to FILE_NAME
    )
  }

  fun testInnerClass() {
    assertTokenTypes(
      "-keepclassmembers class **.R${'$'}InnerClass",
      "-keepclassmembers" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "**" to DOUBLE_ASTERISK,
      "." to DOT,
      "R${'$'}InnerClass" to JAVA_IDENTIFIER
    )
  }

  fun testClassSpecification() {
    assertTokenTypes(
      """
        -assumenoexternalsideeffects class **java.lang.StringBuilder {
        static *** fieldName;
        public <methods>;
        }
      """.trimIndent(),
      "-assumenoexternalsideeffects" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "**java" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      "." to DOT,
      "lang" to JAVA_IDENTIFIER,
      "." to DOT,
      "StringBuilder" to JAVA_IDENTIFIER,
      SPACE,
      "{" to OPEN_BRACE,
      NEWLINE,
      "static" to STATIC,
      SPACE,
      "***" to ANY_TYPE_,
      SPACE,
      "fieldName" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "public" to PUBLIC,
      SPACE,
      "<methods>" to _METHODS_,
      ";" to SEMICOLON,
      NEWLINE,
      "}" to CLOSE_BRACE
    )
  }

  fun testStrangeJavaIdentifiers() {
    assertTokenTypes(
      """
          -keep class i.am.NormalOne {
          int _9pins;
          int ανδρος;
          int OReilly;
          int кирилиц;
          int 弛;
          }
      """.trimIndent(),
      "-keep" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "i" to JAVA_IDENTIFIER,
      "." to DOT,
      "am" to JAVA_IDENTIFIER,
      "." to DOT,
      "NormalOne" to JAVA_IDENTIFIER,
      SPACE,
      "{" to OPEN_BRACE,
      NEWLINE,
      "int" to INT,
      SPACE,
      "_9pins" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "ανδρος" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "OReilly" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "кирилиц" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "弛" to JAVA_IDENTIFIER,
      ";" to SEMICOLON,
      NEWLINE,
      "}" to CLOSE_BRACE
    )
  }

  fun testWildcardsJavaIdentifiers() {
    assertTokenTypes(
      """
          -keep class i.am.NormalOne {
          int ident1fier?;
          int ?ident1fier;
          int ident1fier*ident1fier;
          int **ident1fier**;
          int **弛?ident1fier**弛*ident1fier?;
          }
      """.trimIndent(),
      "-keep" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "i" to JAVA_IDENTIFIER,
      "." to DOT,
      "am" to JAVA_IDENTIFIER,
      "." to DOT,
      "NormalOne" to JAVA_IDENTIFIER,
      SPACE,
      "{" to OPEN_BRACE,
      NEWLINE,
      "int" to INT,
      SPACE,
      "ident1fier?" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "?ident1fier" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "ident1fier*ident1fier" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "**ident1fier**" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      ";" to SEMICOLON,
      NEWLINE,
      "int" to INT,
      SPACE,
      "**弛?ident1fier**弛*ident1fier?" to JAVA_IDENTIFIER_WITH_WILDCARDS,
      ";" to SEMICOLON,
      NEWLINE,
      "}" to CLOSE_BRACE
    )
  }

  fun testIsJavaIdentifier() {
    assertThat(ProguardR8Lexer.isJavaIdentifier("simple")).isTrue()
    assertThat(ProguardR8Lexer.isJavaIdentifier("ανδρος")).isTrue()
    assertThat(ProguardR8Lexer.isJavaIdentifier("wildcards**")).isFalse()
    assertThat(ProguardR8Lexer.isJavaIdentifier("goodStart^^^^")).isFalse()
    assertThat(ProguardR8Lexer.isJavaIdentifier("public")).isFalse()
  }

  fun testFileNameAfterAt() {
    assertTokenTypes(
      """
      @keep-rules.txt
      -secondrule
      """.trimIndent(),
      "@" to AT,
      "keep-rules.txt" to FILE_NAME,
      NEWLINE,
      "-secondrule" to FLAG_TOKEN
    )
  }

  fun testAnnotationInKeepRuleHeader() {
    assertTokenTypes(
      """
      -keep @annotation
      """.trimIndent(),
      "-keep" to FLAG_TOKEN,
      SPACE,
      "@" to AT,
      "annotation" to JAVA_IDENTIFIER
    )
  }

  fun testFileNamesAtSameLine() {
    assertTokenTypes(
      """
      @file @file
      """.trimIndent(),
      "@" to AT,
      "file" to FILE_NAME,
      SPACE,
      "@" to AT,
      "file" to FILE_NAME
    )
  }

  fun testBackReferenceWildcard() {
    assertTokenTypes(
      """
      -keep class **${'$'}D<2>
      """.trimIndent(),
      "-keep" to FLAG_TOKEN,
      SPACE,
      "class" to CLASS,
      SPACE,
      "**${'$'}D<2>" to JAVA_IDENTIFIER_WITH_WILDCARDS
    )
  }

  fun testQuotingInFileNames() {
    assertTokenTypes(
      """
        -printusage 'xxx'
        -printusage "xxx"
        -printusage 'xxx xxx'
        -printusage "xxx xxx"
        -printusage "'xxx'"
        -printusage " xxx xxx ("
        -printusage '"xxx"'
        -printusage " xxx xxx "
        -printusage ' xxx xxx '
        -printusage "xxx'
      """.trimIndent(),
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "'xxx'" to SINGLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\"xxx\"" to DOUBLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "'xxx xxx'" to SINGLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\"xxx xxx\"" to DOUBLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\"'xxx'\"" to DOUBLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\" xxx xxx (\"" to DOUBLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "'\"xxx\"'" to SINGLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\" xxx xxx \"" to DOUBLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "' xxx xxx '" to SINGLE_QUOTED_STRING,
      NEWLINE,
      "-printusage" to FLAG_TOKEN,
      SPACE,
      "\"xxx'" to UNTERMINATED_DOUBLE_QUOTED_STRING
    )
  }
}
