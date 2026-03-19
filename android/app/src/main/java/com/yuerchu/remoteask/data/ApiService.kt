package com.yuerchu.remoteask.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.yuerchu.remoteask.data.model.AnswerResponse
import com.yuerchu.remoteask.data.model.DeviceRegistration
import com.yuerchu.remoteask.data.model.QuestionsListResponse
import com.yuerchu.remoteask.data.model.StatusResponse
import com.yuerchu.remoteask.data.model.SubmitAnswerRequest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("answer/{questionId}")
    suspend fun submitAnswer(
        @Path("questionId") questionId: String,
        @Body body: SubmitAnswerRequest
    ): Response<StatusResponse>

    @GET("answer/{questionId}")
    suspend fun checkAnswer(
        @Path("questionId") questionId: String
    ): Response<AnswerResponse>

    @GET("questions")
    suspend fun getQuestions(): Response<QuestionsListResponse>

    @POST("register-device")
    suspend fun registerDevice(
        @Body body: DeviceRegistration
    ): Response<StatusResponse>

    companion object {
        fun create(baseUrl: String, authToken: String): ApiService {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val json = Json { ignoreUnknownKeys = true }
            val contentType = "application/json".toMediaType()

            return Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(ApiService::class.java)
        }
    }
}
