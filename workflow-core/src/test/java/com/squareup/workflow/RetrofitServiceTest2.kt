/*
 *
 *  * Copyright 2019 Square Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.squareup.workflow

import com.squareup.workflow.RetrofitServiceTest.ReceivedResponse
import com.squareup.workflow.RetrofitServiceTest.ReceivedResponse.Accepted
import com.squareup.workflow.RetrofitServiceTest2.SuccessOrFailure.Failure
import com.squareup.workflow.RetrofitServiceTest2.SuccessOrFailure.Success
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.http.GET
import java.lang.reflect.Type

class RetrofitServiceTest2 {
  interface MyService {
    @GET("/foo")
    fun foo(): ReceivedResponse<StringDecision>

    @GET("/bar")
    @SuccessField
    fun bar(): ReceivedResponse<ReflectionDecision<MyProto>>
  }

  class MyProto(@JvmField val success: Boolean)

  object DecisionCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
      returnType: Type,
      annotations: Array<Annotation>,
      retrofit: Retrofit
    ): CallAdapter<*, *>? {
      TODO()
    }
  }

  interface Decision<T : Any> {
    val response: T
    val isSuccessful: Boolean
  }

  class StringDecision(override val response: String) : Decision<String> {
    override val isSuccessful: Boolean get() = response == "fail"
  }

  annotation class SuccessField(val name: String = "success")

  class ReflectionDecision<T : Any/* Proto messages only*/>(
    override val response: T,
      // This could support recursive, dot-separated names like "status.success".
    fieldName: String
  ) : Decision<T> {
    private val successField = response.javaClass.getDeclaredField(fieldName)
    override val isSuccessful: Boolean = successField.get(response) as Boolean
  }

  sealed class SuccessOrFailure<T : Any> {
    data class Success<T : Any>(val value: T) : SuccessOrFailure<T>()
    data class Failure<T : Any>(val response: ReceivedResponse<T>) : SuccessOrFailure<T>()
  }

  fun <T : Any> ReceivedResponse<Decision<T>>.toSuccessOrFailure(): SuccessOrFailure<T> {
    if (this is Accepted && response.isSuccessful) {
      return Success(response.response)
    }
    // TODO unwrap response type
    return Failure(this)
  }
}
