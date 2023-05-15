package com.example.homefitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val planId: Int = 0,
    val name:String = "",
//    var exercises :List<Exercise> ,
//    var date: Date = Date(),
    var planState: PlanState = PlanState.EMPTY
    )

enum class PlanState{
    MY,DONE,RUN,EMPTY,BROWSE,NEW_RUN
}

//class Converters {
//    @TypeConverter
//    fun fromTimestamp(value: Long?): Date? {
//        return value?.let { Date(it) }
//    }
//
//    @TypeConverter
//    fun dateToTimestamp(date: Date?): Long? {
//        return date?.time?.toLong()
//    }
//}