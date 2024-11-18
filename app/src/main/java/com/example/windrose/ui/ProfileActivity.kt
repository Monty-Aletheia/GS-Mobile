package com.example.windrose.ui

import android.content.Intent
import android.graphics.Color
import com.example.windrose.repository.UserRepository.getAllUsersDevices
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.windrose.R
import com.example.windrose.databinding.ActivityProfileBinding
import com.example.windrose.repository.UserRepository.getUserIdByFirebaseUid
import com.example.windrose.utils.CustomPercentFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

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

        val username: String = auth.currentUser!!.displayName.toString().split(" ")[0]
        binding.profileNameTextView.text = "Olá ${username}"

        binding.signOutImageView.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }


        binding.buttonToYourDevices.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }



    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null){
            finish()
        }

        lifecycleScope.launch {
            setPieChartConfig()
        }
    }



    private suspend fun setPieChartConfig() {


        val user = getUserIdByFirebaseUid(auth.currentUser!!.uid)
        val devices = getAllUsersDevices(user!!.id, this)

        if (devices.isEmpty()) {
            val list: ArrayList<PieEntry> = ArrayList()
            list.add(PieEntry(100f, ""))

            val pieDataSet = PieDataSet(list, "")
            val colors = arrayListOf(Color.parseColor("#9E9E9E"))
            pieDataSet.colors = colors
            pieDataSet.valueTextSize = 0f  // Remove o texto das fatias


            val pieData = PieData(pieDataSet)

            pieChart.description.text = ""
            pieChart.legend.isEnabled = false
            pieChart.data = pieData
            pieChart.centerText = "Sem Dados"
            pieChart.animateY(2000)
            pieChart.invalidate()

        } else {

            val sortedDevices = devices.sortedByDescending { it.consumption }


            val topDevices = sortedDevices.take(5)
            val othersDevices = sortedDevices.drop(5)


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


}