package com.squareup.workflow.detekt

import io.gitlab.arturbosch.detekt.test.compileContentForTest
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.fail

class RenderStateTest {

  @Test fun stuff() {
    val file = compileContentForTest(
        """
          package com.squareup.workflow
    
          class BadStatelessWorkflow : StatefulWorkflow<String, String, String, String>() {
            override fun initialState(
              props: String,
              snapshot: Snapshot?
            ): String = ""
    
            override fun render(
              props: String,
              state: String,
              context: RenderContext<String, String>
            ): String {
              action {
                nextState = state
              }
              return ""
            }
    
            override fun snapshotState(state: String): Snapshot = Snapshot.EMPTY
          }
        """.trimIndent()
    )
    val findings = RenderState.lint(file)
//    val findings = RenderState.lint(
//        """
//          import com.squareup.workflow.StatelessWorkflow
//
//          class Foo : StatelessWorkflow<A, B, C>() {
//            fun bar(baz: String) {
//              println(baz)
//            }
//          }
//        """.trimIndent()
//    )
    fail(findings.joinToString(separator = "\n"))
  }
}
