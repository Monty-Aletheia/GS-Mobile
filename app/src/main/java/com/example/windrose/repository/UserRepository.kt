package com.example.windrose.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.windrose.network.API
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UserResponseDTO

object UserRepository {

    suspend fun getUserIdByFirebaseUid(firebaseUid: String): UserResponseDTO? {
        try {
            val buildService = API.buildUserService()
            val response = buildService.getUserByFirebaseUid(firebaseUid)

            if (response.isSuccessful) {
                val user = response.body()!!
                return user
            } else {
                Log.i("API_ERROR", "Failed to fetch consultations")
                return null
            }

        } catch (ex: Exception) {
            Log.e("API_ERROR", ex.message ?: "Unknown error")
            return null
        }
    }

     suspend fun getAllUsersDevices(userId: String, context: Context): List<DeviceDTO> {
        try {
            val buildService = API.buildUserDeviceService()
            val response = buildService.getUserDevicesById(userId)

            if (response.isSuccessful) {
                val devices = response.body()!!.content
                return devices
            } else {
                Log.i("API_ERROR", "Failed to fetch consultations")
                return emptyList()
            }


        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
            return emptyList()
        }
    }
}