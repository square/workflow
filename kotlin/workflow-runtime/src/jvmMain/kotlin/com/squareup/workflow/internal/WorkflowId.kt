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

import com.squareup.workflow.Workflow
import com.squareup.workflow.parse
import com.squareup.workflow.readUtf8WithLength
import com.squareup.workflow.writeUtf8WithLength
import okio.Buffer
import okio.ByteString
import kotlin.reflect.KClass

internal typealias AnyId = WorkflowId<*, *, *>

/**
 * Value type that can be used to distinguish between different workflows of different types or
 * the same type (in that case using a [name]).
 */
data class WorkflowId<in PropsT, OutputT : Any, out RenderingT>
@PublishedApi
internal constructor(
  internal val type: KClass<out Workflow<PropsT, OutputT, RenderingT>>,
  internal val name: String = ""
) {
  constructor(
    workflow: Workflow<PropsT, OutputT, RenderingT>,
    name: String = ""
  ) : this(workflow::class, name)

  /**
   * String representation of this workflow's type (i.e. class name), suitable for use in
   * diagnostic output (see
   * [WorkflowHierarchyDebugSnapshot][com.squareup.workflow.diagnostic.WorkflowHierarchyDebugSnapshot]
   * ).
   */
  val typeDebugString: String = type.java.name
}

@Suppress("unused")
internal fun <W : Workflow<I, O, R>, I, O : Any, R>
    W.id(key: String = ""): WorkflowId<I, O, R> = WorkflowId(this, key)

internal fun WorkflowId<*, *, *>.toByteString(): ByteString = Buffer()
    .also { sink ->
      sink.writeUtf8WithLength(type.java.name)
      sink.writeUtf8WithLength(name)
    }
    .readByteString()

internal fun restoreId(bytes: ByteString): WorkflowId<*, *, *> = bytes.parse { source ->
  val typeName = source.readUtf8WithLength()
  @Suppress("UNCHECKED_CAST")
  val type = Class.forName(typeName) as Class<out Workflow<Nothing, Any, Any>>
  val name = source.readUtf8WithLength()
  return WorkflowId(type.kotlin, name)
}
