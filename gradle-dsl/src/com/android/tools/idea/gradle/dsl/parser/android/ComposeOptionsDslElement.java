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
package com.android.tools.idea.gradle.dsl.parser.android;

import static com.android.tools.idea.gradle.dsl.model.android.ComposeOptionsModelImpl.KOTLIN_COMPILER_EXTENSION_VERSION;
import static com.android.tools.idea.gradle.dsl.model.android.ComposeOptionsModelImpl.KOTLIN_COMPILER_VERSION;
import static com.android.tools.idea.gradle.dsl.parser.semantics.ArityHelper.exactly;
import static com.android.tools.idea.gradle.dsl.parser.semantics.ArityHelper.property;
import static com.android.tools.idea.gradle.dsl.parser.semantics.MethodSemanticsDescription.SET;
import static com.android.tools.idea.gradle.dsl.parser.semantics.ModelMapCollector.toModelMap;
import static com.android.tools.idea.gradle.dsl.parser.semantics.PropertySemanticsDescription.VAR;

import com.android.tools.idea.gradle.dsl.parser.GradleDslNameConverter;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslBlockElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleNameElement;
import com.android.tools.idea.gradle.dsl.parser.groovy.GroovyDslNameConverter;
import com.android.tools.idea.gradle.dsl.parser.kotlin.KotlinDslNameConverter;
import com.android.tools.idea.gradle.dsl.parser.semantics.ModelEffectDescription;
import com.android.tools.idea.gradle.dsl.parser.semantics.PropertiesElementDescription;
import com.android.tools.idea.gradle.dsl.parser.semantics.SurfaceSyntaxDescription;
import com.google.common.collect.ImmutableMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ComposeOptionsDslElement extends GradleDslBlockElement {
  public static final PropertiesElementDescription<ComposeOptionsDslElement> COMPOSE_OPTIONS =
    new PropertiesElementDescription<>("composeOptions", ComposeOptionsDslElement.class, ComposeOptionsDslElement::new);

  public static final ImmutableMap<SurfaceSyntaxDescription, ModelEffectDescription> ktsToModelMap = Stream.of(new Object[][]{
    {"kotlinCompilerExtensionVersion", property, KOTLIN_COMPILER_EXTENSION_VERSION, VAR},
    {"kotlinCompilerVersion", property, KOTLIN_COMPILER_VERSION, VAR}
  }).collect(toModelMap());

  public static final ImmutableMap<SurfaceSyntaxDescription, ModelEffectDescription> groovyToModelMap = Stream.of(new Object[][] {
    {"kotlinCompilerExtensionVersion", property, KOTLIN_COMPILER_EXTENSION_VERSION, VAR},
    {"kotlinCompilerExtensionVersion", exactly(1), KOTLIN_COMPILER_EXTENSION_VERSION, SET},
    {"kotlinCompilerVersion", property, KOTLIN_COMPILER_VERSION, VAR},
    {"kotlinCompilerVersion", exactly(1), KOTLIN_COMPILER_VERSION, SET}
  }).collect(toModelMap());

  @Override
  public @NotNull ImmutableMap<SurfaceSyntaxDescription, ModelEffectDescription> getExternalToModelMap(@NotNull GradleDslNameConverter converter) {
    if (converter instanceof KotlinDslNameConverter) {
      return ktsToModelMap;
    }
    else if (converter instanceof GroovyDslNameConverter) {
      return groovyToModelMap;
    }
    else {
      return super.getExternalToModelMap(converter);
    }
  }

  ComposeOptionsDslElement(@NotNull GradleDslElement parent, @NotNull GradleNameElement name) {
    super(parent, name);
  }
}
