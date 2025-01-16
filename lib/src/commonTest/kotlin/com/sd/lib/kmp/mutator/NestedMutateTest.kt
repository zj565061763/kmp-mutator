package com.sd.lib.kmp.mutator

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedMutateTest {
  @Test
  fun `test mutate in mutate block`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    mutator.mutate {
      runCatching {
        mutator.mutate { }
      }.also {
        assertEquals("Nested mutate", it.exceptionOrNull()!!.message)
        list.add("1")
      }
      list.add("2")
    }

    assertEquals(listOf("1", "2"), list)
  }

  @Test
  fun `test effect in effect block`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    mutator.effect {
      runCatching {
        mutator.effect { }
      }.also {
        assertEquals("Nested mutate", it.exceptionOrNull()!!.message)
        list.add("1")
      }
      list.add("2")
    }

    assertEquals(listOf("1", "2"), list)
  }

  @Test
  fun `test mutate in effect block`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    mutator.effect {
      runCatching {
        mutator.mutate { }
      }.also {
        assertEquals("Nested mutate", it.exceptionOrNull()!!.message)
        list.add("1")
      }
      list.add("2")
    }

    assertEquals(listOf("1", "2"), list)
  }

  @Test
  fun `test effect in mutate block`() = runTest {
    val mutator = Mutator()
    val list = mutableListOf<String>()

    mutator.mutate {
      runCatching {
        mutator.effect { }
      }.also {
        assertEquals("Nested mutate", it.exceptionOrNull()!!.message)
        list.add("1")
      }
      list.add("2")
    }

    assertEquals(listOf("1", "2"), list)
  }
}