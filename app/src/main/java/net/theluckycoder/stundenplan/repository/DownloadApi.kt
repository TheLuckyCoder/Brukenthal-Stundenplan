package net.theluckycoder.stundenplan.repository

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadApi {

    @Streaming
    @GET
    suspend fun download(@Url url: String): ResponseBody
}
