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
class MutateTest {
  @Test
  fun `test mutate success`() = runTest {
    Mutator().mutate { "1" }.also { result ->
      assertEquals("1", result)
    }
  }

  @Test
  fun `test mutate error`() = runTest {
    runCatching {
      Mutator().mutate { error("error") }
    }.also { result ->
      assertEquals("error", result.exceptionOrNull()!!.message)
    }
  }

  @Test
  fun `test mutate CancellationException in block`() = runTest {
    launch {
      Mutator().mutate { throw CancellationException() }
    }.also { job ->
      advanceUntilIdle()
      assertEquals(true, job.isCancelled)
      assertEquals(true, job.isCompleted)
    }
  }

  @Test
  fun `test mutate cancel in block`() = runTest {
    launch {
      Mutator().mutate { cancel() }
    }.also { job ->
      advanceUntilIdle()
      assertEquals(true, job.isCancelled)
      assertEquals(true, job.isCompleted)
    }
  }

  @Test
  fun `test cancelMutate when mutate in progress`() = runTest {
    val mutator = Mutator()

    val job = launch {
      mutator.mutate { delay(Long.MAX_VALUE) }
    }.also {
      runCurrent()
      assertEquals(true, mutator.isMutating())
    }

    mutator.cancelMutate()

    assertEquals(false, mutator.isMutating())
    assertEquals(true, job.isCancelled)
    assertEquals(true, job.isCompleted)
  }

  @Test
  fun `test mutate when mutate in progress`() = runTest {
    val mutator = Mutator()

    val job = launch {
      mutator.mutate { delay(Long.MAX_VALUE) }
    }.also {
      runCurrent()
      assertEquals(true, mutator.isMutating())
    }

    mutator.mutate { }

    assertEquals(false, mutator.isMutating())
    assertEquals(true, job.isCancelled)
    assertEquals(true, job.isCompleted)
  }

  @Test
  fun `test mutate when effect in progress`() = runTest {
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

    mutator.mutate { list.add("2") }
    assertEquals(false, job.isCancelled)
    assertEquals(true, job.isCompleted)
    assertEquals(listOf("1", "2"), list)
  }
}