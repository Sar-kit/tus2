package com.example.myapplication

object AppConfig {
    private const val DEBUG_HOST = "192.168.0.101"
    private const val PROD_HOST = "api.myapp.com"

    private const val PORT = 1080

    val BASE_API = "http://$DEBUG_HOST:$PORT"

    val TUS_ENDPOINT = "$BASE_API/uploads"
}
