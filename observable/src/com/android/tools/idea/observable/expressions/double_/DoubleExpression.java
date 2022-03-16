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
package com.android.tools.idea.observable.expressions.double_;

import com.android.tools.idea.observable.ObservableValue;
import com.android.tools.idea.observable.core.ObservableBool;
import com.android.tools.idea.observable.core.ObservableDouble;
import com.android.tools.idea.observable.expressions.Expression;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for Double expressions, providing a default implementation for the {@link ObservableDouble} interface.
 */
public abstract class DoubleExpression extends Expression<Double> implements ObservableDouble {

  protected DoubleExpression(ObservableValue... values) {
    super(values);
  }

  @NotNull
  @Override
  public ObservableBool isEqualTo(@NotNull ObservableValue<Double> value) {
    return ComparisonExpression.isEqual(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isGreaterThan(@NotNull ObservableValue<Double> value) {
    return ComparisonExpression.isGreaterThan(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isGreaterThanEqualTo(@NotNull ObservableValue<Double> value) {
    return ComparisonExpression.isGreaterThanEqual(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isLessThan(@NotNull ObservableValue<Double> value) {
    return ComparisonExpression.isLessThan(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isLessThanEqualTo(@NotNull ObservableValue<Double> value) {
    return ComparisonExpression.isLessThanEqual(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isEqualTo(double value) {
    return ComparisonExpression.isEqual(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isGreaterThan(double value) {
    return ComparisonExpression.isGreaterThan(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isLessThan(double value) {
    return ComparisonExpression.isLessThan(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isGreaterThanEqualTo(double value) {
    return ComparisonExpression.isGreaterThanEqual(this, value);
  }

  @NotNull
  @Override
  public ObservableBool isLessThanEqualTo(double value) {
    return ComparisonExpression.isLessThanEqual(this, value);
  }
}
