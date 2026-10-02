package com.example.tasktracker.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromInts(v: List<Int>): String = v.joinToString(",")
    @TypeConverter fun toInts(s: String): List<Int> =
        if (s.isBlank()) emptyList() else s.split(",").mapNotNull { it.trim().toIntOrNull() }
}
