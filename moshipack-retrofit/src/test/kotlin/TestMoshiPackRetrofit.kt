import com.daveanthonythomas.moshipack.MoshiPackConverterFactory
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

class TestMoshiPackRetrofit {

    @Rule @JvmField
    var server = MockWebServer()

    interface Cats {
        @GET("/")
        fun cats(): Call<List<Cat>>
    }

    @Test
    fun testCats() {
        val buffer = Buffer().apply { write(TestMoshiPackRetrofit::class.java.getResource("cats").readBytes()) }
        server.enqueue(MockResponse().setBody(buffer))

        val retrofit = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(MoshiPackConverterFactory())
                .build()

        val catsEndpoint = retrofit.create(Cats::class.java)
        val call = catsEndpoint.cats()
        val response = call.execute()

        assertEquals(32, response.body()?.size)
    }
}

class Cat {
    var breed: String = ""
    var country: String? = null
    var origin: String? = null
    var bodytype: String? = null
    var coat: String? = null
    var pattern: String? = null
    var image: String? = null
}