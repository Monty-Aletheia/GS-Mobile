package com.example.windrose.network


import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

const val URL = "http://192.168.0.181:8080/api/"

data class UserResponse(
    val message: String,
    val user: User,
    val _links: Links
)

data class User(
    val id: String,
    val name: String,
    val email: String
)

data class UserDto(
    val name: String,
    val email: String,
    val password: String
)

data class Links(
    val login: Link
)

data class Link(
    val href: String
)

data class UserLoginDto(
    val email: String,
    val password: String
)

interface AuthService {

    @POST("auth/register")
    suspend fun registerUser(@Body userDto: UserDto): Response<UserResponse>

    @POST("auth/login")
    suspend fun signInUser(@Body userLoginDto: UserLoginDto): Response<UserResponse>
}



object API {
    fun buildAuthService(): AuthService {
        val retrofit =
            Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(AuthService::class.java)
    }
}
