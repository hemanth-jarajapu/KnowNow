package com.passion.knownow.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface RawApi {
    @GET("{path}")
    suspend fun getText(@Path("path", encoded = true) path: String): String
}