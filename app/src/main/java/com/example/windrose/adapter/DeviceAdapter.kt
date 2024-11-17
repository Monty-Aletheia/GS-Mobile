package com.example.windrose.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.windrose.databinding.DeviceItemBinding
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UserDeviceResponseDTO

class DeviceAdapter (val deviceList: List<DeviceDTO>, val onClick: (String) -> Unit) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: DeviceItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(device: DeviceDTO){
            binding.deviceNameTextView.text = device.name
            binding.categoryTextView.text = device.category
            binding.deviceItemCardView.setOnClickListener{
                onClick(device.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DeviceItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = deviceList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = deviceList[position]

        holder.bind(item)

    }

}