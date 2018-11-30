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
