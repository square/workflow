package com.squareup.possiblefuture.authworkflow

import com.squareup.viewbuilder.EventHandlingScreen

data class LoginScreen(
  override val data: String,
  override val onEvent: (SubmitLogin) -> Unit
) : EventHandlingScreen<String, SubmitLogin>
