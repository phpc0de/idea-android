/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.editors.literals.internal

import com.android.tools.idea.editors.literals.LiveLiteralsMonitorHandler
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.testing.AndroidProjectRule
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.openapi.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread
import kotlin.random.Random

internal class LiveLiteralsDeploymentReportServiceTest {
  @get:Rule
  val projectRule = AndroidProjectRule.inMemory()
  private val project: Project
    get() = projectRule.project
  private lateinit var service: LiveLiteralsDeploymentReportService

  private fun Collection<Pair<String, LiveLiteralsMonitorHandler.Problem>>.asString() =
    map { "[${it.first}] ${it.second.severity}: ${it.second.content}" }
      .sorted()
      .joinToString("\n")

  @Before
  fun setup() {
    StudioFlags.COMPOSE_LIVE_LITERALS.override(true)
    service = LiveLiteralsDeploymentReportService.getInstanceForTesting(project, MoreExecutors.directExecutor())
  }

  @Test
  fun `check deploy calls notify listener`() {
    var deployments = 0
    service.subscribe(projectRule.fixture.testRootDisposable, object: LiveLiteralsDeploymentReportService.Listener {
      override fun onMonitorStarted(deviceId: String) {}
      override fun onMonitorStopped(deviceId: String) {}

      override fun onLiveLiteralsPushed(deviceId: String) {
        deployments++
      }
    })

    assertFalse(service.hasProblems)
    assertFalse(service.hasActiveDevices)
    service.liveLiteralsMonitorStarted("DeviceA", LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    assertTrue(service.hasActiveDevices)
    assertEquals(0, deployments)

    // Push to a different device should not trigger a deployment
    service.liveLiteralPushed("DeviceB", "0")
    assertTrue(service.hasActiveDevices)
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW))
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW, LiveLiteralsMonitorHandler.DeviceType.EMULATOR))
    assertFalse(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.EMULATOR))
    assertEquals(0, deployments)

    service.liveLiteralPushed("DeviceA", "0")
    assertTrue(service.hasActiveDevices)
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW))
    assertEquals(1, deployments)
  }

  @Test
  fun `check problems are recorded`() {
    assertFalse(service.hasProblems)
    assertFalse(service.hasActiveDevices)
    service.liveLiteralsMonitorStarted("DeviceA", LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    // Finish the deployment successfully
    service.liveLiteralPushed("DeviceA", "0")
    assertFalse(service.hasProblems)

    service.liveLiteralsMonitorStarted("DeviceA", LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    service.liveLiteralPushed("DeviceA", "0", listOf(
      LiveLiteralsMonitorHandler.Problem.info("Info"),
      LiveLiteralsMonitorHandler.Problem.warn("Warn"),
    ))
    assertTrue(service.hasProblems)

    assertEquals("""
      [DeviceA] INFO: Info
      [DeviceA] WARNING: Warn
    """.trimIndent(), service.problems.asString())

    service.liveLiteralsMonitorStarted("DeviceA", LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    assertTrue(service.hasProblems)
    service.liveLiteralPushed("DeviceA", "0")
    assertTrue(service.problems.isEmpty())
  }

  @Test
  fun `check multiple devices`() {
    var deployments = 0
    service.subscribe(projectRule.fixture.testRootDisposable, object: LiveLiteralsDeploymentReportService.Listener {
      override fun onMonitorStarted(deviceId: String) {}
      override fun onMonitorStopped(deviceId: String) {}

      override fun onLiveLiteralsPushed(deviceId: String) {
        deployments++
      }
    })
    service.liveLiteralPushed("DeviceB", "0")
    assertEquals("The device was not active, no deployments expected", 0, deployments)

    service.liveLiteralsMonitorStarted("DeviceA", LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW))
    assertFalse(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.EMULATOR))
    assertFalse(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PHYSICAL))
    assertFalse(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PHYSICAL, LiveLiteralsMonitorHandler.DeviceType.EMULATOR))

    service.liveLiteralsMonitorStarted("DeviceB", LiveLiteralsMonitorHandler.DeviceType.EMULATOR)
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW, LiveLiteralsMonitorHandler.DeviceType.EMULATOR))
    assertTrue(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PREVIEW,
                                             LiveLiteralsMonitorHandler.DeviceType.EMULATOR,
                                             LiveLiteralsMonitorHandler.DeviceType.PHYSICAL))
    assertFalse(service.hasActiveDeviceOfType(LiveLiteralsMonitorHandler.DeviceType.PHYSICAL))
    service.liveLiteralPushed("DeviceB", "0")
    assertTrue(service.hasActiveDevices)
    assertEquals(1, deployments)
    service.liveLiteralPushed("DeviceC", "0") // Device is not active, will be ignored
    assertEquals(1, deployments)
    service.liveLiteralPushed("DeviceA", "0")
    assertEquals(2, deployments)

    service.liveLiteralPushed("DeviceB", "0", listOf(LiveLiteralsMonitorHandler.Problem.info("Test")))
    assertTrue(service.hasProblems)
    assertEquals(3, deployments)
    service.liveLiteralsMonitorStopped("DeviceB")
    assertFalse(service.hasProblems)

    // Device is not active, this should not be recorded
    service.liveLiteralPushed("DeviceB", "0", listOf(LiveLiteralsMonitorHandler.Problem.info("Test")))
    assertFalse(service.hasProblems)
    service.liveLiteralPushed("DeviceA", "0", listOf(LiveLiteralsMonitorHandler.Problem.info("Test")))
    assertTrue(service.hasProblems)

    service.liveLiteralsMonitorStarted("DeviceC", LiveLiteralsMonitorHandler.DeviceType.PHYSICAL)
  }

  private fun runWithSynchronizedStartStop(iterations: Int, blocks: List<() -> Unit>) {
    val entryBarrier = CyclicBarrier(blocks.size)
    blocks.map {
      thread {
        entryBarrier.await()
        repeat(iterations) {
          it()
        }
      }
    }.forEach { it.join() }
  }

  /**
   * Regression test for b/180999880 that simulates the contention on the reporting service to reproduce
   * any concurrency issues.
   */
  @Test
  fun `check active devices synchronization`() {
    var hasStarted = true
    service.subscribe(projectRule.fixture.testRootDisposable, object: LiveLiteralsDeploymentReportService.Listener {
      override fun onMonitorStarted(deviceId: String) {
        hasStarted = true
      }

      override fun onMonitorStopped(deviceId: String) {}
      override fun onLiveLiteralsPushed(deviceId: String) {}
    })

    // Generate 1000 devices
    val devices = (0..1000).map {
      "device$it"
    }.toList()

    // Start all devices
    devices.forEach {
      service.liveLiteralsMonitorStarted(it, LiveLiteralsMonitorHandler.DeviceType.PREVIEW)
    }

    // Run in three different threads starts/stop and stop
    runWithSynchronizedStartStop(
      10000,
      listOf(
        { service.liveLiteralsMonitorStarted(devices.random(), LiveLiteralsMonitorHandler.DeviceType.PREVIEW) },
        { service.liveLiteralsMonitorStopped(devices.random()) },
        { service.stopAllMonitors() }
      )
    )

    // Ensure that the threads have executed
    assertTrue(hasStarted)
  }
}