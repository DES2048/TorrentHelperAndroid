package com.yae.torrenthelper.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(username:String, password:String):Interceptor {
    private val creds = Credentials.basic(username, password)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", creds)
                .build()
        )
    }
}