package com.example.harish332m.data.api

import com.example.harish332m.data.model.HealthResponse
import com.example.harish332m.data.model.SearchRequest
import com.example.harish332m.data.model.SearchResponse
import com.example.harish332m.data.model.UploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @POST("search/")
    suspend fun search(
        @Body request: SearchRequest
    ): Response<SearchResponse>
}
