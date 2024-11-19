import co.com.jairocpd.ar_app.data.api.RandomUserApi
import co.com.jairocpd.ar_app.data.api.RandomUserResponse
import co.com.jairocpd.ar_app.data.api.UserName
import co.com.jairocpd.ar_app.data.api.UserResult
import co.com.jairocpd.ar_app.data.repository.RandomUserRepository
import co.com.jairocpd.ar_app.domain.model.RandomUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response
import java.io.IOException

class RandomUserRepositoryTest {

    private val api = mock(RandomUserApi::class.java)
    private val repository = RandomUserRepository(api)

    @Test
    fun `fetchRandomUser returns user on success`(): Unit = runBlocking {
        val mockResponse = Response.success(
            RandomUserResponse(
                results = listOf(UserResult("male", UserName("Mr", "John", "Doe")))
            )
        )
        `when`(api.getRandomUser()).thenReturn(mockResponse)


        val result = repository.fetchRandomUser()


        result.onSuccess { user ->
            assertNotNull(user)
            assertEquals("John Doe", user?.fullName)
            assertEquals("male", user?.gender)
        }.onFailure {
            fail("The result should be a success.")
        }
    }

    @Test
    fun `fetchRandomUser returns success with null on empty response`() = runBlocking {

        val mockResponse = Response.success(
            RandomUserResponse(results = emptyList())
        )

        `when`(api.getRandomUser()).thenReturn(mockResponse)

        val result = repository.fetchRandomUser()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }


    @Test
    fun `fetchRandomUser returns failure on API error`() = runBlocking {
        val mockErrorResponse = Response.error<RandomUserResponse>(
            404,
            okhttp3.ResponseBody.create(null, "")
        )
        `when`(api.getRandomUser()).thenReturn(mockErrorResponse)

        val result = repository.fetchRandomUser()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IOException)


        assertEquals("Error 404: Response.error()", exception?.message)
    }


    @Test
    fun `fetchRandomUser returns failure on network error`() = runBlocking {
        val exception = IOException("Network error")

        `when`(api.getRandomUser()).thenAnswer {
            throw exception
        }

        val result = repository.fetchRandomUser()

        assertTrue(result.isFailure)
        val thrownException = result.exceptionOrNull()
        assertNotNull(thrownException)
        assertEquals("Network error", thrownException?.message)
        assertTrue(thrownException is IOException)
    }



}
