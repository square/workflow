package com.squareup.sample.porchbox

import android.net.Uri
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.squareup.sample.porchbox.PorchWorkflow.Auth
import com.squareup.sample.porchbox.PorchWorkflow.Props
import com.squareup.sample.porchbox.PorchWorkflow.RealtimeDB
import com.squareup.sample.porchbox.PorchWorkflow.State.LoadingPorch
import com.squareup.sample.porchbox.PorchWorkflow.State.LoggingIn
import com.squareup.workflow.testing.testFromStart
import com.squareup.workflow.testing.testFromState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class PorchWorkflowTest {

  // Use fakes for Firebase services
  object FakeAuthSuccess: Auth {
    override val currentUser: FirebaseUser? = Mockito.mock(FirebaseUser::class.java)

    init {
      Mockito.`when`(currentUser?.email).thenReturn(BuildConfig.CLIENT_EMAIL)
    }

    @Suppress("UNCHECKED_CAST")
    override fun signInWithEmailAndPassword(
      email: String,
      pass: String,
      listener: OnCompleteListener<AuthResult>
    ): Task<AuthResult> {
      val authTaskResult = Mockito.mock(Task::class.java)
      val authResult = Mockito.mock(AuthResult::class.java)
      Mockito.`when`(authTaskResult.result).thenReturn(authResult)
      Mockito.`when`(authTaskResult.isSuccessful).thenReturn(true)
      Mockito.`when`(authResult.user).thenReturn(currentUser)
      listener.onComplete(authTaskResult as Task<AuthResult>)
      return authTaskResult
    }
  }

  object FakeRealtimeDb: RealtimeDB {
    private val testInbox = listOf(
        Mockito.mock(DocumentSnapshot::class.java).also {
          Mockito.`when`(it.getString("name")).thenReturn("Lysol Wipes")
          Mockito.`when`(it.getString("photoUri")).thenReturn("https://picsum.photos/id/1021/200/200.jpg")
        },
        Mockito.mock(DocumentSnapshot::class.java).also {
          Mockito.`when`(it.getString("name")).thenReturn("Groceries")
          Mockito.`when`(it.getString("photoUri")).thenReturn("https://picsum.photos/id/1021/200/200.jpg")
        },
        Mockito.mock(DocumentSnapshot::class.java).also {
          Mockito.`when`(it.getString("name")).thenReturn("Shopvac")
          Mockito.`when`(it.getString("photoUri")).thenReturn("https://picsum.photos/id/1021/200/200.jpg")
        },
        Mockito.mock(DocumentSnapshot::class.java).also {
          Mockito.`when`(it.getString("name")).thenReturn("Book")
          Mockito.`when`(it.getString("photoUri")).thenReturn("https://picsum.photos/id/1021/200/200.jpg")
        }
    )

    override fun query(
      path: String,
      field: String?,
      subset: List<String?>,
      listener: OnSuccessListener<QuerySnapshot>
    ): Task<QuerySnapshot> {
      val queryTaskResult = Mockito.mock(Task::class.java)
      val snapshot = Mockito.mock(QuerySnapshot::class.java)
      Mockito.`when`(queryTaskResult.result).thenReturn(snapshot)
      Mockito.`when`(queryTaskResult.isSuccessful).thenReturn(true)
      Mockito.`when`(snapshot.documents).thenReturn(
          testInbox
      )
      listener.onSuccess(snapshot)
      @Suppress("UNCHECKED_CAST")
      return queryTaskResult as Task<QuerySnapshot>
    }
  }

  @Test fun `starts in Login`() {
    val workflow = PorchWorkflow(FakeAuthSuccess, FakeRealtimeDb);

    workflow.testFromStart(Props) {
      assertThat(awaitNextRendering().beneathModals is LoginRendering).isTrue()
    }
  }

  @Test fun `test login loading`() {
    PorchWorkflow(FakeAuthSuccess, FakeRealtimeDb).testFromState(
        Props,
        LoggingIn(BuildConfig.CLIENT_EMAIL, BuildConfig.CLIENT_PASS)) {
      assertThat(awaitNextRendering().beneathModals).isEqualTo(LoggingIn(BuildConfig.CLIENT_EMAIL, BuildConfig.CLIENT_PASS))
    }
  }

  @Test fun `test porch loading`() {
    PorchWorkflow(FakeAuthSuccess, FakeRealtimeDb).testFromState(
        Props,
        LoadingPorch(FakeAuthSuccess.currentUser!!)) {
      assertThat(awaitNextRendering().beneathModals).isEqualTo(LoadingPorch(FakeAuthSuccess.currentUser!!))
    }
  }


}