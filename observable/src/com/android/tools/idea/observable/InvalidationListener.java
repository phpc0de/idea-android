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
package com.android.tools.idea.observable;

/**
 * Listener that is notified any time an observable value may have changed (either directly or
 * because a value it is bound to has become invalidated).
 *
 * Note that an {@link #onInvalidated()} event does not include the underlying value that changed;
 * it just notifies listeners that a value *has* changed. If you want to register a listener that
 * receives a value, consider using {@link ListenerManager#listen(ObservableValue, Receiver)}
 * instead.
 */
public interface InvalidationListener {
  void onInvalidated();
}
