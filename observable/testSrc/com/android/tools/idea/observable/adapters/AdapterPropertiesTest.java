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
package com.android.tools.idea.observable.adapters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.android.tools.idea.observable.BatchInvoker;
import com.android.tools.idea.observable.BindingsManager;
import com.android.tools.idea.observable.core.DoubleProperty;
import com.android.tools.idea.observable.core.DoubleValueProperty;
import com.android.tools.idea.observable.core.IntProperty;
import com.android.tools.idea.observable.core.IntValueProperty;
import com.android.tools.idea.observable.core.StringProperty;
import com.android.tools.idea.observable.core.StringValueProperty;
import com.google.common.truth.Truth;
import java.util.Locale;
import org.junit.After;
import org.junit.Test;

public class AdapterPropertiesTest {
  @After
  public void resetLocale() {
    Locale.setDefault(Locale.US);
  }

  @Test
  public void initializingStringToIntAdapterWithValidValueWorks() throws Exception {
    StringProperty intString = new StringValueProperty("42");
    StringToIntAdapterProperty adapterProperty = new StringToIntAdapterProperty(intString);

    assertThat(adapterProperty.get()).isEqualTo(42);

    intString.set("1234");
    assertThat(adapterProperty.get()).isEqualTo(1234);
  }

  @Test
  public void initializingStringToIntAdapterWithInvalidValueDefaultsTo0() throws Exception {
    StringProperty intString = new StringValueProperty("Forty-two");
    StringToIntAdapterProperty adapterProperty = new StringToIntAdapterProperty(intString);

    assertThat(adapterProperty.get()).isEqualTo(0);
  }

  @Test
  public void bindingStringToIntAdapterWorks() throws Exception {
    BindingsManager bindings = new BindingsManager(BatchInvoker.INVOKE_IMMEDIATELY_STRATEGY);
    StringProperty intString = new StringValueProperty("0");
    IntProperty intValue = new IntValueProperty(1);

    bindings.bindTwoWay(new StringToIntAdapterProperty(intString), intValue);

    assertThat(intString.get()).isEqualTo("1");

    intString.set("-99");
    Truth.assertThat(intValue.get()).isEqualTo(-99);

    intString.set("not an int");
    Truth.assertThat(intValue.get()).isEqualTo(-99);
  }

  @Test
  public void initializingStringToDoubleAdapterWithValidValueWorks() throws Exception {
    StringProperty doubleString = new StringValueProperty("12.34");
    StringToDoubleAdapterProperty adapterProperty = new StringToDoubleAdapterProperty(doubleString, 2);

    Truth.assertThat(adapterProperty.inSync().get()).isTrue();
    assertThat(adapterProperty.get()).isWithin(0).of(12.34);

    doubleString.set("3");
    assertThat(adapterProperty.get()).isWithin(0).of(3.00);
  }

  @Test
  public void initializingStringToDoubleAdapterWithInvalidValueDefaultsTo0() throws Exception {
    StringProperty doubleString = new StringValueProperty("OneDotTwo");
    StringToDoubleAdapterProperty adapterProperty = new StringToDoubleAdapterProperty(doubleString);

    Truth.assertThat(adapterProperty.inSync().get()).isFalse();
    assertThat(adapterProperty.get()).isWithin(0f).of(0);
  }

  @Test
  public void bindingStringToDoubleAdapterWorks() throws Exception {
    BindingsManager bindings = new BindingsManager(BatchInvoker.INVOKE_IMMEDIATELY_STRATEGY);
    StringProperty doubleString = new StringValueProperty("0");
    DoubleProperty doubleValue = new DoubleValueProperty(20.0);

    // Defaults to 1 decimal point of precision
    StringToDoubleAdapterProperty adapterProperty = new StringToDoubleAdapterProperty(doubleString);
    bindings.bindTwoWay(adapterProperty, doubleValue);

    assertThat(doubleString.get()).isEqualTo("20");
    Truth.assertThat(adapterProperty.inSync().get()).isTrue();

    doubleString.set("100.5");
    assertThat(doubleValue.get()).isWithin(0.01).of(100.5);
    Truth.assertThat(adapterProperty.inSync().get()).isTrue();

    doubleString.set("not a double");
    assertThat(doubleValue.get()).isWithin(0.01).of(100.5);
    Truth.assertThat(adapterProperty.inSync().get()).isFalse();
  }

  @Test
  public void bindingStringToDoubleAdapterWithLocale() throws Exception {
    Locale.setDefault(Locale.ITALIAN);
    BindingsManager bindings = new BindingsManager(BatchInvoker.INVOKE_IMMEDIATELY_STRATEGY);
    StringProperty doubleString = new StringValueProperty("0");
    DoubleProperty doubleValue = new DoubleValueProperty(0.9876);

    bindings.bindTwoWay(new StringToDoubleAdapterProperty(doubleString, 2, 3), doubleValue);

    assertThat(doubleString.get()).isEqualTo("0,988");

    doubleValue.set(0.3);
    assertThat(doubleString.get()).isEqualTo("0,3");

    doubleValue.set(0.299);
    assertThat(doubleString.get()).isEqualTo("0,299");

    doubleValue.set(3.0);
    assertThat(doubleString.get()).isEqualTo("3");
  }

  @Test
  public void bindingStringToDoubleWithBadParameters() throws Exception {
    BindingsManager bindings = new BindingsManager(BatchInvoker.INVOKE_IMMEDIATELY_STRATEGY);
    StringProperty doubleString = new StringValueProperty("0");
    DoubleProperty doubleValue = new DoubleValueProperty(0.9876);
    //noinspection EmptyCatchBlock
    try {
      bindings.bindTwoWay(new StringToDoubleAdapterProperty(doubleString, 4, 3), doubleValue);
      fail("Expect an exception because maxDecimals is specified smaller than num decimals");
    }
    catch (IllegalArgumentException unused) {
    }
  }

  @Test
  public void bindingStringToDoubleAdapterWithPrecisionWorks() throws Exception {
    BindingsManager bindings = new BindingsManager(BatchInvoker.INVOKE_IMMEDIATELY_STRATEGY);
    StringProperty doubleString = new StringValueProperty("0");
    DoubleProperty doubleValue = new DoubleValueProperty(0.1234);

    bindings.bindTwoWay(new StringToDoubleAdapterProperty(doubleString, 3), doubleValue);

    assertThat(doubleString.get()).isEqualTo("0.123");
  }
}