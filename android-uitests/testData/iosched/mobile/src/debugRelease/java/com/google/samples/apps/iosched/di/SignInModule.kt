/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.FirebaseAuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.FirestoreRegisteredUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.fcm.FcmTokenUpdater
import com.google.samples.apps.iosched.util.signin.DefaultSignInHandler
import com.google.samples.apps.iosched.util.signin.SignInHandler
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class SignInModule {
    @Provides
    fun provideSignInHandler(): SignInHandler = DefaultSignInHandler()

    @Singleton
    @Provides
    fun provideRegisteredUserDataSource(firestore: FirebaseFirestore): RegisteredUserDataSource {
        return FirestoreRegisteredUserDataSource(firestore)
    }

    @Singleton
    @Provides
    fun provideAuthStateUserDataSource(
        firebase: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthStateUserDataSource {
        return FirebaseAuthStateUserDataSource(
            firebase,
            FcmTokenUpdater(firestore)
        )
    }

    @Singleton
    @Provides
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
