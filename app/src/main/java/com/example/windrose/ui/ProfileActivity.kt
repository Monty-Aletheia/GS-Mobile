package com.example.windrose.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import com.example.windrose.repository.UserRepository.getAllUsersDevices
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.windrose.R
import com.example.windrose.databinding.ActivityProfileBinding
import com.example.windrose.network.API
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UpdateUserDTO
import com.example.windrose.repository.UserRepository.getUserIdByFirebaseUid
import com.example.windrose.utils.CustomPercentFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding;
    private lateinit var auth: FirebaseAuth
    private lateinit var pieChart: PieChart



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = Firebase.auth
        binding = ActivityProfileBinding.inflate(layoutInflater)
        pieChart = binding.pieChart
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.pieChart.isVisible = false
        binding.progressBar.isVisible = true


        lifecycleScope.launch {

            setPieChartConfig()
        }

        val username: String = auth.currentUser!!.displayName.toString().split(" ")[0]
        binding.profileNameTextView.text = "Olá ${username}"

        binding.editPencil.setOnClickListener {
            showDialog()
        }

        binding.signOutImageView.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }


        binding.yourDevicesCardView.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }

        lifecycleScope.launch {
            val user = getUserIdByFirebaseUid(auth.currentUser!!.uid)
            val devices = getAllUsersDevices(user!!.id, this@ProfileActivity)
            val (kWattsPerDay, kWattsPerMonth) = calculateEnergyConsumption(devices)

            binding.dailyWasteWattTextView.text = kWattsPerDay.toString()
            binding.monthWasteTextView.text = kWattsPerMonth.toString()
        }

    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            setPieChartConfig()
        }

        lifecycleScope.launch {
            val user = getUserIdByFirebaseUid(auth.currentUser!!.uid)
            val devices = getAllUsersDevices(user!!.id, this@ProfileActivity)
            val (kWattsPerDay, kWattsPerMonth) = calculateEnergyConsumption(devices)

            binding.dailyWasteWattTextView.text = kWattsPerDay.toString()
            binding.monthWasteTextView.text = kWattsPerMonth.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            finish()
        }
    }

    private fun showDialog(){
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_update_username, null)
        val newNameEditText: EditText = view.findViewById(R.id.newNameEditText)
        val passwordEditText: EditText = view.findViewById(R.id.passwordEditText)


        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Alterar nome")
            .setView(view)
            .setPositiveButton("Confirmar") { dialogInterface, i ->
                val newUserName = newNameEditText.text.toString()
                val password = passwordEditText.text.toString()
                updateUserName(newUserName, password)
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancelar"){ dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create()

        alertDialog.show()
    }


    private fun updateUserName(newUserName: String, userPassword: String) = lifecycleScope.launch {
        try {
            val user = auth.currentUser!!
            val userFound = getUserIdByFirebaseUid(user.uid)
            val userId = userFound!!.id
            val email = user.email!!
            val updateUserDTO = UpdateUserDTO(newUserName, email, userPassword, user.uid)
            val buildService = API.buildUserService()
            val response = buildService.updateUserName(userId, updateUserDTO)

            if (response.isSuccessful){
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newUserName)
                    .build()
                user.updateProfile(profileUpdates).await()

                Toast.makeText(this@ProfileActivity, "Nome alterado com sucesso!", Toast.LENGTH_LONG).show()
                this@ProfileActivity.recreate()
            }

        } catch (ex: Exception) {
            Toast.makeText(this@ProfileActivity, ex.message, Toast.LENGTH_LONG).show()
        }

    }



    private fun calculateEnergyConsumption(devices: List<DeviceDTO>): Pair<Double, Double> {
        var totalWattsPerDay = 0.0

        devices.forEach { device ->
            val dailyConsumption = device.powerRating * device.estimatedUsageHours
            totalWattsPerDay += dailyConsumption
        }

        val totalKWPerDay = totalWattsPerDay / 1000
        val totalKWPerMonth = (totalWattsPerDay * 30) / 1000

        val roundedKWPerDay = String.format("%.2f", totalKWPerDay).toDouble()
        val roundedKWPerMonth = String.format("%.2f", totalKWPerMonth).toDouble()

        return Pair(roundedKWPerDay, roundedKWPerMonth)
    }


    private suspend fun setPieChartConfig() {

        data class Device(
            val name: String,
            val consumption: Double
        )

        val user = getUserIdByFirebaseUid(auth.currentUser!!.uid)
        val devices = getAllUsersDevicesToPie(user!!.id, this)

        if (devices.isEmpty()) {
            val list: ArrayList<PieEntry> = ArrayList()
            list.add(PieEntry(100f, ""))

            val pieDataSet = PieDataSet(list, "")
            val colors = arrayListOf(Color.parseColor("#9E9E9E"))
            pieDataSet.colors = colors
            pieDataSet.valueTextSize = 0f


            val pieData = PieData(pieDataSet)

            pieChart.description.text = ""
            pieChart.legend.isEnabled = false
            pieChart.data = pieData
            pieChart.centerText = "Sem Dados"
            pieChart.animateY(2000)
            pieChart.invalidate()

        } else {

            val groupedDevices = devices
                .groupBy { it.name }
                .mapValues { entry ->
                    entry.value.sumOf { it.consumption }
                }
                .map { (name, totalConsumption) ->
                    Device(name, totalConsumption)
                }

            val sortedDevices = groupedDevices.sortedByDescending { it.consumption }


            val topDevices = sortedDevices.take(4)
            val othersDevices = sortedDevices.drop(4)


            val totalConsumption = sortedDevices.sumOf { it.consumption }
            val othersConsumption = othersDevices.sumOf { it.consumption }


            val list: ArrayList<PieEntry> = ArrayList()

            topDevices.forEach {
                val percentual = (it.consumption / totalConsumption) * 100
                list.add(PieEntry(percentual.toFloat(), it.name))
            }

            if (othersDevices.isNotEmpty()) {
                val othersPercentual = (othersConsumption / totalConsumption) * 100
                list.add(PieEntry(othersPercentual.toFloat(), "OUTROS"))
            }

            val pieDataSet = PieDataSet(list, "")

            val colors = arrayListOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336"),
                Color.parseColor("#00BCD4")
            )

            colors.add(Color.parseColor("#9E9E9E"))

            pieDataSet.colors = colors

            pieDataSet.valueTextSize = 14f

            pieDataSet.valueTextColor = Color.WHITE

            pieDataSet.valueFormatter = CustomPercentFormatter()


            val pieData = PieData(pieDataSet)

            pieChart.data = pieData


            pieChart.description.text = ""

            pieChart.centerText = "Consumo"

            pieChart.animateY(2000)

            pieChart.legend.isEnabled = true
            pieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
            pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
            pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT

            pieChart.legend.textSize = 16f

            pieChart.setDrawEntryLabels(false)

            pieChart.invalidate()
        }
    }

    private suspend fun getAllUsersDevicesToPie(userId: String, context: Context): List<DeviceDTO> {
        try {
            val buildService = API.buildUserDeviceService()
            val response = buildService.getUserDevicesById(userId)

            if (response.isSuccessful) {
                val devices = response.body()!!.content
                binding.pieChart.isVisible = true
                binding.progressBar.isVisible = false
                return devices
            } else {
                Log.e("API_ERROR", "Failed to fetch consultations")
                return emptyList()
            }


        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
            return emptyList()
        }
    }


}