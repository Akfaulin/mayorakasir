package com.example.mayoandorid.api

import android.content.Context
import com.example.mayoandorid.util.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl = ""

    @Synchronized
    fun getService(context: Context): ApiService {
        val session = SessionManager(context.applicationContext)
        var baseUrl = session.getBaseUrl()
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }

        if (retrofit == null || baseUrl != currentBaseUrl) {
            currentBaseUrl = baseUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val original = chain.request()
                    val builder = original.newBuilder()
                        .header("Accept", "application/json")

                    val token = session.getToken()
                    if (token.isNotEmpty()) {
                        builder.header("Authorization", "Bearer $token")
                    }

                    chain.proceed(builder.build())
                })
                .addInterceptor(logging)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }

    @Synchronized
    fun reset() {
        retrofit = null
        currentBaseUrl = ""
    }
}