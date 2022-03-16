/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.datastore.database

import com.android.tools.datastore.DataStoreDatabase
import com.android.tools.datastore.FakeLogService
import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.Energy
import com.android.tools.profiler.proto.EnergyProfiler
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class EnergyTableTest {

  private class TablePopulator {
    var numSamples = 0
      private set

    var samplePeriod = 0
      private set

    var numWakeLockGroups = 0
      private set

    var wakeLockPeriod = 0
      private set

    var wakeLockLength = 0
      private set

    fun setSampleValues(numSamples: Int, samplePeriod: Int): TablePopulator {
      this.numSamples = numSamples
      this.samplePeriod = samplePeriod

      return this
    }

    fun setWakeLockEventValues(numWakeLockGroups: Int, wakeLockPeriod: Int, wakeLockLength: Int): TablePopulator {
      this.numWakeLockGroups = numWakeLockGroups
      this.wakeLockPeriod = wakeLockPeriod
      this.wakeLockLength = wakeLockLength
      return this
    }

    fun populate(table: EnergyTable, session: Common.Session) {
      for (i in 0 until numSamples) {
        table.insertOrReplace(
          session, EnergyProfiler.EnergySample.newBuilder()
          .setEnergyUsage(Energy.EnergyUsageData.newBuilder()
                            .setCpuUsage(i * 100)
                            .setNetworkUsage((numSamples - i) * 100))
          .setTimestamp((samplePeriod * i).toLong())
          .build()
        )
      }

      for (i in 0 until numWakeLockGroups) {
        val timeAcquired = (wakeLockPeriod * i).toLong()
        val timeReleased = timeAcquired + wakeLockLength
        val eventId = (i + 1).toLong()
        table.insertOrReplace(
          session, Common.Event.newBuilder()
          .setGroupId(eventId)
          .setTimestamp(timeAcquired)
          .setEnergyEvent(Energy.EnergyEventData.newBuilder().setWakeLockAcquired(Energy.WakeLockAcquired.getDefaultInstance()))
          .build()
        )

        table.insertOrReplace(
          session, Common.Event.newBuilder()
          .setGroupId(eventId)
          .setTimestamp(timeReleased)
          .setIsEnded(true)
          .setEnergyEvent(Energy.EnergyEventData.newBuilder().setWakeLockReleased(Energy.WakeLockReleased.getDefaultInstance()))
          .build()
        )
      }

    }
  }

  private lateinit var dbFile: File
  private lateinit var table: EnergyTable
  private lateinit var database: DataStoreDatabase

  @Before
  fun setUp() {
    dbFile = File.createTempFile("EnergyTable", "mysql")
    database = DataStoreDatabase(dbFile.absolutePath, DataStoreDatabase.Characteristic.DURABLE, FakeLogService())
    table = EnergyTable()
    table.initialize(database.connection)
  }

  @After
  fun tearDown() {
    database.disconnect()
    dbFile.delete()
  }

  private fun newEnergyRequest(t0: Long, t1: Long, session: Common.Session = MAIN_SESSION) = EnergyProfiler.EnergyRequest.newBuilder()
    .setSession(session)
    .setStartTimestamp(t0)
    .setEndTimestamp(t1)
    .build()

  private fun newEnergyGroupRequest(eventId: Long, session: Common.Session = MAIN_SESSION) =
    EnergyProfiler.EnergyEventGroupRequest.newBuilder()
      .setSession(session)
      .setEventId(eventId)
      .build()

  @Test
  fun testGetSamples() {
    TablePopulator().setSampleValues(10, 200).populate(table, MAIN_SESSION)

    with(table.getSamples(newEnergyRequest(Long.MIN_VALUE, Long.MAX_VALUE))) {
      assertThat(this).hasSize(10)
    }

    with(table.getSamples(newEnergyRequest(250, 500))) {
      assertThat(this).hasSize(1)
    }

    with(table.getSamples(newEnergyRequest(500, 1001))) {
      assertThat(this).hasSize(3)
    }

    // t1 is exclusive
    with(table.getSamples(newEnergyRequest(500, 1000))) {
      assertThat(this).hasSize(2)
    }

    // t0 is inclusive
    with(table.getSamples(newEnergyRequest(400, 1001))) {
      assertThat(this).hasSize(4)
    }

    with(table.getSamples(newEnergyRequest(Long.MIN_VALUE, Long.MAX_VALUE, ANOTHER_SESSION))) {
      assertThat(this).isEmpty()
    }
  }

  @Test
  fun testGetEvents() {
    // Events: 0->500, 1000->1500, 2000->2500, ...
    TablePopulator().setWakeLockEventValues(10, 1000, 500).populate(table, MAIN_SESSION)

    with(table.getEvents(newEnergyRequest(Long.MIN_VALUE, Long.MAX_VALUE))) {
      assertThat(this).hasSize(10 * 2) // 2 events per event group
    }

    // t1 is exclusive
    with(table.getEvents(newEnergyRequest(800, 1000))) {
      assertThat(this).hasSize(0)
    }

    //     t0              t1
    //      |              | [-----]
    with(table.getEvents(newEnergyRequest(1600, 1800))) {
      assertThat(this).hasSize(0)
    }

    //     t0              t1
    //  [---|---]          |
    with(table.getEvents(newEnergyRequest(1250, 1750))) {
      assertThat(this).hasSize(2)
      assertThat(this[0].energyEvent.hasWakeLockAcquired()).isTrue()
      assertThat(this[1].energyEvent.hasWakeLockReleased()).isTrue()
    }

    //     t0              t1
    //      |   [------]   |
    with(table.getEvents(newEnergyRequest(750, 1750))) {
      assertThat(this).hasSize(2)
      assertThat(this[0].energyEvent.hasWakeLockAcquired()).isTrue()
      assertThat(this[1].energyEvent.hasWakeLockReleased()).isTrue()
    }

    //     t0              t1
    //      |          [---|---]
    with(table.getEvents(newEnergyRequest(750, 1250))) {
      assertThat(this).hasSize(1)
      assertThat(this[0].energyEvent.hasWakeLockAcquired()).isTrue()
    }

    //     t0              t1
    //  [---|--------------|---]
    with(table.getEvents(newEnergyRequest(1100, 1450))) {
      assertThat(this).hasSize(1)
      assertThat(this[0].energyEvent.hasWakeLockAcquired()).isTrue()
    }

    with(table.getEvents(newEnergyRequest(Long.MIN_VALUE, Long.MAX_VALUE, ANOTHER_SESSION))) {
      assertThat(this).isEmpty()
    }
  }

  @Test
  fun testGetEventsGroups() {
    // Events: 0->500, 1000->1500, 2000->2500, ...
    TablePopulator().setWakeLockEventValues(10, 1000, 500).populate(table, MAIN_SESSION)

    for (id in 1..10) {
      with(table.getEventGroup(newEnergyGroupRequest(id.toLong()))) {
        assertWithMessage("Event group with ID ${id} is not empty").that(this).hasSize(2) // 2 events per event group
        this.forEach { energyEvent -> assertThat(energyEvent.groupId).isEqualTo(id) }
      }

      with(table.getEventGroup(newEnergyGroupRequest(id.toLong(), ANOTHER_SESSION))) {
        assertThat(this).isEmpty()
      }
    }
  }

  companion object {
    private val MAIN_SESSION = Common.Session.newBuilder().setSessionId(1L).setStreamId(1234).build()
    private val ANOTHER_SESSION = Common.Session.newBuilder().setSessionId(2L).setStreamId(4321).build()
  }
}
