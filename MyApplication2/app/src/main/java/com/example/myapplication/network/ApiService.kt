package com.example.myapplication.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

data class CreateFormRequest(
    val title: String,
    val description: String
)

data class CreateFormResponse(
    val id: String
)

data class FormsResponse(
    val forms: List<FormItem>
)

data class FormItem(
    val id: String,
    val title: String?,
    val description: String?,
    val createdAt: String,
    val media: List<MediaItem>
)

data class MediaItem(
    val id: String,
    val fileName: String?,
    val mimeType: String?,
    val status: String,
    val url: String?,      // null when pending
    val size: Long?,
    val createdAt: String,
    val updatedAt: String
)


interface ApiService {
    @GET("/forms/all")
    suspend fun getAllForms(): FormsResponse

    @POST("/forms")
    suspend fun createForm(@Body body: CreateFormRequest): CreateFormResponse
}
