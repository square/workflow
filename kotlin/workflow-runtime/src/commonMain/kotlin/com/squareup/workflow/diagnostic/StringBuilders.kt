package com.squareup.workflow.diagnostic

/**
 * Apparently this function only exists on the JVM `StringBuilder`.
 */
internal fun StringBuilder.appendln(value: Any? = null) = apply {
  append(value)
  append('\n')
}
