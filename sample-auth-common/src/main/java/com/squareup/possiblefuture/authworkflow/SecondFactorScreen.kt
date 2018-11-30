package com.squareup.possiblefuture.authworkflow

import com.squareup.viewbuilder.EventHandlingScreen

data class SecondFactorScreen(
  override val data: String,
  override val onEvent: (SubmitSecondFactor) -> Unit
) : EventHandlingScreen<String, SubmitSecondFactor>
