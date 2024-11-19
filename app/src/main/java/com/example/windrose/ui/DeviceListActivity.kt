package com.example.windrose.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.windrose.R
import com.example.windrose.adapter.DeviceAdapter
import com.example.windrose.databinding.ActivityDeviceListBinding
import com.example.windrose.databinding.DeviceDetailsBottomSheetBinding
import com.example.windrose.network.API
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UserDeviceDTO
import com.example.windrose.network.UserDeviceRemoveDTO
import com.example.windrose.repository.UserRepository.getAllUsersDevices
import com.example.windrose.repository.UserRepository.getUserIdByFirebaseUid
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        binding.recyclerView.isVisible = false
        binding.progressBarRecyclerView.isVisible = true
        binding.noListTextView1.isVisible = false
        binding.noListTextView2.isVisible = false
        binding.noListTextView3.isVisible = false
        binding.imageView2.isVisible = false
        binding.refreshIconImageView.isVisible = false
        binding.tryAgainButton.isVisible = false

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
        val list = getAllUsersDevicesForRecyclerView(userId, this@DeviceListActivity)


        if (list == null){
            binding.refreshIconImageView.isVisible = true
            binding.tryAgainButton.isVisible = true
            binding.tryAgainButton.setOnClickListener{
                this@DeviceListActivity.recreate()
            }

        } else if(list.isEmpty()){
            binding.progressBarRecyclerView.isVisible = false
            binding.noListTextView1.isVisible = true
            binding.noListTextView2.isVisible = true
            binding.noListTextView3.isVisible = true
            binding.imageView2.isVisible = true
        }

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this@DeviceListActivity)
        recyclerView.adapter = DeviceAdapter(list!!, { deviceId ->
            for (device in list) {
                if (device.id == deviceId) {
                    showDeviceDetailsBottomSheet(device)
                }
            }
        }, { device ->
            showDeleteConfirmationDialog(device, userId)
        })


    }


    private fun showDeleteConfirmationDialog(device: DeviceDTO, userid: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Excluir ${device.name}?")
            .setMessage("Tem certeza que deseja excluir o dispositivo?")
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
                Toast.makeText(
                    this@DeviceListActivity,
                    "Dispositivo excluído com sucesso!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@DeviceListActivity,
                    "Falha ao excluir o dispositivo",
                    Toast.LENGTH_LONG
                ).show()
            }


        } catch (ex: Exception) {
            Toast.makeText(this@DeviceListActivity, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun getAllUsersDevicesForRecyclerView(userId: String, context: Context): List<DeviceDTO>? {
        try {
            val buildService = API.buildUserDeviceService()
            val response = buildService.getUserDevicesById(userId)

            if (response.isSuccessful) {
                val devices = response.body()!!.content
                binding.recyclerView.isVisible = true
                binding.progressBarRecyclerView.isVisible = false
                return devices
            }

            return emptyList()

        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()

            return null
        }
    }


    private fun showDeviceDetailsBottomSheet(device: DeviceDTO) {
        val binding: DeviceDetailsBottomSheetBinding =
            DeviceDetailsBottomSheetBinding.inflate(layoutInflater)
        binding.deviceNameTextView.text = device.name
        binding.deviceCategoryTextView.text = device.category
        binding.wattageValueTextView.text = device.powerRating.toString()
        binding.dailyUsageValue.text = device.estimatedUsageHours.toString()

        binding.editPencilImageView.setOnClickListener {
            showEditEstimatedHourDialog(device)
        }

        val dailyWaste = (device.powerRating / 1000) * device.estimatedUsageHours
        binding.dailyWasteValue.text = dailyWaste.toString()
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(binding.root)
        dialog.show()

    }

    private fun showEditEstimatedHourDialog(device: DeviceDTO) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_update_estimated_hour, null)
        val newEstimatedHourEditText: EditText = view.findViewById(R.id.editTextNumberDecimal)


        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Alterar tempo de uso")
            .setView(view)
            .setPositiveButton("Confirmar") { dialogInterface, i ->
                val newEstimatedHour = newEstimatedHourEditText.text.toString().toDouble()
                updateUserEstimatedHour(device, newEstimatedHour)
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancelar") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun updateUserEstimatedHour(device: DeviceDTO, newEstimatedHour: Double) =
        lifecycleScope.launch {
            val firebaseUid = auth.currentUser!!.uid
            val user = getUserIdByFirebaseUid(firebaseUid)
            val userId = user!!.id
            val deviceId = device.id
            val userDeviceDto = UserDeviceDTO(deviceId, newEstimatedHour)
            try {
                val buildService = API.buildUserDeviceService()
                val response =
                    buildService.updateUserDeviceEstimatedHour(userId, deviceId, userDeviceDto)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@DeviceListActivity,
                        "Horas de uso atualizadas com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    this@DeviceListActivity.recreate()

                } else {
                    Toast.makeText(
                        this@DeviceListActivity,
                        "Não foi possivel alterar seu dado.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (ex: Exception) {
                Toast.makeText(this@DeviceListActivity, ex.message, Toast.LENGTH_LONG).show()
            }
        }

}