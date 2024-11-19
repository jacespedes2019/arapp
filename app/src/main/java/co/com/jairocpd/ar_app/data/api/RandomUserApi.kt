package co.com.jairocpd.ar_app.data.api

import retrofit2.Response
import retrofit2.http.GET

interface RandomUserApi {
    @GET("api/")
    suspend fun getRandomUser(): Response<RandomUserResponse>
}
