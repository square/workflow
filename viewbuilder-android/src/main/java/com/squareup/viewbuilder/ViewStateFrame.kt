/*
 * Copyright 2018 Square Inc.
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
package com.squareup.viewbuilder

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.SparseArray

internal data class ViewStateFrame(
  val key: String,
  val viewState: SparseArray<Parcelable>
) : Parcelable {

  override fun describeContents(): Int = 0

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeString(key)
    @Suppress("UNCHECKED_CAST")
    parcel.writeSparseArray(viewState as SparseArray<Any>)
  }

  companion object CREATOR : Creator<ViewStateFrame> {
    override fun createFromParcel(parcel: Parcel): ViewStateFrame {
      val key = parcel.readString()

      @Suppress("UNCHECKED_CAST")
      val viewState = parcel.readSparseArray(ViewStateFrame::class.java.classLoader)
          as SparseArray<Parcelable>

      return ViewStateFrame(key, viewState)
    }

    override fun newArray(size: Int): Array<ViewStateFrame?> = arrayOfNulls(size)
  }
}
