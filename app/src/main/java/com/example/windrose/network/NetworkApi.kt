package com.example.windrose.network


import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

const val URL = "http://192.168.0.181:8080/api/"


data class UserDeviceDTO(
    val deviceId: String,
    val estimatedUsageHours: Double
)

data class UserDeviceListDTO(
    val userDevices: List<UserDeviceDTO>
)

data class UserDeviceRemoveDTO(
    val deviceIds: List<String>
)


data class UserDeviceResponseDTO(
    val content: List<DeviceDTO>
)

data class DeviceDTO(
    val id: String,
    val name: String,
    val category: String,
    val model: String,
    val powerRating: Double,
    val estimatedUsageHours: Double,
    val consumption: Double

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

interface UserDeviceService{

    @GET("users/firebase/{firebaseId}")
    suspend fun getUserByFirebaseUid(@Path("firebaseId") firebaseId: String): Response<UserResponseDTO>

    @GET("users/{userId}/devices")
    suspend fun getUserDevicesById(@Path("userId") userId: String): Response<UserDeviceResponseDTO>

    @POST("users/{userId}/devices")
    suspend fun addUserDevice(@Path("userId") userId: String, @Body userDeviceListDTO: UserDeviceListDTO): Response<Unit>

    @HTTP(method = "DELETE", path = "users/{userId}/devices", hasBody = true)
    suspend fun deleteUserDevice(@Path("userId") userId: String, @Body userDeviceRemoveDTO: UserDeviceRemoveDTO): Response<Unit>

}



object API {

    fun buildUserDeviceService(): UserDeviceService{
        val retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(UserDeviceService::class.java)
    }

    fun buildAuthService(): AuthService {
        val retrofit =
            Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(AuthService::class.java)
    }
}
