package co.com.jairocpd.ar_app.data.repository

import co.com.jairocpd.ar_app.data.api.RandomUserApi
import co.com.jairocpd.ar_app.domain.model.RandomUser
import java.io.IOException

class RandomUserRepository(private val api: RandomUserApi) {

    suspend fun fetchRandomUser(): Result<RandomUser?> {
        return try {
            val response = api.getRandomUser()
            if (response.isSuccessful) {
                val userResult = response.body()?.results?.firstOrNull()
                Result.success(
                    userResult?.let {
                        RandomUser(
                            gender = it.gender,
                            fullName = "${it.name.first} ${it.name.last}"
                        )
                    }
                )
            } else {
                Result.failure(IOException("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: IOException) {
            Result.failure(e) // Manejo de errores de red
        } catch (e: Exception) {
            Result.failure(e) // Otros errores inesperados
        }
    }
}
