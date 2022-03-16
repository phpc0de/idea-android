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

import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Sends a request to reserve or cancel a reservation for a session.
 */
open class ReservationActionUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository
) : MediatorUseCase<ReservationRequestParameters, ReservationRequestAction>() {

    override fun execute(parameters: ReservationRequestParameters) {

        DefaultScheduler.execute {
            try {
                val (userId, sessionId, action) = parameters
                val updateResult = repository.changeReservation(userId, sessionId, action)

                result.removeSource(updateResult)
                result.addSource(updateResult, {
                    result.postValue(updateResult.value)
                })
            } catch (e: Exception) {
                Timber.d("Exception changing reservation")
                result.postValue(Result.Error(e))
            }
        }
    }
}

data class ReservationRequestParameters(
    val userId: String,
    val sessionId: SessionId,
    val action: ReservationRequestAction
)

sealed class ReservationRequestAction {
    class RequestAction : ReservationRequestAction() {
        override fun equals(other: Any?): Boolean {
            return other is RequestAction
        }

        // This class isn't intended to be used as a key of a collection. Overriding this to remove
        // the lint warning
        @Suppress("redundant")
        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    class CancelAction : ReservationRequestAction() {
        override fun equals(other: Any?): Boolean {
            return other is CancelAction
        }

        // This class isn't intended to be used as a key of a collection. Overriding this to remove
        // the lint warning
        @Suppress("redundant")
        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    /**
     * The action when the user is trying to reserve a session, but there is already an overlapping
     * reservation.
     */
    class SwapAction(val parameters: SwapRequestParameters) : ReservationRequestAction()
}
