package com.example.windrose.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.example.windrose.R
import com.example.windrose.databinding.ActivityProfileBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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

        binding.profileNameTextView.text = "Olá ${auth.currentUser!!.displayName.toString()}"

        binding.imageView3.setOnClickListener{
            auth.signOut()
            findNavController(R.id.fragmentContainerView).navigate(R.id.loginFragment)
        }

        setPieChartConfig()




    }

    private fun setPieChartConfig() {
        val list: ArrayList<PieEntry> = ArrayList()

        list.add(PieEntry(100f, "Geladeira"))
        list.add(PieEntry(101f, "Fogão"))
        list.add(PieEntry(102f, "Micro-ondas"))
        list.add(PieEntry(103f, "Tv"))
        list.add(PieEntry(107f, "Torradeira"))

        val pieDataSet = PieDataSet(list, "")

        val colors = arrayListOf(
            Color.parseColor("#4CAF50"), // Verde (ex: Geladeira)
            Color.parseColor("#FFEB3B"), // Amarelo (ex: Microondas)
            Color.parseColor("#FF9800"), // Laranja (ex: Televisão)
            Color.parseColor("#F44336"), // Vermelho (ex: Fogão)
            Color.parseColor("#00BCD4")  // Azul (ex: Outros)
        )

        pieDataSet.colors = colors

        pieDataSet.valueTextSize = 10f


        pieDataSet.valueTextColor = Color.BLACK

        val pieData = PieData(pieDataSet)

        pieChart.data = pieData

        pieChart.description.text = ""

        pieChart.centerText = "Consumo"

        pieChart.animateY(2000)

        pieChart.legend.isEnabled = true
        pieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT

        pieChart.setEntryLabelTextSize(7f)


        pieChart.invalidate()
    }


}