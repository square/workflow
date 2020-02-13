package com.squareup.workflow.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity.Defect
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * TODO kdoc
 *
 * @active
 */
@Suppress("UNUSED_PARAMETER")
class RenderState(config: Config) : Rule() {

  override val issue = Issue(
      id = javaClass.simpleName,
      severity = Defect,
      description = "TODO this is a rule",
      debt = Debt.FIVE_MINS
  )

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    if (bindingContext == BindingContext.EMPTY) {
      println("NO binding context, aborting.")
      return
    }

    val superTypes = classOrObject.getSuperTypeList()?.entries ?: return

    val isStatefulWorkflow =
      superTypes.mapNotNull { bindingContext[BindingContext.TYPE, it.typeReference] }
          .map { it.getJetTypeFqName(false) }
          .any { it == "com.squareup.workflow.StatefulWorkflow" }
    if (!isStatefulWorkflow) return

    println("Found a workflow!")

    super.visitClassOrObject(classOrObject)
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    super.visitReferenceExpression(expression)
//    report(CorrectableCodeSmell())

    println("expression: ${expression.text} ${bindingContext.getType(expression)}")
  }
}
