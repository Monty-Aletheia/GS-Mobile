package com.example.windrose.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.windrose.R
import com.example.windrose.adapter.DeviceAdapter
import com.example.windrose.databinding.ActivityDeviceListBinding
import com.example.windrose.databinding.DeviceDetailsBottomSheetBinding
import com.example.windrose.network.DeviceDTO
import com.google.android.material.bottomsheet.BottomSheetDialog

class DeviceListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceListBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.floatingActionButton.setOnClickListener{
            val intent = Intent(this, DeviceFinderActivity::class.java)
            startActivity(intent)
        }
        binding.backArrowImageView.setOnClickListener{
            super.finish()
        }


        // Criação dos objetos DeviceDTO
        val device1 = DeviceDTO(
            id = "1",
            name = "Smart TV",
            category = "Eletrônicos",
            model = "Samsung 55Q70A",
            powerRating = 150.0,          // Consumo de energia em Watts
            estimatedUsageHours = 5.0     // Horas de uso estimado por dia
        )

        val device2 = DeviceDTO(
            id = "2",
            name = "Geladeira",
            category = "Eletrodomésticos",
            model = "Brastemp Inverse",
            powerRating = 200.0,          // Consumo de energia em Watts
            estimatedUsageHours = 10.0    // Horas de uso estimado por dia
        )

        val list = listOf(device1, device2)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DeviceAdapter(list) { deviceId ->
            for(device in list){
                if(device.id == deviceId){
                    showDeviceDetailsBottomSheet(device)
                }
            }
        }

    }

    private fun showDeviceDetailsBottomSheet(device: DeviceDTO){
        val binding: DeviceDetailsBottomSheetBinding =
            DeviceDetailsBottomSheetBinding.inflate(layoutInflater)
            binding.deviceNameTextView.text = device.name
            binding.deviceCategoryTextView.text = device.category
            binding.wattageValueTextView.text = device.powerRating.toString()
            binding.dailyUsageValue.text = device.estimatedUsageHours.toString()

            val dailyWaste =  (device.powerRating / 1000) * device.estimatedUsageHours
            binding.dailyWasteValue.text = dailyWaste.toString()
            val dialog = BottomSheetDialog(this)
            dialog.setContentView(binding.root)
            dialog.show()

    }
}