/*
 * Copyright 2017 Square Inc.
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
package com.squareup.sample.mainactivity

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
