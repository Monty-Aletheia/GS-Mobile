package com.example.windrose.utils

import com.github.mikephil.charting.formatter.ValueFormatter

class CustomPercentFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return "${value.toInt()}%"
    }
}