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
package com.squareup.sample.poetry

import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.TabStopSpan
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.TextView.BufferType.SPANNABLE
import androidx.appcompat.widget.Toolbar
import com.squareup.sample.container.overviewdetail.OverviewDetailConfig
import com.squareup.sample.container.overviewdetail.OverviewDetailConfig.Detail
import com.squareup.sample.container.poetry.R
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.backPressedHandler
import com.squareup.workflow.ui.backstack.BackStackConfig
import com.squareup.workflow.ui.backstack.BackStackConfig.None

class StanzaLayoutRunner(private val view: View) : LayoutRunner<StanzaRendering> {
  private val tabSize = TypedValue
      .applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, view.resources.displayMetrics)
      .toInt()

  private val toolbar = view.findViewById<Toolbar>(R.id.stanza_toolbar)
      // Hack works around strange TransitionManager behavior until I figure it out properly.
      .apply { id = -1 }
  private val lines = view.findViewById<TextView>(R.id.stanza_lines)
  private val more = view.findViewById<TextView>(R.id.stanza_more)
  private val goBack = view.findViewById<TextView>(R.id.stanza_back)

  override fun showRendering(
    rendering: StanzaRendering,
    viewEnvironment: ViewEnvironment
  ) {
    if (viewEnvironment[OverviewDetailConfig] == Detail) {
      toolbar.title = "Stanza ${rendering.stanzaNumber}"
      toolbar.subtitle = null
    } else {
      toolbar.title = rendering.title
      toolbar.subtitle = "Stanza ${rendering.stanzaNumber}"
    }

    lines.setTabulatedText(rendering.lines)

    rendering.onGoForth
        ?.let {
          lines.setOnClickListener { it() }
          more.setOnClickListener { it() }
          more.visibility = View.VISIBLE
        }
        ?: run {
          lines.setOnClickListener(null)
          more.setOnClickListener(null)
          more.visibility = View.GONE
        }

    rendering.onGoBack
        ?.let {
          goBack.setOnClickListener { it() }
          goBack.visibility = View.VISIBLE
        }
        ?: run {
          goBack.setOnClickListener(null)
          goBack.visibility = View.INVISIBLE
        }

    if (viewEnvironment[OverviewDetailConfig] != Detail && viewEnvironment[BackStackConfig] != None) {
      toolbar.setNavigationOnClickListener { rendering.onGoUp.invoke() }
    } else {
      toolbar.navigationIcon = null
    }

    view.backPressedHandler = rendering.onGoBack
        ?: rendering.onGoUp.takeIf { viewEnvironment[OverviewDetailConfig] != Detail }
  }

  private fun TextView.setTabulatedText(lines: List<String>) {
    val spans = SpannableStringBuilder()

    lines.forEach {
      if (spans.isNotEmpty()) spans.append("\n")
      val span = SpannableStringBuilder(it).apply {
        for (i in 1..5) {
          setSpan(TabStopSpan.Standard(tabSize * 1), 0, length, SPAN_EXCLUSIVE_EXCLUSIVE)
        }
      }
      spans.append(span)
    }
    setText(spans, SPANNABLE)
  }

  companion object : ViewFactory<StanzaRendering> by LayoutRunner.bind(
      R.layout.stanza_layout,
      ::StanzaLayoutRunner
  )
}
