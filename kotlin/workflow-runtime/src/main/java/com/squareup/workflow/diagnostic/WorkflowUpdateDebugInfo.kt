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
package com.squareup.workflow.diagnostic

import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Passthrough
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Kind.Updated
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Sink
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Subtree
import com.squareup.workflow.diagnostic.WorkflowUpdateDebugInfo.Source.Worker
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.decode
import kotlinx.serialization.encode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * A description of a workflow update triggered by a [Source] (worker, event, etc).
 *
 * This is a simple linked list that represents a traversal down the workflow tree that starts at
 * the root and indicates, for each workflow, if its child output something that it handled
 * (see [Kind]), or if it was just a parent of a workflow that didn't output anything.
 *
 * When a workflow handles an update, the type of update is indicated by [Source].
 */
@Serializable
data class WorkflowUpdateDebugInfo(
  val workflowType: String,
  @Polymorphic val kind: Kind
) {

  /**
   * A sealed class that indicates whether a workflow actually executed a `WorkflowAction`, or
   * was just the ancestor of a workflow that did.
   *
   * Contains two subclasses, see their documentation for details:
   * - [Updated]
   * - [Passthrough]
   */
  sealed class Kind {
    /**
     * Indicates that this workflow actually executed a `WorkflowAction`.
     * [Updated.source] is a [Source] that indicates if the action was triggered by:
     * - An event ([Source.Sink])
     * - A worker ([Source.Worker])
     * - A child workflow emitting an output ([Source.Subtree])
     */
    @Serializable
    data class Updated(@Polymorphic val source: Source) : Kind()

    /**
     * Indicates that one of this workflow's descendants executed a
     * `WorkflowAction`, but none of its immediate children emitted an output, so this workflow
     * didn't get directly notified about it.
     */
    @Serializable
    data class Passthrough(
      val key: String,
      val childInfo: WorkflowUpdateDebugInfo
    ) : Kind()

    companion object {
      internal val serializationModule = SerializersModule {
        polymorphic<Kind> {
          addSubclass(Updated.serializer())
          addSubclass(Passthrough.serializer())
        }
      }
    }
  }

  /**
   * A sealed class that indicates what triggered the update.
   *
   * Contains three subclasses, see their documentation for details:
   * - [Sink]
   * - [Worker]
   * - [Subtree]
   */
  sealed class Source {
    /**
     * Indicates that the update was triggered by an event being received by a
     * [Sink][com.squareup.workflow.Sink] (see
     * [makeActionSink][com.squareup.workflow.RenderContext.makeActionSink]).
     */
    object Sink : Source()

    /**
     * Indicates that the update was triggered by an output emitted from a
     * [Worker][com.squareup.workflow.Worker] being run by this workflow.
     *
     * @param key The string key of the worker that emitted the output.
     * @param output The value emitted from the worker.
     */
    @Serializable
    data class Worker(
      val key: String,
      @ContextualSerialization val output: Any
    ) : Source()

    /**
     * Indicates that the update was triggered by an output emitted from a child workflow that was
     * rendered by this workflow.
     *
     * @param key The string key of the child that emitted the output.
     * @param output The value emitted from the child.
     * @param childInfo The [WorkflowUpdateDebugInfo] that describes the child's update.
     */
    @Serializable
    data class Subtree(
      val key: String,
      @ContextualSerialization val output: Any,
      val childInfo: WorkflowUpdateDebugInfo
    ) : Source()

    companion object {
      @UseExperimental(ImplicitReflectionSerializer::class)
      internal val serializationModule = SerializersModule {
        polymorphic<Source> {
          addSubclass(Sink::class, SinkSerializer)
          addSubclass(Worker.serializer())
          addSubclass(Subtree.serializer())
        }
      }
    }
  }

  /**
   * This string is expensive to generate, so cache it.
   */
  private val lazyDescription by lazy(PUBLICATION) {
    buildString {
      writeUpdate(this@WorkflowUpdateDebugInfo)
    }
  }

  override fun toString(): String = """
    |WorkflowUpdateDebugInfo(
    |${toDescriptionString().trimEnd().prependIndent("  ")}
    |)
  """.trimMargin("|")

  /**
   * Generates a multi-line, recursive string describing the update.
   */
  fun toDescriptionString(): String = lazyDescription

  companion object {
    /**
     * Use this module if you need to use kotlinx serialization to serialize/deserialize this type.
     */
    val serializationModule = Kind.serializationModule + Source.serializationModule
  }
}

private fun StringBuilder.writeUpdate(update: WorkflowUpdateDebugInfo) {
  append(update.workflowType)
  append(' ')

  when (val kind = update.kind) {
    is Updated -> {
      append("updated from ")
      when (val source = kind.source) {
        Sink -> append("sink")
        is Worker -> {
          append("worker[key=")
          append(source.key)
          append("]: ")
          append(source.output)
        }
        is Subtree -> {
          append("workflow[key=")
          append(source.key)
          append("]: ")
          appendln(source.output)
          append("↳ ")
          append(source.childInfo.toDescriptionString())
        }
      }
    }
    is Passthrough -> {
      append("passing through from workflow[key=")
      append(kind.key)
      appendln("]")
      append("↳ ")
      append(kind.childInfo.toDescriptionString())
    }
  }
}

// TODO this is gross
private object SinkSerializer : KSerializer<Sink> {
  private val jsonObjectSerizlier = JsonObjectSerializer

  override val descriptor: SerialDescriptor = jsonObjectSerizlier.descriptor

  @UseExperimental(ImplicitReflectionSerializer::class)
  override fun deserialize(decoder: Decoder): Sink {
    decoder.decode<JsonObject>()
    return Sink
  }

  @UseExperimental(ImplicitReflectionSerializer::class)
  override fun serialize(
    encoder: Encoder,
    obj: Sink
  ) {
    encoder.encode(JsonObject(emptyMap()))
  }
}
