package com.sd.lib.kmp.mutator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WithLockTest {
  @Test
  fun `test withLock success`() = runTest {
    Mutator().effect { "1" }.also { result ->
      assertEquals("1", result)
    }
  }

  @Test
  fun `test withLock error`() = runTest {
    runCatching {
      Mutator().effect { error("error") }
    }.also { result ->
      assertEquals("error", result.exceptionOrNull()!!.message)
    }
  }

  @Test
  fun `test withLock CancellationException in block`() = runTest {
    launch {
      Mutator().effect { throw CancellationException() }
    }.also { job ->
      advanceUntilIdle()
      assertEquals(true, job.isCancelled)
      assertEquals(true, job.isCompleted)
    }
  }

  @Test
  fun `test mutate cancel in block`() = runTest {
    launch {
      Mutator().effect { cancel() }
    }.also { job ->
      advanceUntilIdle()
      assertEquals(true, job.isCancelled)
      assertEquals(true, job.isCompleted)
    }
  }

  @Test
  fun `test cancelMutate when withLock in progress`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    val job = launch {
      mutator.effect {
        delay(5_000)
        list.add("1")
      }
    }.also {
      runCurrent()
    }

    mutator.cancelMutate()
    assertEquals(false, job.isCancelled)
    assertEquals(false, job.isCompleted)

    advanceUntilIdle()
    assertEquals(false, job.isCancelled)
    assertEquals(true, job.isCompleted)
    assertEquals(listOf("1"), list)
  }

  @Test
  fun `test withLock when withLock in progress`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    val job = launch {
      mutator.effect {
        delay(5_000)
        list.add("1")
      }
    }.also {
      runCurrent()
    }

    mutator.effect { list.add("2") }
    assertEquals(false, job.isCancelled)
    assertEquals(true, job.isCompleted)
    assertEquals(listOf("1", "2"), list)
  }

  @Test
  fun `test withLock when mutate in progress`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    val job = launch {
      mutator.mutate {
        delay(5_000)
        list.add("1")
      }
    }.also {
      runCurrent()
    }

    mutator.effect { list.add("2") }
    assertEquals(false, job.isCancelled)
    assertEquals(true, job.isCompleted)
    assertEquals(listOf("1", "2"), list)
  }
}