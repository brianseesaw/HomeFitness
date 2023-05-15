package com.example.homefitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.*

@Entity(tableName = "exercise_runs")
data class ExerciseRun(
    @PrimaryKey(autoGenerate = true) val runId: Int = 0,
    var order: Int = 0,
    var repDone:Int = 0,
    val planId: Int = 0,
    var date: Date = Date(),
    var runState: RunState = RunState.NEW,
)

enum class RunState{
    DONE, PAUSED, NEW, CONFIGURED, CHANGED
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}