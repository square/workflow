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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sample.container.masterdetail.MasterDetailConfig
import com.squareup.sample.container.masterdetail.MasterDetailConfig.Master
import com.squareup.sample.container.poetry.R
import com.squareup.sample.poetry.StanzasWorkflow.Rendering
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.LayoutRunner.Companion.bind
import com.squareup.workflow.ui.ViewBinding

class StanzasLayoutRunner(view: View) : LayoutRunner<Rendering> {
  private val toolbar = view.findViewById<Toolbar>(R.id.stanza_list_toolbar)
  private val recyclerView = view.findViewById<RecyclerView>(R.id.stanza_list)
      .apply { layoutManager = LinearLayoutManager(context) }

  private val adapter = Adapter()

  override fun showRendering(
    rendering: Rendering,
    containerHints: ContainerHints
  ) {
    adapter.rendering = rendering
    adapter.hints = containerHints
    adapter.notifyDataSetChanged()
    if (recyclerView.adapter == null) recyclerView.adapter = adapter
    toolbar.title = rendering.title
    toolbar.subtitle = rendering.subtitle

    if (rendering.selection >= 0) recyclerView.scrollToPosition(rendering.selection)
  }

  private class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)

  private class Adapter : RecyclerView.Adapter<ViewHolder>() {
    lateinit var rendering: Rendering
    lateinit var hints: ContainerHints

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): ViewHolder {
      val selectable = hints[MasterDetailConfig] == Master
      val layoutId = if (selectable) {
        R.layout.stanza_list_row_selectable
      } else {
        R.layout.stanza_list_row_unselectable
      }

      return ViewHolder(
          LayoutInflater.from(parent.context).inflate(layoutId, parent, false) as TextView
      )
    }

    override fun getItemCount(): Int {
      return rendering.firstLines.size
    }

    override fun onBindViewHolder(
      holder: ViewHolder,
      position: Int
    ) = with(holder.view) {
      text = rendering.firstLines[position]
      isActivated = rendering.selection == position
      setOnClickListener {
        rendering.onStanzaSelected(position)
      }
    }
  }

  companion object : ViewBinding<Rendering>
  by bind(
      R.layout.stanza_list,
      ::StanzasLayoutRunner
  )
}
