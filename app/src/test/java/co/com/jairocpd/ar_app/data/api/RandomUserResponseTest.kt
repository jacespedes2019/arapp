package co.com.jairocpd.ar_app.data.api

import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

class RandomUserResponseTest {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(RandomUserResponse::class.java)

    @Test
    fun `deserialize valid JSON into RandomUserResponse`() {
        val json = """
            {
                "results": [
                    {
                        "gender": "female",
                        "name": {
                            "title": "Ms",
                            "first": "Jane",
                            "last": "Doe"
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(1, response?.results?.size)

        val userResult = response?.results?.first()
        assertEquals("female", userResult?.gender)
        assertEquals("Ms", userResult?.name?.title)
        assertEquals("Jane", userResult?.name?.first)
        assertEquals("Doe", userResult?.name?.last)
    }

    @Test
    fun `serialize RandomUserResponse into JSON`() {
        val userName = UserName("Mr", "John", "Smith")
        val userResult = UserResult("male", userName)
        val randomUserResponse = RandomUserResponse(listOf(userResult))

        val json = adapter.toJson(randomUserResponse)

        val expectedJson = """
            {
                "results": [
                    {
                        "gender": "male",
                        "name": {
                            "title": "Mr",
                            "first": "John",
                            "last": "Smith"
                        }
                    }
                ]
            }
        """.trimIndent().replace("\\s".toRegex(), "")

        assertEquals(expectedJson, json.replace("\\s".toRegex(), ""))
    }

    @Test
    fun `deserialize empty results list`() {
        val json = """
            {
                "results": []
            }
        """.trimIndent()

        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response?.results?.isEmpty() ?: false)
    }

}
