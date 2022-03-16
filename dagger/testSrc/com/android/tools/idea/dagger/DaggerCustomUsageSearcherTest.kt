/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.dagger

import com.android.tools.idea.testing.caret
import com.android.tools.idea.testing.loadNewFile
import com.android.tools.idea.testing.moveCaret
import com.google.common.truth.Truth.assertThat
import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.registerServiceInstance
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.impl.UsageViewImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.junit.Assert

class DaggerCustomUsageSearcherTest : DaggerTestCase() {

  private fun findAllUsages(targetElement: PsiElement): MutableSet<Usage> {
    val usagesManager = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager
    val handler = usagesManager.getFindUsagesHandler(targetElement, false)
    Assert.assertNotNull("Cannot find handler for: $targetElement", handler)
    val usageView = usagesManager.doFindUsages(handler!!.primaryElements,
                                               handler.secondaryElements,
                                               handler,
                                               handler.findUsagesOptions,
                                               false) as UsageViewImpl

    return usageView.usages
  }

  fun testProviders() {
    myFixture.addClass(
      //language=JAVA
      """
        package myExample;

        import dagger.Provides;
        import dagger.Module;

        @Module
        class MyModule {
          @Provides String provider() {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package myExample;

        import javax.inject.Inject;

        class MyClass {
          @Inject String ${caret}injectedString;
        }
      """.trimIndent()
    )

    val trackerService = TestDaggerAnalyticsTracker()
    project.registerServiceInstance(DaggerAnalyticsTracker::class.java, trackerService)

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |    myExample (1 usage)
      |     MyModule (1 usage)
      |      provider() (1 usage)
      |       8@Provides String provider() {}
      """.trimMargin()
    )

    assertThat(trackerService.calledMethods).hasSize(1)
    assertThat(trackerService.calledMethods.last()).startsWith("trackFindUsagesNodeWasDisplayed owner: CONSUMER time:")
    assertThat(trackerService.calledMethods.last()
                 .removePrefix("trackFindUsagesNodeWasDisplayed owner: CONSUMER time: ").toInt()).isNotNull()

    val usage = findAllUsages(myFixture.elementAtCaret).filterIsInstance<UsageInfo2UsageAdapter>().first()

    assertThat(trackerService.calledMethods).hasSize(2)
    assertThat(trackerService.calledMethods.last()).startsWith("trackFindUsagesNodeWasDisplayed owner: CONSUMER time: ")
    assertThat(trackerService.calledMethods.last()
                 .removePrefix("trackFindUsagesNodeWasDisplayed owner: CONSUMER time: ").toInt()).isNotNull()

    usage.navigate(false)

    assertThat(trackerService.calledMethods.last()).isEqualTo("trackNavigation CONTEXT_USAGES CONSUMER PROVIDER")
  }

  fun testProvidersFromKotlin() {
    myFixture.addFileToProject(
      "MyClass.kt",
      //language=kotlin
      """
        package example

        import dagger.Provides
        import dagger.BindsInstance
        import dagger.Module

        @Module
        class MyModule {
          @Provides fun provider():String {}
          @Provides fun providerInt():Int {}
          @BindsInstance fun bindsMethod():String {}
          fun builder(@BindsInstance str:String) {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package example;

        import javax.inject.Inject;

        class MyClass {
          @Inject String ${caret}injectedString;
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (3 usages)
      |  Providers (3 usages)
      |   ${module.name} (3 usages)
      |     (3 usages)
      |     MyClass.kt (3 usages)
      |      MyModule (3 usages)
      |       builder (1 usage)
      |        12fun builder(@BindsInstance str:String) {}
      |       9@Provides fun provider():String {}
      |       11@BindsInstance fun bindsMethod():String {}
      """.trimMargin()
    )
  }

  fun testInjectedConstructor() {
    myFixture.addClass(
      //language=JAVA
      """
        package myExample;

        import javax.inject.Inject;

        public class MyProvider {
          @Inject public MyProvider() {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package myExample;

        import javax.inject.Inject;

        class MyClass {
          @Inject MyProvider ${caret}injectedString;
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |    myExample (1 usage)
      |     MyProvider (1 usage)
      |      MyProvider() (1 usage)
      |       6@Inject public MyProvider() {}
      """.trimMargin()
    )
  }

  fun testInjectedConstructor_kotlin() {
    myFixture.addFileToProject(
      "MyProvider.kt",
      //language=kotlin
      """
        import javax.inject.Inject

        class MyProvider @Inject constructor()
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        import javax.inject.Inject;

        class MyClass {
          @Inject MyProvider ${caret}injectedString;
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |     (1 usage)
      |     MyProvider.kt (1 usage)
      |      MyProvider (1 usage)
      |       3class MyProvider @Inject constructor()
      """.trimMargin()
    )
  }

  fun testBinds() {
    myFixture.addClass(
      //language=JAVA
      """
        package myExample;

        import dagger.Binds;
        import dagger.Module;

        @Module
        abstract class MyModule {
          @Binds abstract String bindsMethod(String s) {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package myExample;

        import javax.inject.Inject;

        class MyClass {
          @Inject String ${caret}injectedString;
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |    myExample (1 usage)
      |     MyModule (1 usage)
      |      bindsMethod(String) (1 usage)
      |       8@Binds abstract String bindsMethod(String s) {}
      """.trimMargin()
    )
  }

  fun testBindsFromKotlin() {
    myFixture.addFileToProject(
      "MyClass.kt",
      //language=kotlin
      """
        package example

        import dagger.Binds
        import dagger.Module

        @Module
        abstract class MyModule {
          @Binds abstract fun bindsMethod(s: String):String {}
          @Binds abstract fun bindsMethodInt(i: Int):Int {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package example;

        import javax.inject.Inject;

        class MyClass {
          @Inject String ${caret}injectedString;
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |     (1 usage)
      |     MyClass.kt (1 usage)
      |      MyModule (1 usage)
      |       8@Binds abstract fun bindsMethod(s: String):String {}
      """.trimMargin()
    )
  }

  fun testBinds_for_param() {
    myFixture.addFileToProject(
      "MyClass.kt",
      //language=kotlin
      """
        package example

        import dagger.Binds
        import dagger.Module

        @Module
        abstract class MyModule {
          @Binds abstract fun bindsMethod(s: String):String {}
          @Binds abstract fun bindsMethodInt(i: Int):Int {}
        }
      """.trimIndent()
    )

    // JAVA consumer
    myFixture.configureByText(
      //language=JAVA
      JavaFileType.INSTANCE,
      """
        package example;

        import javax.inject.Inject;

        class MyClass {
          @Inject MyClass(String ${caret}str) {}
        }
      """.trimIndent()
    )

    var presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |     (1 usage)
      |     MyClass.kt (1 usage)
      |      MyModule (1 usage)
      |       8@Binds abstract fun bindsMethod(s: String):String {}
      """.trimMargin()
    )

    // kotlin consumer
    myFixture.configureByText(
      //language=kotlin
      KotlinFileType.INSTANCE,
      """
        import javax.inject.Inject

        class MyClass @Inject constructor(${caret}strKotlin: String)
      """.trimIndent()
    )

    presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |     (1 usage)
      |     MyClass.kt (1 usage)
      |      MyModule (1 usage)
      |       8@Binds abstract fun bindsMethod(s: String):String {}
      """.trimMargin()
    )
  }

  fun testDaggerConsumer() {
    // Dagger provider.
    myFixture.loadNewFile("example/MyProvider.java",
      //language=JAVA
                          """
        package example;

        import javax.inject.Inject;

        public class MyProvider {
          @Inject public MyProvider() {}
        }
      """.trimIndent()
    ).containingFile

    val provider = myFixture.moveCaret("public MyProvi|der()").parentOfType<PsiMethod>()!!

    // Dagger consumer as param of @Provides-annotated method.
    myFixture.addClass(
      //language=JAVA
      """
        package example;

        import dagger.Provides;
        import dagger.Module;

        @Module
        class MyModule {
          @Provides String provider(MyProvider consumer) {}
        }
      """.trimIndent()
    )

    // Dagger consumer as param of @Inject-annotated constructor.
    myFixture.addClass(
      //language=JAVA
      """
        package example;

        import javax.inject.Inject;

        public class MyClass {
          @Inject public MyClass(MyProvider consumer) {}
        }
      """.trimIndent()
    )

    // Dagger consumer as @Inject-annotated field.
    myFixture.addClass(
      //language=JAVA
      """
        package example;

        import javax.inject.Inject;

        public class MyClassWithInjectedField {
          @Inject MyProvider consumer;
        }
      """.trimIndent()
    )

    // Dagger consumer as @Inject-annotated field in Kotlin.
    myFixture.addFileToProject(
      "example/MyClassWithInjectedFieldKt.kt",
      //language=kotlin
      """
        package example

        import javax.inject.Inject

        class MyClassWithInjectedFieldKt {
          @Inject val consumer:MyProvider
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(provider)
    assertThat(presentation).contains(
      """
      | Found usages (4 usages)
      |  Consumers (4 usages)
      |   ${module.name} (4 usages)
      |    example (4 usages)
      |     MyClass (1 usage)
      |      MyClass(MyProvider) (1 usage)
      |       6@Inject public MyClass(MyProvider consumer) {}
      |     MyClassWithInjectedField (1 usage)
      |      6@Inject MyProvider consumer;
      |     MyModule (1 usage)
      |      provider(MyProvider) (1 usage)
      |       8@Provides String provider(MyProvider consumer) {}
      |     MyClassWithInjectedFieldKt.kt (1 usage)
      |      MyClassWithInjectedFieldKt (1 usage)
      |       6@Inject val consumer:MyProvider
      """.trimMargin()
    )
  }

  fun testDaggerComponentMethods() {
    val classFile = myFixture.addFileToProject(
      "test/MyClass.java",
      //language=JAVA
      """
      package test;

      import javax.inject.Inject;

      public class MyClass {
        @Inject public MyClass() {}
      }
    """.trimIndent()
    ).virtualFile

    val componentFile = myFixture.addFileToProject(
      "test/MyComponent.java",
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component()
      public interface MyComponent {
        MyClass getMyClass();
      }
    """.trimIndent()
    ).virtualFile

    myFixture.configureFromExistingVirtualFile(componentFile)
    val componentMethod = myFixture.moveCaret("getMyCl|ass").parentOfType<PsiMethod>()!!

    var presentation = myFixture.getUsageViewTreeTextRepresentation(componentMethod)

    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |    test (1 usage)
      |     MyClass (1 usage)
      |      MyClass() (1 usage)
      |       6@Inject public MyClass() {}
      """.trimMargin()
    )

    myFixture.configureFromExistingVirtualFile(classFile)
    val classProvider = myFixture.moveCaret("@Inject public MyCla|ss").parentOfType<PsiMethod>()!!

    presentation = myFixture.getUsageViewTreeTextRepresentation(classProvider)

    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Exposed by components (1 usage)
      |   ${module.name} (1 usage)
      |    test (1 usage)
      |     MyComponent (1 usage)
      |      getMyClass() (1 usage)
      |       6MyClass getMyClass();
      """.trimMargin()
    )
  }

  fun testEntryPointMethodsForProvider() {
    val classFile = myFixture.addClass(
      //language=JAVA
      """
      package test;

      import javax.inject.Inject;

      public class MyClass {
        @Inject public MyClass() {}
      }
    """.trimIndent()
    ).containingFile.virtualFile

    val entryPointFile = myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.hilt.EntryPoint;

      @EntryPoint
      public interface MyEntryPoint {
        MyClass getMyClassInEntryPoint();
      }
    """.trimIndent()
    ).containingFile.virtualFile

    myFixture.configureFromExistingVirtualFile(entryPointFile)
    val entryPointMethod = myFixture.moveCaret("getMyClassInEntry|Point").parentOfType<PsiMethod>()!!

    var presentation = myFixture.getUsageViewTreeTextRepresentation(entryPointMethod)

    assertThat(presentation).contains(
      """
      | Found usages (1 usage)
      |  Providers (1 usage)
      |   ${module.name} (1 usage)
      |    test (1 usage)
      |     MyClass (1 usage)
      |      MyClass() (1 usage)
      |       6@Inject public MyClass() {}
      """.trimMargin()
    )

    myFixture.configureFromExistingVirtualFile(classFile)
    val classProvider = myFixture.moveCaret("@Inject public MyCla|ss").parentOfType<PsiMethod>()!!

    presentation = myFixture.getUsageViewTreeTextRepresentation(classProvider)

    assertThat(presentation).contains(
      """
      |  Exposed by entry points (1 usage)
      |   ${module.name} (1 usage)
      |    test (1 usage)
      |     MyEntryPoint (1 usage)
      |      getMyClassInEntryPoint() (1 usage)
      |       6MyClass getMyClassInEntryPoint();
      """.trimMargin()
    )
  }

  fun testUsagesForModules() {
    val moduleFile = myFixture.addClass(
      //language=JAVA
      """
        package test;
        import dagger.Module;

        @Module
        class MyModule {}
      """.trimIndent()
    ).containingFile.virtualFile

    // Java Component
    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component(modules = { MyModule.class })
      public interface MyComponent {}
    """.trimIndent()
    )

    // Kotlin Component
    myFixture.addFileToProject("test/MyComponentKt.kt",
      //language=kotlin
                               """
      package test
      import dagger.Component

      @Component(modules = [MyModule::class])
      interface MyComponentKt
    """.trimIndent()
    )

    // Java Module
    myFixture.addClass(
      //language=JAVA
      """
        package test;
        import dagger.Module;

        @Module(includes = { MyModule.class })
        class MyModule2 {}
      """.trimIndent()
    )

    myFixture.configureFromExistingVirtualFile(moduleFile)
    val module = myFixture.moveCaret("class MyMod|ule {}").parentOfType<PsiClass>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(module)

    assertThat(presentation).contains(
      """
      |  Included in components (2 usages)
      |   ${myFixture.module.name} (2 usages)
      |    test (2 usages)
      |     MyComponent.java (1 usage)
      |      5public interface MyComponent {}
      |     MyComponentKt.kt (1 usage)
      |      5interface MyComponentKt
      |  Included in modules (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MyModule2.java (1 usage)
      |      5class MyModule2 {}
      """.trimMargin()
    )
  }

  fun testDependantComponentsForComponent() {
    // Java Component
    val componentFile = myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component
      public interface MyComponent {}
    """.trimIndent()
    ).containingFile.virtualFile

    // Kotlin Component
    myFixture.addFileToProject(
      "test/MyDependantComponent.kt",
      //language=kotlin
      """
      package test
      import dagger.Component

      @Component(dependencies = [MyComponent::class])
      interface MyDependantComponent
    """.trimIndent()
    )

    myFixture.configureFromExistingVirtualFile(componentFile)
    val component = myFixture.moveCaret("MyCompon|ent {}").parentOfType<PsiClass>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(component)

    assertThat(presentation).contains(
      """
      |  Parent components (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MyDependantComponent.kt (1 usage)
      |      5interface MyDependantComponent
      """.trimMargin()
    )
  }

  fun testParentsForSubcomponent() {
    // Java Subcomponent
    val subcomponentFile = myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Subcomponent;

      @Subcomponent
      public interface MySubcomponent {}
    """.trimIndent()
    ).containingFile.virtualFile

    myFixture.addClass(
      //language=JAVA
      """
        package test;

        import dagger.Module;

        @Module(subcomponents = { MySubcomponent.class })
        class MyModule { }
      """.trimIndent()
    )

    // Java Component
    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component(modules = { MyModule.class })
      public interface MyComponent {}
    """.trimIndent()
    )

    // Kotlin Component
    myFixture.addFileToProject(
      "test/MyComponentKt.kt",
      //language=kotlin
      """
      package test
      import dagger.Component

      @Component(modules = [ MyModule::class])
      interface MyComponentKt
    """.trimIndent()
    )


    // Java Subcomponent
    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Subcomponent;

      @Subcomponent(modules = { MyModule.class })
      public interface MyParentSubcomponent {}
    """.trimIndent()
    )

    myFixture.configureFromExistingVirtualFile(subcomponentFile)
    val component = myFixture.moveCaret("MySubcompon|ent").parentOfType<PsiClass>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(component)

    assertThat(presentation).contains(
      """
      |  Parent components (3 usages)
      |   ${myFixture.module.name} (3 usages)
      |    test (3 usages)
      |     MyComponent.java (1 usage)
      |      5public interface MyComponent {}
      |     MyParentSubcomponent.java (1 usage)
      |      5public interface MyParentSubcomponent {}
      |     MyComponentKt.kt (1 usage)
      |      5interface MyComponentKt
      """.trimMargin()
    )
  }

  fun testSubcomponentsForSubcomponent() {
    // Java Subcomponent
    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Subcomponent;

      @Subcomponent
      public interface MySubcomponent {}
    """.trimIndent()
    )

    myFixture.addClass(
      //language=JAVA
      """
        package test;

        import dagger.Module;

        @Module(subcomponents = { MySubcomponent.class })
        class MyModule { }
      """.trimIndent()
    )

    // Java parent Subcomponent
    val file = myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Subcomponent;

      @Subcomponent(modules = { MyModule.class })
      public interface MyParentSubcomponent {}
    """.trimIndent()
    ).containingFile.virtualFile


    myFixture.configureFromExistingVirtualFile(file)
    val component = myFixture.moveCaret("MyParent|Subcomponent").parentOfType<PsiClass>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(component)

    assertThat(presentation).contains(
      """
      |  Subcomponents (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MySubcomponent.java (1 usage)
      |      5public interface MySubcomponent {}
      """.trimMargin()
    )
  }

  fun testSubcomponentAndModulesForComponent() {
    // Java Subcomponent
    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Subcomponent;

      @Subcomponent
      public interface MySubcomponent {
        @Subcomponent.Builder
          interface Builder {}
      }
    """.trimIndent()
    )

    // Kotlin Subcomponent
    myFixture.addFileToProject(
      "test/MySubcomponent2.kt",
      //language=kotlin
      """
      package test
      import dagger.Subcomponent

      @Subcomponent
      interface MySubcomponent2
    """.trimIndent()
    )

    myFixture.addClass(
      //language=JAVA
      """
        package test;

        import dagger.Module;

        @Module(subcomponents = { MySubcomponent.class, MySubcomponent2.class })
        class MyModule { }
      """.trimIndent()
    )

    // Java Component
    val file = myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component(modules = { MyModule.class })
      public interface MyComponent {}
    """.trimIndent()
    ).containingFile.virtualFile

    myFixture.configureFromExistingVirtualFile(file)
    val component = myFixture.moveCaret("MyCompon|ent").parentOfType<PsiClass>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(component)

    assertThat(presentation).contains(
      """
      |  Subcomponents (2 usages)
      |   ${myFixture.module.name} (2 usages)
      |    test (2 usages)
      |     MySubcomponent.java (1 usage)
      |      5public interface MySubcomponent {
      |     MySubcomponent2.kt (1 usage)
      |      5interface MySubcomponent2
      """.trimMargin()
    )

    assertThat(presentation).contains(
      """
      |  Modules included (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MyModule.java (1 usage)
      |      6class MyModule { }
      """.trimMargin()
    )
  }


  fun testObjectClassInKotlin() {
    val moduleFile = myFixture.addFileToProject("test/MyModule.kt",
      //language=kotlin
                                                """
        package test
        import dagger.Module

        @Module
        object MyModule
      """.trimIndent()
    ).virtualFile

    myFixture.addClass(
      //language=JAVA
      """
      package test;
      import dagger.Component;

      @Component(modules = { MyModule.class })
      public interface MyComponent {}
    """.trimIndent()
    )

    myFixture.configureFromExistingVirtualFile(moduleFile)
    val module = myFixture.moveCaret("MyMod|ule").parentOfType<KtClassOrObject>()!!
    val presentation = myFixture.getUsageViewTreeTextRepresentation(module)

    assertThat(presentation).contains(
      """
      |  Included in components (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MyComponent.java (1 usage)
      |      5public interface MyComponent {}
      """.trimMargin()
    )
  }

  fun testProvidersKotlin() {
    myFixture.addClass(
      //language=JAVA
      """
        package example;

        import dagger.Provides;
        import dagger.Module;

        @Module
        class MyModule {
          @Provides String provider() {}
        }
      """.trimIndent()
    )

    myFixture.configureByText(
      KotlinFileType.INSTANCE,
      //language=kotlin
      """
        import javax.inject.Inject

        class MyClass {
          @Inject val injected<caret>String:String
        }
      """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
          | Found usages (1 usage)
          |  Providers (1 usage)
          |   ${myFixture.module.name} (1 usage)
          |    example (1 usage)
          |     MyModule (1 usage)
          |      provider() (1 usage)
          |       8@Provides String provider() {}
          """.trimMargin()
    )
  }

  fun testFromKotlinComponentToKotlinSubcomponent() {
    myFixture.addFileToProject(
      "test/MySubcomponent.kt",
      //language=kotlin
      """
      package test

      import dagger.Subcomponent

      @Subcomponent
      interface MySubcomponent
    """.trimIndent())

    myFixture.loadNewFile(
      "test/MyComponent.kt",
      //language=kotlin
      """
      package test

      import dagger.Component
      import dagger.Module

      @Module(subcomponents = [ MySubcomponent::class ])
      class MyModule

      @Component(modules = [ MyModule::class])
      interface MyComponen<caret>t
    """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
          |  Subcomponents (1 usage)
          |   ${myFixture.module.name} (1 usage)
          |    test (1 usage)
          |     MySubcomponent.kt (1 usage)
          |      6interface MySubcomponent
      """.trimMargin()
    )
  }

  fun testFromKotlinModuleToKotlinComponent() {
    myFixture.addFileToProject(
      "test/MyComponent.kt",
      //language=kotlin
      """
      package test

      import dagger.Component

      @Component(modules = [MyModule::class])
      interface MyComponent
    """.trimIndent()
    )


    myFixture.loadNewFile(
      "test/MyModule.kt",
      //language=kotlin
      """
      package test

      import dagger.Module

      @Module
      class MyModu<caret>le
    """.trimIndent()
    )

    val presentation = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
    assertThat(presentation).contains(
      """
      |  Included in components (1 usage)
      |   ${myFixture.module.name} (1 usage)
      |    test (1 usage)
      |     MyComponent.kt (1 usage)
      |      6interface MyComponent
      """.trimMargin()
    )
  }
}
