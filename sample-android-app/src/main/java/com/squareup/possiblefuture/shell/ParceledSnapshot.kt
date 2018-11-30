package com.squareup.possiblefuture.shell

import android.os.Parcel
import android.os.Parcelable
import com.squareup.workflow.Snapshot
import okio.ByteString

internal class ParceledSnapshot(val snapshot: Snapshot) : Parcelable {

  override fun describeContents(): Int = 0

  override fun writeToParcel(
    dest: Parcel,
    flags: Int
  ) = dest.writeByteArray(snapshot.bytes.toByteArray())

  companion object CREATOR : Parcelable.Creator<ParceledSnapshot> {
    override fun createFromParcel(parcel: Parcel): ParceledSnapshot {
      val bytes = parcel.createByteArray()
      return ParceledSnapshot(Snapshot.of(ByteString.of(*bytes)))
    }

    override fun newArray(size: Int): Array<ParceledSnapshot?> = arrayOfNulls(size)
  }
}
