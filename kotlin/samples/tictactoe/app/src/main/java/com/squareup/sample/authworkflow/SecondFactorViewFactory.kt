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
package com.squareup.sample.authworkflow

import com.squareup.sample.tictactoe.databinding.SecondFactorLayoutBinding
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewFactory
import com.squareup.workflow.ui.backPressedHandler

internal val SecondFactorViewFactory: ViewFactory<SecondFactorScreen> =
  LayoutRunner.bind(SecondFactorLayoutBinding::inflate) { rendering, _ ->
    root.backPressedHandler = { rendering.onCancel() }
    secondFactorToolbar.setNavigationOnClickListener { rendering.onCancel() }

    secondFactorErrorMessage.text = rendering.errorMessage

    secondFactorSubmitButton.setOnClickListener {
      rendering.onSubmit(secondFactor.text.toString())
    }
  }
