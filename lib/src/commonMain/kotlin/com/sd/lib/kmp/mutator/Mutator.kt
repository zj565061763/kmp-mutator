package com.sd.lib.kmp.mutator

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface Mutator {
  /** [mutate]是否正在执行修改中 */
  suspend fun isMutating(): Boolean

  /**
   * 互斥修改，如果协程A正在修改，则协程B调用此方法时会先取消协程A，然后再执行[block]，
   * [block]总是串行，不会并发
   */
  suspend fun <T> mutate(block: suspend MutateScope.() -> T): T

  /**
   * 尝试执行[block]，如果[mutate]的block正在执行则此方法会挂起直到它结束，
   * [block]总是串行，不会并发
   */
  suspend fun <T> withLock(block: suspend () -> T): T

  /** 取消正在执行的[mutate]修改 */
  suspend fun cancelMutate()

  interface MutateScope {
    /** 确保[mutate]的协程处于激活状态，否则抛出取消异常 */
    suspend fun ensureMutateActive()
  }
}

fun Mutator(): Mutator = MutatorImpl()

private class MutatorImpl : Mutator {
  private var _job: Job? = null
  private val _jobMutex = Mutex()
  private val _mutateMutex = Mutex()

  override suspend fun isMutating(): Boolean {
    return _jobMutex.withLock {
      _job?.isActive == true
    }
  }

  override suspend fun <R> mutate(block: suspend Mutator.MutateScope.() -> R): R {
    checkNestedMutate()
    return coroutineScope {
      val mutateContext = coroutineContext
      val mutateJob = checkNotNull(mutateContext[Job])

      _jobMutex.withLock {
        _job?.cancelAndJoin()
        _job = mutateJob
      }

      try {
        withLock {
          with(newMutateScope(mutateContext)) { block() }
        }
      } finally {
        releaseJob(mutateJob)
      }
    }
  }

  override suspend fun <T> withLock(block: suspend () -> T): T {
    checkNestedMutate()
    return _mutateMutex.withLock {
      withContext(MutateElement(tag = this@MutatorImpl)) {
        block()
      }
    }
  }

  override suspend fun cancelMutate() {
    _jobMutex.withLock {
      _job?.cancelAndJoin()
    }
  }

  /** 释放Job，不一定会成功，因为当前协程可能已经被取消 */
  private suspend fun releaseJob(job: Job) {
    _jobMutex.withLock {
      if (_job === job) _job = null
    }
  }

  private fun newMutateScope(mutateContext: CoroutineContext): Mutator.MutateScope {
    return object : Mutator.MutateScope {
      override suspend fun ensureMutateActive() {
        currentCoroutineContext().ensureActive()
        mutateContext.ensureActive()
      }
    }
  }

  private suspend fun checkNestedMutate() {
    if (currentCoroutineContext()[MutateElement]?.tag === this@MutatorImpl) {
      error("Nested mutate")
    }
  }

  private class MutateElement(val tag: Mutator) : AbstractCoroutineContextElement(MutateElement) {
    companion object Key : CoroutineContext.Key<MutateElement>
  }
}