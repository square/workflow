package com.squareup.workflow.diagnostic

import com.squareup.workflow.diagnostic.BrowseableDebugData.Data
import com.squareup.workflow.diagnostic.BrowseableDebugData.Node
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DebugServerTest {

  private val root = Node(
      children = mapOf(
          "foo" to { Data("Hello, world!\n") },
          "bar" to { Data("Foo Bar\n") },
          "baz" to { Node(children = mapOf("a" to { Data("b") })) }
      )
  )

  @Test fun server() {
    runBlocking {
      val server = DebugServer(root)
      server.serve()
    }
  }

  @Test fun `resolve root`() {
    val result = root.resolve(Paths.get("/"))
    assertSame(root, result)
  }

  @Test fun `resolve directory`() {
    val result = root.resolve(Paths.get("/baz"))
    assertEquals(root.children.getValue("baz")(), result)
  }

  @Test fun `resolve directory trailing slash`() {
    val result = root.resolve(Paths.get("/baz/"))
    assertEquals(root.children.getValue("baz")(), result)
  }

  @Test fun `resolve file`() {
    val result = root.resolve(Paths.get("/baz/a"))
    assertEquals("b", (result as Data).description)
  }

  @Test fun `resolve file trailing slash`() {
    val result = root.resolve(Paths.get("/baz/a/"))
    assertEquals("b", (result as Data).description)
  }
}
