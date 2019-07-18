/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.workflow.rx2

import com.squareup.workflow.testing.test
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import io.reactivex.BackpressureStrategy.MISSING
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.MaybeSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject

class RxWorkersTest : AnnotationSpec() {

  private class ExpectedException : RuntimeException()

  // region Observable

  @Test fun `observable emits`() {
    val subject = PublishSubject.create<String>()
    // Should support out-projected parameters.
    val worker = (subject as Observable<out String?>).asWorker()

    worker.test {
      subject.onNext("foo")
      nextOutput() shouldBe "foo"

      subject.onNext("bar")
      nextOutput() shouldBe "bar"
    }
  }

  @Test fun `observable finishes`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `observable finishes after emitting`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onNext("foo")
      nextOutput() shouldBe "foo"

      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `observable throws`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onError(ExpectedException())
      getException() should beInstanceOf<ExpectedException>()
    }
  }

  @Test fun `observable is subscribed lazily`() {
    var subscriptions = 0
    val subject = PublishSubject.create<String>()
    val worker = subject.doOnSubscribe { subscriptions++ }
        .asWorker()

    subscriptions shouldBe 0

    worker.test {
      subscriptions shouldBe 1
    }
  }

  @Test fun `observable is disposed when worker cancelled`() {
    var disposals = 0
    val subject = PublishSubject.create<String>()
    val worker = subject.doOnDispose { disposals++ }
        .asWorker()

    disposals shouldBe 0

    worker.test {
      disposals shouldBe 0
      cancelWorker()
      disposals shouldBe 1
    }
  }

  // endregion

  // region Flowable

  @Test fun `flowable emits`() {
    val subject = PublishSubject.create<String>()
    val worker = (subject.toFlowable(MISSING) as Flowable<out String?>)
        .asWorker()

    worker.test {
      subject.onNext("foo")
      nextOutput() shouldBe "foo"

      subject.onNext("bar")
      nextOutput() shouldBe "bar"
    }
  }

  @Test fun `flowable finishes`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.toFlowable(MISSING)
        .asWorker()

    worker.test {
      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `flowable finishes after emitting`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.toFlowable(MISSING)
        .asWorker()

    worker.test {
      subject.onNext("foo")
      nextOutput() shouldBe "foo"

      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `flowable throws`() {
    val subject = PublishSubject.create<String>()
    val worker = subject.toFlowable(MISSING)
        .asWorker()

    worker.test {
      subject.onError(ExpectedException())
      getException() should beInstanceOf<ExpectedException>()
    }
  }

  @Test fun `flowable is subscribed lazily`() {
    var subscriptions = 0
    val subject = PublishSubject.create<String>()
    val worker = subject.toFlowable(MISSING)
        .doOnSubscribe { subscriptions++ }
        .asWorker()

    subscriptions shouldBe 0

    worker.test {
      subscriptions shouldBe 1
    }
  }

  @Test fun `flowable is cancelled when worker cancelled`() {
    var cancels = 0
    val subject = PublishSubject.create<String>()
    val worker = subject.toFlowable(MISSING)
        .doOnCancel { cancels++ }
        .asWorker()

    cancels shouldBe 0

    worker.test {
      cancels shouldBe 0
      cancelWorker()
      cancels shouldBe 1
    }
  }

  // endregion

  // region Maybe

  @Test fun `maybe emits`() {
    val subject = MaybeSubject.create<String>()
    val worker = (subject as Maybe<out String?>).asWorker()

    worker.test {
      subject.onSuccess("foo")
      nextOutput() shouldBe "foo"
      assertFinished()
    }
  }

  @Test fun `maybe finishes without emitting`() {
    val subject = MaybeSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `maybe throws`() {
    val subject = MaybeSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onError(ExpectedException())
      getException() should beInstanceOf<ExpectedException>()
    }
  }

  @Test fun `maybe is subscribed lazily`() {
    var subscriptions = 0
    val subject = MaybeSubject.create<String>()
    val worker = subject.doOnSubscribe { subscriptions++ }
        .asWorker()

    subscriptions shouldBe 0

    worker.test {
      subscriptions shouldBe 1
    }
  }

  @Test fun `maybe is disposed when worker cancelled`() {
    var cancels = 0
    val subject = MaybeSubject.create<String>()
    val worker = subject.doOnDispose { cancels++ }
        .asWorker()

    cancels shouldBe 0

    worker.test {
      cancels shouldBe 0
      cancelWorker()
      cancels shouldBe 1
    }
  }

  // endregion

  // region Single

  @Test fun `single emits`() {
    val subject = SingleSubject.create<String>()
    val worker = (subject as Single<out String?>).asWorker()

    worker.test {
      subject.onSuccess("foo")
      nextOutput() shouldBe "foo"
      assertFinished()
    }
  }

  @Test fun `single throws`() {
    val subject = SingleSubject.create<String>()
    val worker = subject.asWorker()

    worker.test {
      subject.onError(ExpectedException())
      getException() should beInstanceOf<ExpectedException>()
    }
  }

  @Test fun `single is subscribed lazily`() {
    var subscriptions = 0
    val subject = SingleSubject.create<String>()
    val worker = subject.doOnSubscribe { subscriptions++ }
        .asWorker()

    subscriptions shouldBe 0

    worker.test {
      subscriptions shouldBe 1
    }
  }

  @Test fun `single is disposed when worker cancelled`() {
    var cancels = 0
    val subject = SingleSubject.create<String>()
    val worker = subject.doOnDispose { cancels++ }
        .asWorker()

    cancels shouldBe 0

    worker.test {
      cancels shouldBe 0
      cancelWorker()
      cancels shouldBe 1
    }
  }

  // endregion

  // region Completable

  @Test fun `completable emits`() {
    val subject = CompletableSubject.create()
    val worker = subject.asWorker("")

    worker.test {
      subject.onComplete()
      assertFinished()
    }
  }

  @Test fun `completable throws`() {
    val subject = CompletableSubject.create()
    val worker = subject.asWorker("")

    worker.test {
      subject.onError(ExpectedException())
      getException() should beInstanceOf<ExpectedException>()
    }
  }

  @Test fun `completable is subscribed lazily`() {
    var subscriptions = 0
    val subject = CompletableSubject.create()
    val worker = subject.doOnSubscribe { subscriptions++ }
        .asWorker("")

    subscriptions shouldBe 0

    worker.test {
      subscriptions shouldBe 1
    }
  }

  @Test fun `completable is disposed when worker cancelled`() {
    var cancels = 0
    val subject = CompletableSubject.create()
    val worker = subject.doOnDispose { cancels++ }
        .asWorker("")

    cancels shouldBe 0

    worker.test {
      cancels shouldBe 0
      cancelWorker()
      cancels shouldBe 1
    }
  }

  // endregion
}
