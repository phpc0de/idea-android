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

package com.google.samples.apps.iosched.shared.domain.users

import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

open class StarEventUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository
) : MediatorUseCase<StarEventParameter, StarUpdatedStatus>() {

    override fun execute(parameters: StarEventParameter) {
        val updateResult = try {
            repository.starEvent(parameters.userId, parameters.userEvent)
        } catch (e: Exception) {
            result.postValue(Result.Error(e))
            return
        }
        // Avoid duplicating sources and trigger an update on the LiveData from the base class.
        result.removeSource(updateResult)
        result.addSource(updateResult, {
            result.postValue(updateResult.value)
        })
    }
}

data class StarEventParameter(
    val userId: String,
    val userEvent: UserEvent
)

enum class StarUpdatedStatus {
    STARRED,
    UNSTARRED
}
