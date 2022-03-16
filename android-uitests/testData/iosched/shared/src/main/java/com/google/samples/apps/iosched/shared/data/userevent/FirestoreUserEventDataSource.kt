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

package com.google.samples.apps.iosched.shared.data.userevent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CancelAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.SwapAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestAction
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The data source for user data stored in firestore. It observes user data and also updates
 * stars and reservations.
 */
class FirestoreUserEventDataSource @Inject constructor(
    val firestore: FirebaseFirestore
) : UserEventDataSource {

    companion object {
        /**
         * Firestore constants.
         */
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        private const val QUEUE_COLLECTION = "queue"
        internal const val ID = "id"
        internal const val START_TIME = "startTime"
        internal const val END_TIME = "endTime"
        internal const val IS_STARRED = "isStarred"

        internal const val RESERVATION_REQUEST_KEY = "reservationRequest"

        private const val RESERVE_REQ_ACTION = "RESERVE_REQUESTED"
        private const val RESERVE_CANCEL_ACTION = "CANCEL_REQUESTED"

        internal const val RESERVATION_REQUEST_ACTION_KEY = "action"
        internal const val RESERVATION_REQUEST_REQUEST_ID_KEY = "requestId"
        private const val RESERVATION_REQUEST_TIMESTAMP_KEY = "timestamp"

        private const val REQUEST_QUEUE_ACTION_KEY = "action"
        private const val REQUEST_QUEUE_SESSION_KEY = "sessionId"
        private const val REQUEST_QUEUE_REQUEST_ID_KEY = "requestId"
        private const val REQUEST_QUEUE_ACTION_RESERVE = "RESERVE"
        private const val REQUEST_QUEUE_ACTION_CANCEL = "CANCEL"
        private const val REQUEST_QUEUE_ACTION_SWAP = "SWAP"
        private const val SWAP_QUEUE_RESERVE_SESSION_ID_KEY = "reserveSessionId"
        private const val SWAP_QUEUE_CANCEL_SESSION_ID_KEY = "cancelSessionId"

        internal const val RESERVATION_RESULT_KEY = "reservationResult"
        internal const val RESERVATION_RESULT_TIME_KEY = "timestamp"
        internal const val RESERVATION_RESULT_RESULT_KEY = "requestResult"
        internal const val RESERVATION_RESULT_REQ_ID_KEY = "requestId"

        internal const val RESERVATION_STATUS_KEY = "reservationStatus"
    }

    // Null if the listener is not yet added
    private var eventsChangedListenerSubscription: ListenerRegistration? = null
    private var eventChangedListenerSubscription: ListenerRegistration? = null

    // Observable events
    private val resultEvents = MutableLiveData<UserEventsResult>()
    private val resultSingleEvent = MutableLiveData<UserEventResult>()

    /**
     * Asynchronous method to get the user events.
     *
     * This method generates important messages to the user if a reservation is confirmed or
     * waitlisted.
     */
    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        if (userId.isEmpty()) {
            resultEvents.postValue(UserEventsResult(emptyList()))
            return resultEvents
        }

        registerListenerForEvents(resultEvents, userId)
        return resultEvents
    }

    override fun getObservableUserEvent(
        userId: String,
        eventId: SessionId
    ): LiveData<UserEventResult> {
        if (userId.isEmpty()) {
            resultSingleEvent.postValue(UserEventResult(userEvent = null))
            return resultSingleEvent
        }
        registerListenerForSingleEvent(eventId, userId)
        return resultSingleEvent
    }

    override fun getUserEvents(userId: String): List<UserEvent> {
        if (userId.isEmpty()) {
            return emptyList()
        }

        val task = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS)
        return snapshot.documents.map { parseUserEvent(it) }
    }

    private fun registerListenerForEvents(
        result: MutableLiveData<UserEventsResult>,
        userId: String
    ) {
        val eventsListener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit =
            listener@{ snapshot, _ ->
                snapshot ?: return@listener

                DefaultScheduler.execute {
                    Timber.d("Events changes detected: ${snapshot.documentChanges.size}")

                    // Generate important user messages, like new reservations, if any.
                    val userMessage = generateReservationChangeMsg(snapshot, result.value)
                    val userEventsResult = UserEventsResult(
                        userEvents = snapshot.documents.map { parseUserEvent(it) },
                        userEventsMessage = userMessage
                    )
                    result.postValue(userEventsResult)
                }
            }

        val eventsCollection = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)

        eventsChangedListenerSubscription?.remove() // Remove in case userId changes.
        // Set a value in case there are no changes to the data on start
        // This needs to be set to avoid that the upper layer LiveData detects the old data as a
        // new data.
        // When addSource was called in DefaultSessionAndUserEventRepository#getObservableUserEvents,
        // the old data was considered as a new data even though it's for another user's data
        result.value = null
        eventsChangedListenerSubscription = eventsCollection.addSnapshotListener(eventsListener)
    }

    private fun registerListenerForSingleEvent(
        sessionId: SessionId,
        userId: String
    ) {
        val result = resultSingleEvent

        val singleEventListener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit =
            listener@{ snapshot, _ ->
                snapshot ?: return@listener

                DefaultScheduler.execute {
                    Timber.d("Event changes detected on session: $sessionId")

                    // If oldValue doesn't exist, it's the first run so don't generate messages.
                    val userMessage = result.value?.userEvent?.let { oldValue: UserEvent ->

                        // Generate message if the reservation changed
                        if (snapshot.exists()) {
                            getUserMessageFromChange(oldValue, snapshot, sessionId)
                        } else {
                            null
                        }
                    }

                    val userEvent = if (snapshot.exists()) {
                        parseUserEvent(snapshot)
                    } else {
                        null
                    }

                    val userEventResult = UserEventResult(
                        userEvent = userEvent,
                        userEventMessage = userMessage
                    )
                    result.postValue(userEventResult)
                }
            }

        val eventDocument = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(sessionId)

        eventChangedListenerSubscription?.remove() // Remove in case userId changes.
        resultSingleEvent.value = null
        eventChangedListenerSubscription = eventDocument.addSnapshotListener(singleEventListener)
    }

    override fun clearSingleEventSubscriptions() {
        Timber.d("Firestore Event data source: Clearing subscriptions")
        resultSingleEvent.value = null
        eventChangedListenerSubscription?.remove() // Remove to avoid leaks
    }

    /** Firestore writes **/

    /**
     * Stars or unstars an event.
     *
     * @returns a result via a LiveData.
     */
    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        val result = MutableLiveData<Result<StarUpdatedStatus>>()

        val data = mapOf(
            ID to userEvent.id,
            IS_STARRED to userEvent.isStarred
        )

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(userEvent.id).set(data, SetOptions.merge()).addOnCompleteListener({
                if (it.isSuccessful) {
                    result.postValue(
                        Result.Success(
                            if (userEvent.isStarred) StarUpdatedStatus.STARRED
                            else StarUpdatedStatus.UNSTARRED
                        )
                    )
                } else {
                    result.postValue(
                        Result.Error(
                            it.exception ?: RuntimeException("Error updating star.")
                        )
                    )
                }
            })
        return result
    }

    /**
     * Requests a reservation for an event.
     *
     * This method makes two write operations at once.
     *
     * @return a LiveData indicating whether the request was successful (not whether the event
     * was reserved)
     */
    override fun requestReservation(
        userId: String,
        session: Session,
        action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {

        val result = MutableLiveData<Result<ReservationRequestAction>>()

        val logCancelOrReservation = if (action is CancelAction) "Cancel" else "Request"

        Timber.d("Requesting $logCancelOrReservation for session ${session.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        val newRandomRequestId = UUID.randomUUID().toString()

        // Write #1: Mark this session as reserved. This is for clients to track.
        val userSession = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(session.id)

        val reservationRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to getReservationRequestedEventAction(action),
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to FieldValue.serverTimestamp()
        )

        val userSessionData = mapOf(
            ID to session.id,
            RESERVATION_REQUEST_KEY to reservationRequest
        )

        batch.set(userSession, userSessionData, SetOptions.merge())

        // Write #2: Send a request to the server. The result will appear in the UserSession. A
        // success in this reservation only means that the request was accepted. Even offline, this
        // request will succeed.
        val newRequest = firestore
            .collection(QUEUE_COLLECTION)
            .document(userId)

        val queueReservationRequest = mapOf(
            REQUEST_QUEUE_ACTION_KEY to getReservationRequestedQueueAction(action),
            REQUEST_QUEUE_SESSION_KEY to session.id,
            REQUEST_QUEUE_REQUEST_ID_KEY to newRandomRequestId
        )

        batch.set(newRequest, queueReservationRequest)

        // Commit write batch

        batch.commit().addOnSuccessListener {
            Timber.d("$logCancelOrReservation request for session ${session.id} succeeded")
            result.postValue(Result.Success(action))
        }.addOnFailureListener {
            Timber.e(it, "$logCancelOrReservation request for session ${session.id} failed")
            result.postValue(Result.Error(it))
        }

        return result
    }

    override fun swapReservation(
        userId: String,
        fromSession: Session,
        toSession: Session
    ): LiveData<Result<SwapRequestAction>> {
        val result = MutableLiveData<Result<SwapRequestAction>>()

        Timber.d("Swapping reservations from: ${fromSession.id} to: ${toSession.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        val newRandomRequestId = UUID.randomUUID().toString()
        val serverTimestamp = FieldValue.serverTimestamp()

        // Write #1: Mark the toSession as reserved. This is for clients to track.
        val toUserSession = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(toSession.id)
        val toSwapRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to RESERVE_REQ_ACTION,
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to serverTimestamp
        )
        val userSessionData = mapOf(
            ID to toSession.id,
            RESERVATION_REQUEST_KEY to toSwapRequest
        )
        batch.set(toUserSession, userSessionData, SetOptions.merge())

        // Write #2: Mark the fromSession as canceled. This is for clients to track.
        val fromUserSession = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(fromSession.id)
        val fromSwapRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to RESERVE_CANCEL_ACTION,
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to serverTimestamp
        )
        val fromUserSessionData = mapOf(
            ID to fromSession.id,
            RESERVATION_REQUEST_KEY to fromSwapRequest
        )
        batch.set(fromUserSession, fromUserSessionData, SetOptions.merge())

        // Write #3: Send a request to the server. The result will appear in the both UserSessions
        // (from and to). success in this reservation only means that the request was accepted.
        // Even offline, this request will succeed.
        val newRequest = firestore
            .collection(QUEUE_COLLECTION)
            .document(userId)

        val queueSwapRequest = mapOf(
            REQUEST_QUEUE_ACTION_KEY to REQUEST_QUEUE_ACTION_SWAP,
            SWAP_QUEUE_RESERVE_SESSION_ID_KEY to toSession.id,
            SWAP_QUEUE_CANCEL_SESSION_ID_KEY to fromSession.id,
            REQUEST_QUEUE_REQUEST_ID_KEY to newRandomRequestId
        )

        batch.set(newRequest, queueSwapRequest)

        // Commit write batch
        batch.commit().addOnSuccessListener {
            Timber.d(
                "Queueing the swap request from: ${fromSession.id} to: ${toSession.id} succeeded"
            )
            result.postValue(Result.Success(SwapRequestAction()))
        }.addOnFailureListener {
            Timber.d("Queueing the swap request from: ${fromSession.id} to: ${toSession.id} failed")
            result.postValue(Result.Error(it))
        }

        return result
    }

    private fun getReservationRequestedEventAction(action: ReservationRequestAction): String =
        when (action) {
            is RequestAction -> RESERVE_REQ_ACTION
            is CancelAction -> RESERVE_CANCEL_ACTION
            // This should not happen because there is a dedicated method for the swap request
            is SwapAction -> throw IllegalStateException()
        }

    private fun getReservationRequestedQueueAction(action: ReservationRequestAction) =
        when (action) {
            is RequestAction -> REQUEST_QUEUE_ACTION_RESERVE
            is CancelAction -> REQUEST_QUEUE_ACTION_CANCEL
            // This should not happen because there is a dedicated method for the swap request
            is SwapAction -> throw IllegalStateException()
        }
}
