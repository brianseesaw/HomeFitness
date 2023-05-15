package com.example.homefitness.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseRunDao {

    @Query("SELECT * from exercise_runs ORDER BY planId ASC")
    fun getAllExerciseRuns(): Flow<List<ExerciseRun>>

    @Query("SELECT * from exercise_runs WHERE runId = :id")
    fun getExerciseRun(id: Int): Flow<ExerciseRun>

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exerciseRun: ExerciseRun)

    @Update
    suspend fun update(exerciseRun: ExerciseRun)

    @Delete
    suspend fun delete(exerciseRun: ExerciseRun)

    @Query("SELECT * from exercise_runs WHERE planId = :planId")
    fun getExerciseRunsByPlan(planId: Int): Flow<List<ExerciseRun>>

    @Query("SELECT * from exercise_runs WHERE runState = :runState")
    fun getExerciseRunsByState(runState: RunState): Flow<List<ExerciseRun>>
}