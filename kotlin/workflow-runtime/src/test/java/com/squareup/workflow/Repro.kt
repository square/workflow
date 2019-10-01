package com.squareup.workflow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.junit.Test

class Repro {

  @Suppress("DEPRECATION")
  @UseExperimental(
      InternalCoroutinesApi::class,
      ObsoleteCoroutinesApi::class,
      ExperimentalCoroutinesApi::class
  )
  @Test fun asplode() {
    runBlocking {
      val channel = Channel<Unit>(RENDEZVOUS)
      channel.close()
      select<Unit> {
        channel.onReceiveOrClosed {
          println(it)
        }
      }
    }
  }
}