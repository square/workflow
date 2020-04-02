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

import com.squareup.workflow.internal.InlineLinkedList.InlineListNode

/**
 * A simple singly-linked list that uses the list elements themselves to store the links.
 *
 * List elements must implement [InlineListNode].
 *
 * It supports a limited number of operations:
 *  - [forEach]: Iterate over the list. This is an inline function, so has no lambda allocation
 *    overhead.
 *  - [removeFirst]: Find the first element that matches a predicate, remove it, and return it.
 *    This function is also inline and so has no lambda allocation overhead.
 *  - [plusAssign]
 *  - [clear]
 */
internal class InlineLinkedList<T : InlineListNode<T>> {

  /**
   * Interface to be implemented by something that can be stored in an [InlineLinkedList].
   *
   * @property nextListNode For use by [InlineLinkedList] only – implementors should never mutate
   * this property. It's default value should be null.
   */
  interface InlineListNode<T : InlineListNode<T>> {
    var nextListNode: T?
  }

  var head: T? = null
  var tail: T? = null

  /**
   * Append an element to the end of the list.
   *
   * @throws IllegalArgumentException If node is already linked in another list.
   */
  operator fun plusAssign(node: T) {
    require(node.nextListNode == null) { "Expected node to not be linked." }

    tail?.let { oldTail ->
      tail = node
      oldTail.nextListNode = node
      return
    }

    // List is currently empty.
    check(head == null)
    head = node
    tail = node
  }

  /**
   * Finds the first node matching [predicate], removes it, and returns it.
   *
   * This function is inline and has no lambda allocation overhead.
   *
   * @return The matching element, or null if not found.
   */
  inline fun removeFirst(predicate: (T) -> Boolean): T? {
    var currentNode: T? = head
    var previousNode: T? = null

    while (currentNode != null) {
      if (predicate(currentNode)) {
        // Unlink the node from the list.
        if (previousNode == null) {
          // First element matched.
          head = currentNode.nextListNode
        } else {
          previousNode.nextListNode = currentNode.nextListNode
        }
        if (tail == currentNode) {
          tail = previousNode
        }
        currentNode.nextListNode = null
        return currentNode
      }

      previousNode = currentNode
      currentNode = currentNode.nextListNode
    }

    return null
  }

  /**
   * Iterates over the list.
   */
  inline fun forEach(block: (T) -> Unit) {
    var currentNode = head
    while (currentNode != null) {
      block(currentNode)
      currentNode = currentNode.nextListNode
    }
  }

  /**
   * Removes all elements from the list.
   */
  fun clear() {
    head = null
    tail = null
  }
}
