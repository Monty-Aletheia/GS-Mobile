package com.example.windrose.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.emptyObjectList
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.windrose.R
import com.example.windrose.adapter.DeviceAdapter
import com.example.windrose.databinding.ActivityDeviceListBinding
import com.example.windrose.databinding.DeviceDetailsBottomSheetBinding
import com.example.windrose.network.API
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UserDeviceRemoveDTO
import com.example.windrose.network.UserResponse
import com.example.windrose.network.UserResponseDTO
import com.example.windrose.repository.UserRepository.getAllUsersDevices
import com.example.windrose.repository.UserRepository.getUserIdByFirebaseUid
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class DeviceListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceListBinding;
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = Firebase.auth

        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.floatingActionButton.setOnClickListener {
            val intent = Intent(this, DeviceFinderActivity::class.java)
            startActivity(intent)
        }
        binding.backArrowImageView.setOnClickListener {
            super.finish()
        }

        loadAllDevices()


    }

    private fun loadAllDevices() = lifecycleScope.launch(Dispatchers.Main) {

        val firebaseUid = auth.currentUser!!.uid
        val userId = getUserIdByFirebaseUid(firebaseUid)!!.id
        val list = getAllUsersDevices(userId, this@DeviceListActivity)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this@DeviceListActivity)
        recyclerView.adapter = DeviceAdapter(list, { deviceId ->
            for (device in list) {
                if (device.id == deviceId) {
                    showDeviceDetailsBottomSheet(device)
                }
            }
        }, { device ->
            showDeleteConfirmationDialog(device, userId)
        })


    }


    private fun showDeleteConfirmationDialog(device: DeviceDTO, userid: String){
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Tem certeza?")
            .setMessage("Tem certeza que deseja excluir o dispositivo ${device.name}?")
            .setPositiveButton("Confirmar") { dialog, _ ->
                val deviceId = device.id
                deleteDevice(deviceId, userid)
                loadAllDevices()
                dialog.dismiss()

            }
            .setNegativeButton("Voltar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun deleteDevice(deviceId: String, userid: String) = lifecycleScope.launch {
        try {

            val buildService = API.buildUserDeviceService()
            val list = listOf(deviceId)
            val userDeviceRemoveDto = UserDeviceRemoveDTO(list)
            val response = buildService.deleteUserDevice(userid, userDeviceRemoveDto)

            if (response.isSuccessful) {
                Toast.makeText(this@DeviceListActivity, "Dispositivo excluído com sucesso!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@DeviceListActivity, "Falha ao excluir o dispositivo", Toast.LENGTH_LONG).show()
            }


        } catch (ex: Exception){
            Toast.makeText(this@DeviceListActivity, ex.message, Toast.LENGTH_LONG).show()
        }
    }





    private fun showDeviceDetailsBottomSheet(device: DeviceDTO) {
        val binding: DeviceDetailsBottomSheetBinding =
            DeviceDetailsBottomSheetBinding.inflate(layoutInflater)
        binding.deviceNameTextView.text = device.name
        binding.deviceCategoryTextView.text = device.category
        binding.wattageValueTextView.text = device.powerRating.toString()
        binding.dailyUsageValue.text = device.estimatedUsageHours.toString()

        val dailyWaste = (device.powerRating / 1000) * device.estimatedUsageHours
        binding.dailyWasteValue.text = dailyWaste.toString()
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(binding.root)
        dialog.show()

    }
}