package com.squareup.workflow

import okio.Buffer
import okio.BufferedSink
import okio.ByteString

/**
 * A lazy wrapper of [ByteString]. Allows [ScreenWorkflow]s to capture their state
 * frequently, without worrying about performing unnecessary serialization work.
 */
class Snapshot
private constructor(private val toByteString: () -> ByteString) {

  companion object {
    @JvmField
    val EMPTY = Snapshot.of(ByteString.EMPTY)

    @JvmStatic
    fun of(string: String): Snapshot = Snapshot { ByteString.encodeUtf8(string) }

    @JvmStatic
    fun of(byteString: ByteString): Snapshot = Snapshot { byteString }

    @JvmStatic
    fun of(lazy: () -> ByteString): Snapshot = Snapshot(lazy)

    @JvmStatic
    fun of(integer: Int): Snapshot {
      return Snapshot {
        with(Buffer()) {
          writeInt(integer)
          readByteString()
        }
      }
    }

    /** Create a snapshot by writing to a nice ergonomic [BufferedSink]. */
    @JvmStatic
    fun write(lazy: (BufferedSink) -> Unit): Snapshot = of {
      Buffer().apply(lazy)
          .readByteString()
    }
  }

  @get:JvmName("bytes")
  val bytes: ByteString by lazy { toByteString() }

  /**
   * Returns a `String` describing the [bytes] of this `Snapshot`.
   *
   * **This method forces serialization, calling it may be expensive.**
   */
  override fun toString(): String = "Snapshot($bytes)"

  /**
   * Compares `Snapshot`s by comparing their [bytes].
   *
   * **This method forces serialization, calling it may be expensive.**
   */
  override fun equals(other: Any?) = (other as? Snapshot)?.let { bytes == it.bytes } ?: false

  /**
   * Calculates hashcode using [bytes].
   *
   * **This method forces serialization, calling it may be expensive.**
   */
  override fun hashCode() = bytes.hashCode()
}
