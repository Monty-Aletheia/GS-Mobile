package com.example.windrose.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.example.windrose.R
import com.example.windrose.databinding.ActivityMainBinding
import com.example.windrose.databinding.ActivityProfileBinding
import com.example.windrose.utils.CustomPercentFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

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

        binding.signOutImageView.setOnClickListener{
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.buttonToYourDevices.setOnClickListener{
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }

        setPieChartConfig()




    }



    private fun setPieChartConfig() {
        val list: ArrayList<PieEntry> = ArrayList()


        list.add(PieEntry(50f, "Geladeira"))
        list.add(PieEntry(20f, "Fogão"))
        list.add(PieEntry(20f, "Micro-ondas"))
        list.add(PieEntry(5f, "Tv"))
        list.add(PieEntry(5f, "Torradeira"))

        val pieDataSet = PieDataSet(list, "")

        val colors = arrayListOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#F44336"),
            Color.parseColor("#00BCD4")
        )

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