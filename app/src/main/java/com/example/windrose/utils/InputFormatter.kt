package com.example.windrose.utils

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.EditText
import android.widget.Toast

object InputFormatter {

    fun inputTextToEstimatedHours(inputEstimatedUsageHours: String): Double {
        val parts = inputEstimatedUsageHours.split(":")
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val estimatedUsageHours = hours + (minutes / 60.0)
        return estimatedUsageHours
    }

    fun openTimePickerDialog(diaryUsageInput: EditText, context: Context) {
        val hourOfDay = 23
        val minute = 55
        val is24HourView = true

        val timePickerDialog = TimePickerDialog(
            context,
            android.R.style.Theme_Holo_Light_Dialog,
            { _, hourOfDay, minute ->
                diaryUsageInput.setText("${hourOfDay}:${minute}")
                Toast.makeText(context, "Houras=$hourOfDay Minutos=$minute", Toast.LENGTH_SHORT)
                    .show()
            },
            hourOfDay,
            minute,
            is24HourView
        )

        timePickerDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        timePickerDialog.setTitle("Selecione o tempo de uso:")
        timePickerDialog.show()
    }

    fun estimatedHoursToTextView(estimatedHour: Double): String{
        val hours = estimatedHour.toInt()
        val minutes = ((estimatedHour - hours) * 60).toInt()

        val formattedTime = String.format("${hours}h: ${minutes}m")

        return formattedTime
    }



}