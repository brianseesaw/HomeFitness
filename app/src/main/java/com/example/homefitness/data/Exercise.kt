package com.example.homefitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val exerciseId: Int = 0,
    val name: String = "",
    val rep:Int = 0,
    val set:Int = 0,
    var type: ExerciseType = ExerciseType.REP,
    var runOrder:Int = 0,
    val planId: Int= 0,
    val calorie: Float= 0f
    )

enum class ExerciseType{
    REP,TIME
}

fun Exercise.toExerciseScreen():Int{
    val detectorExercises = listOf("pushup","plank","lunge","squat")
    val landscapeExercises = listOf("pushup","plank")
    if(this.name in detectorExercises){
        if (this.type == ExerciseType.TIME){
            if (this.name in landscapeExercises){
                return 6
            }else{
                return 5
            }
        }
        else if (this.type == ExerciseType.REP){
            if (this.name in landscapeExercises){
                return 4
            }else{
                return 3
            }
        }
    }else{
        if (this.type == ExerciseType.TIME){
            return 2
        }
        else if (this.type == ExerciseType.REP){
            return 1
        }
    }
    return 0
}

