package com.squareup.sample.todo

sealed class TodoEditorState {

  object Loading : TodoEditorState()

  data class Loaded(
    val list: TodoList,
    val saved: Boolean
  ) : TodoEditorState()
}
