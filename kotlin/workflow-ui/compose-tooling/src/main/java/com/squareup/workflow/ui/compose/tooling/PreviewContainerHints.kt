/*
 * Copyright 2020 Square Inc.
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
package com.squareup.workflow.ui.compose.tooling

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.Composable
import androidx.compose.remember
import com.squareup.workflow.ui.ContainerHints
import com.squareup.workflow.ui.ViewBinding
import com.squareup.workflow.ui.ViewRegistry
import kotlin.reflect.KClass

@Composable internal fun PreviewContainerHints(stubBinding: ViewBinding<Any>) =
  remember(stubBinding) { ContainerHints(PreviewViewRegistry(stubBinding)) }

/**
 * TODO kdoc
 */
private class PreviewViewRegistry(private val stubBinding: ViewBinding<Any>) :
    ViewRegistry {

  override val keys: Set<KClass<*>> get() = emptySet()

  override fun <RenderingT : Any> buildView(
    initialRendering: RenderingT,
    initialContainerHints: ContainerHints,
    contextForNewView: Context,
    container: ViewGroup?
  ): View = getBindingFor(initialRendering::class).buildView(
      initialRendering, initialContainerHints, contextForNewView, container
  )

  @Suppress("UNCHECKED_CAST")
  override fun <RenderingT : Any> getBindingFor(
    renderingType: KClass<out RenderingT>
  ): ViewBinding<RenderingT> = stubBinding
}
