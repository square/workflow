package com.squareup.workflow.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * TODO write documentation
 */
class Provider : RuleSetProvider {
  override val ruleSetId = "com-squareup-workflow"

  override fun instance(config: Config) = RuleSet(
      id = ruleSetId,
      rules = listOf(
          RenderState(config)
      )
  )
}
