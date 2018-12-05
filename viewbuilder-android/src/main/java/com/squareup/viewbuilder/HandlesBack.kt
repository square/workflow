/*
 * Copyright 2018 Square Inc.
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
package com.squareup.viewbuilder

import android.view.View
import com.squareup.viewbuilder.HandlesBack.Helper.setConditionalBackHandler

/**
 * Implemented by objects that want the option to intercept back button taps.
 * Can be implemented by [View] subclasses, or can be attached to a stock view via
 * [Helper].
 *
 * When implemented by a container view, the [onBackPressed] methods or tags of their
 * subviews should be invoked first.
 *
 * The typical flow of back button handling starts in the [android.app.Activity.onBackPressed]
 * calling [onBackPressed] on its content view. Each view in turn delegates to its
 * child views to give them first say.
 */
interface HandlesBack {
  /**
   * Returns `true` if back event was handled, `false` if someone higher in
   * the chain should.
   */
  fun onBackPressed(): Boolean

  object Helper {

    /**
     * Sets a [handler][HandlesBack] that always consumes the back event. Convenient for use
     * with method references.
     */
    @JvmStatic
    fun View.setBackHandler(handler: Runnable) {
      setBackHandler(handler::run)
    }

    /**
     * Sets a [handler][HandlesBack] that can return `true` if it has consumed the back
     * event, or `false` if processing should continue.
     */
    @JvmStatic
    fun View.setConditionalBackHandler(handlesBack: HandlesBack) {
      // Seems a lot more likely that this will be an accident than be done intentionally,
      // let's be strict.
      require(this !is HandlesBack) {
        "Cannot set back handlers on views that implement " +
            HandlesBack::class.simpleName
      }

      setTag(R.id.back_handler, handlesBack)
    }

    /**
     * To be called by a container when the back button is pressed. Calls the given
     * view's [HandlesBack.onBackPressed] method, or the handler registered on it
     * via [setConditionalBackHandler] or [setBackHandler].
     */
    @JvmStatic
    fun onBackPressed(view: View): Boolean {
      var handler: HandlesBack? = view.getTag(R.id.back_handler) as HandlesBack?

      if (view is HandlesBack) {
        check(handler == null) { "Should be impossible to have both tag and interface" }
        handler = view
      }

      return handler != null && handler.onBackPressed()
    }
  }
}

/**
 * Sets a [handler][HandlesBack] that always consumes the back event. Convenient for use
 * with method references.
 *
 * Outside of [HandlesBack.Helper] because when it's there, IntelliJ can't find it for auto-import
 * for some reason.
 */
fun View.setBackHandler(handler: () -> Unit) {
  setConditionalBackHandler(object : HandlesBack {
    override fun onBackPressed(): Boolean {
      handler()
      return true
    }
  })
}
