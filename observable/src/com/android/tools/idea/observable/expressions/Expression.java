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
package com.android.tools.idea.observable.expressions;

import com.android.tools.idea.observable.AbstractObservableValue;
import com.android.tools.idea.observable.InvalidationListener;
import com.android.tools.idea.observable.ObservableValue;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * An expression is an observable value that wraps another observable value, modifying the result
 * before passing it on.
 * <p/>
 * Child class constructors should make sure they call {@code super} with all target observables,
 * as this will ensure invalidation notifications propagate correctly.
 */
public abstract class Expression<T> extends AbstractObservableValue<T> implements ObservableValue<T> {
  @SuppressWarnings("FieldCanBeLocal") // must be local to avoid weak garbage collection
  private final InvalidationListener myListener = () -> notifyInvalidated();

  protected Expression(@NotNull ObservableValue<?>... values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Can't create an expression without any target observables");
    }

    for (ObservableValue value : values) {
      value.addWeakListener(myListener);
    }
  }

  @NotNull
  public static <T> Expression<T> create(@NotNull Supplier<? extends T> valueSupplier, @NotNull ObservableValue<?>... values) {
    return new Expression<T>(values) {
      @Override
      @NotNull
      public T get() {
        return valueSupplier.get();
      }
    };
  }
}
