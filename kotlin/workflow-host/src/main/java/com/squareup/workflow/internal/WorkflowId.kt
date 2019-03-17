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
package com.squareup.workflow.internal

import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.Workflow
import com.squareup.workflow.writeUtf8WithLength
import okio.Buffer
import okio.ByteString
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal typealias AnyId = WorkflowId<*, *, *, *>

/**
 * Value type that can be used to distinguish between different workflows of different types or
 * the same type (in that case using a [name]).
 */
internal data class WorkflowId<in InputT : Any, StateT : Any, out OutputT : Any, out RenderingT : Any>
@PublishedApi
internal constructor(
  internal val type: KClass<out Workflow<InputT, StateT, OutputT, RenderingT>>,
  internal val name: String = ""
)

@Suppress("unused")
internal fun <W : Workflow<P, S, O, R>, P : Any, S : Any, O : Any, R : Any>
    W.id(key: String = ""): WorkflowId<P, S, O, R> =
  WorkflowId(this::class, key)

internal fun WorkflowId<*, *, *, *>.toByteString(): ByteString = Buffer()
    .also { sink ->
      sink.writeUtf8WithLength(type.jvmName)
      sink.writeUtf8WithLength(name)
    }
    .readByteString()

internal fun restoreId(bytes: ByteString): WorkflowId<*, *, *, *> = bytes.parse { source ->
  val typeName = source.readUtf8WithLength()
  @Suppress("UNCHECKED_CAST")
  val type = Class.forName(typeName) as Class<out Workflow<Nothing, Any, Any, Any>>
  val name = source.readUtf8WithLength()
  return WorkflowId(type.kotlin, name)
}
