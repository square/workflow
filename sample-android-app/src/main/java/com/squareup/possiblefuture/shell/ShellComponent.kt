package com.squareup.possiblefuture.shell

import com.squareup.possiblefuture.authworkflow.AuthReactor
import com.squareup.possiblefuture.authworkflow.AuthService
import com.squareup.sample.tictactoe.RealGameLog
import com.squareup.sample.tictactoe.RunGameReactor
import com.squareup.sample.tictactoe.TakeTurnsReactor
import com.squareup.workflow.WorkflowPool
import timber.log.Timber
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread

/**
 * Pretend generated code of a pretend DI framework.
 */
internal class ShellComponent {

  val workflowPool = WorkflowPool()

  private val authService = AuthService()

  fun shellReactor() = ShellReactor(gameReactor(), authReactor())

  private fun authReactor() = AuthReactor(authService, mainThread())

  private fun gameLog() = RealGameLog(mainThread())

  private fun gameReactor() = RunGameReactor(takeTurnsReactor(), gameLog())

  private fun takeTurnsReactor() = TakeTurnsReactor()

  companion object {
    init {
      Timber.plant(Timber.DebugTree())

      val stock = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        Timber.e(error)
        stock.uncaughtException(thread, error)
      }
    }
  }
}
