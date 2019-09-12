package com.squareup.workflow.internal

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class ThreadTest {

  @Test fun `independent threads`() {
    val group = ThreadGroup("group")
    val thread1 = Thread(group, {
      Thread.sleep(100)
      throw RuntimeException("Uncaught exception!")
    }, "thread1")
    val thread2 = Thread(group, {
      Thread.sleep(1000)
      println("thread2 finished")
    }, "thread2")

    thread2.start()
    thread1.start()

    group.list()

    group.joinAll()
  }

  @Test fun `supervised threads`() {
    val group = /*SupervisorThreadGroup("group") { t, e ->
      println("thread $t failed with $e, interrupting all children…")
      interrupt()

      if (e !is InterruptedException) parent.uncaughtException(t, e)
    }*/
      InterruptOnFailureThreadGroup()
    val thread1 = Thread(group, {
      Thread.sleep(100)
      throw RuntimeException("Uncaught exception!")
    }, "thread1")
    val thread2 = Thread(group, {
      Thread.sleep(1000)
      println("thread2 finished")
    }, "thread2")

    thread2.start()
    thread1.start()

    group.list()

    group.joinAll()
  }

  @UseExperimental(ExperimentalTime::class)
  @Test fun `time serially`() {
    measureTime {
      loadAllImagesSerially(List(5) { "$it" })
    }.let { println(it) }
  }

  @UseExperimental(ExperimentalTime::class)
  @Test fun `time concurrently`() {
    measureTime {
      loadAllImagesConcurrently(List(5) { "$it" })
    }.let { println(it) }
  }

  @UseExperimental(ExperimentalTime::class)
  @Test fun `time failure`() {
    measureTime {
      loadAllImagesConcurrently(List(5) { "$it" } + "fail")
    }.let { println(it) }
  }
}

fun loadAllImagesSerially(urls: List<String>): List<Image> {
  return urls.map { url ->
    loadImageFromNetwork(url)
  }
}

fun loadAllImagesConcurrently(urls: List<String>): List<Image> {
  val images = arrayOfNulls<Image>(urls.size)
  val threads = mutableListOf<Thread>()
  urls.forEachIndexed { i, url ->
    threads += thread {
      images[i] = loadImageFromNetwork(url)
    }
  }
  threads.forEach { it.join() }
  return images.requireNoNulls()
      .asList()
}

fun loadAllImagesFailurly(urls: List<String>): List<Image> {
  val images = arrayOfNulls<Image>(urls.size)
  val threads = mutableListOf<Thread>()
  val parent = InterruptOnFailureThreadGroup()
  urls.forEachIndexed { i, url ->
    threads += thread(threadGroup = parent) {
      images[i] = loadImageFromNetwork(url)
    }
  }
  threads.forEach { it.join() }
  return images.requireNoNulls()
      .asList()
}

fun loadAllImagesCancellably(urls: List<String>): List<Image> {
  val images = arrayOfNulls<Image>(urls.size)
  val threads = mutableListOf<Thread>()
  val parent = InterruptOnFailureThreadGroup()
  urls.forEachIndexed { i, url ->
    threads += thread(threadGroup = parent) {
      images[i] = loadImageFromNetwork(url)
    }
  }
  try {
    threads.forEach { it.join() }
  } catch (e: InterruptedException) {
    parent.interrupt()
  }
  return images.requireNoNulls()
      .asList()
}

fun loadAllImagesScoped(urls: List<String>): List<Image> {
  val images = arrayOfNulls<Image>(urls.size)
  threadScoped {
    urls.forEachIndexed { i, url ->
      thread {
        images[i] = loadImageFromNetwork(url)
      }
    }
  }
  return images.requireNoNulls()
      .asList()
}

suspend fun loadAllImagesCoroutines(urls: List<String>): List<Image> {
  val images = arrayOfNulls<Image>(urls.size)
  coroutineScope {
    urls.forEachIndexed { i, url ->
      launch {
        images[i] = loadImageFromNetwork(url)
      }
    }
  }
  return images.requireNoNulls()
      .asList()
}

suspend fun loadAllImagesDeferred(urls: List<String>): List<Image> {
  return coroutineScope {
    urls.map { async { loadImageFromNetwork(it) } }
        .map { it.await() }
  }
}

class ThreadScope {
  private val parent = InterruptOnFailureThreadGroup()
  private val threads = mutableListOf<Thread>()

  fun thread(block: () -> Unit) {
    threads += kotlin.concurrent.thread(threadGroup = parent, block = block)
  }

  fun join() {
    try {
      threads.forEach { it.join() }
    } finally {
      parent.interrupt()
    }
  }
}

inline fun threadScoped(block: ThreadScope.() -> Unit) {
  ThreadScope().apply {
    try {
      block()
    } finally {
      join()
    }
  }
}

private fun loadImageFromNetwork(url: String): Image {
  if (url == "fail") {
    Thread.sleep(300)
    throw IOException("image failed to load")
  }

  Thread.sleep(1000)
  println("loaded $url")
  return BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR)
}

private class InterruptOnFailureThreadGroup : ThreadGroup() {
  override fun uncaughtException(
    t: Thread,
    e: Throwable
  ) {
    if (e !is InterruptedException) {
      interrupt()
    }
  }
}

private fun ThreadGroup.joinAll() {
  fun attemptToJoinAll() {
    val threadCountEstimate = activeCount()
    val threads = arrayOfNulls<Thread>(threadCountEstimate)
    // If we miss a thread or two, it's fine, this is being called in a loop anyway.
    enumerate(threads)
    threads.forEach { it?.join() }
  }

  while (activeCount() > 0) {
    attemptToJoinAll()
  }
}
