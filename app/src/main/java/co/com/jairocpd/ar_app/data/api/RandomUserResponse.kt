package co.com.jairocpd.ar_app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RandomUserResponse(
    @Json(name = "results") val results: List<UserResult>
)

@JsonClass(generateAdapter = true)
data class UserResult(
    @Json(name = "gender") val gender: String,
    @Json(name = "name") val name: UserName
)

@JsonClass(generateAdapter = true)
data class UserName(
    @Json(name = "title") val title: String,
    @Json(name = "first") val first: String,
    @Json(name = "last") val last: String
)
