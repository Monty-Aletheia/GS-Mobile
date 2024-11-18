package com.example.windrose.network


import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

const val URL = "http://192.168.0.181:8080/api/"



data class UserDeviceResponseDTO(
    val devices: List<DeviceDTO>
)

data class DeviceDTO(
    val id: String,
    val name: String,
    val category: String,
    val model: String,
    val powerRating: Double,
    val estimatedUsageHours: Double

)


data class UserResponse(
    val message: String,
    val user: UserResponseDTO,
    val _links: Links
)

data class UserResponseDTO(
    val id: String,
    val name: String,
    val email: String,
    val firebaseId: String
)

data class UserDto(
    val name: String,
    val email: String,
    val password: String,
    val firebaseId: String
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
