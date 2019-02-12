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
import okhttp3.ResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type

class RetrofitServiceTest {
  @Test fun stuff() {
    val retro = Retrofit.Builder()
        .addConverterFactory(RRC)
        .addConverterFactory(StringConverter)
        .addCallAdapterFactory(CAF)
        .baseUrl("http://example.com")
        .build()

    val service = retro.create<MyService>()
    val response = service.bar()
  }

  object StringConverter : Converter.Factory() {
    override fun responseBodyConverter(
      type: Type,
      annotations: Array<Annotation>,
      retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
      if (type != String::class.java) return null
      return Converter<ResponseBody, String> { body -> body.string() }
    }
  }

  object RRC : Converter.Factory() {
    override fun responseBodyConverter(
      type: Type,
      annotations: Array<Annotation>,
      retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
      if (getRawType(type) != getRawType(ReceivedResponse::class.java)) return null
      return super.responseBodyConverter(type, annotations, retrofit)
    }
  }

  object CAF : CallAdapter.Factory() {
    private val requestFactoryField = Class.forName("retrofit2.OkHttpCall")
        .getDeclaredField("requestFactory")
        .apply { isAccessible = true }
    private val methodField = Class.forName("retrofit2.RequestFactory")
        .getDeclaredField("method")
        .apply { isAccessible = true }

    private fun Call<*>.getMethod(): Method {
      val requestFactory = requestFactoryField.get(this)
      val method = methodField.get(requestFactory) as Method
      val serviceInterface = method.declaringClass
      val defaultImpls = serviceInterface.classes.single { "DefaultImpls" in it.name }
      val obj = Proxy.newProxyInstance(
          serviceInterface.classLoader, arrayOf(serviceInterface)
      ) { proxy, method, args ->
        TODO()
      }
      val defaultMethod = defaultImpls.declaredMethods.single { it.name == method.name }
      try {
        defaultMethod.invoke(null, obj)
      } catch (e: Throwable) {
        // Eat the DummyException, throw other exceptions.
        if (!generateSequence(e) { it.cause }.any { it is DummyException }) {
          throw e
        }
      }
      val predicate = rejectPredicate.get()!!
      rejectPredicate.remove()

      TODO()
    }

    override fun get(
      returnType: Type,
      annotations: Array<Annotation>,
      retrofit: Retrofit
    ): CallAdapter<*, *>? {
      return object : CallAdapter<Any, Any> {
        override fun adapt(call: Call<Any>): Any {
          return call.getMethod()
        }

        override fun responseType(): Type = String::class.java
      }
    }
  }

  interface MyService {
    @GET("/foo")
    fun bar(): ReceivedResponse<String> = rejectWhen { it == "fail" }
  }

  sealed class ReceivedResponse<T : Any> {
    data class Accepted<T : Any>(val response: T) : ReceivedResponse<T>()
  }
}

private object DummyException : RuntimeException()

private val rejectPredicate = ThreadLocal<(Any) -> Boolean>()
fun <T : Any> rejectWhen(predicate: (T) -> Boolean): ReceivedResponse<T> {
  @Suppress("UNCHECKED_CAST")
  rejectPredicate.set(predicate as (Any) -> Boolean)
  throw DummyException
}
