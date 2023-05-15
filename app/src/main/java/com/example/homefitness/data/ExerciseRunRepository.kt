package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

interface ExerciseRunRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllExerciseRunsStream(): Flow<List<ExerciseRun>>

    /**
     * Retrieve an exerciseRun from the given data source that matches with the [id].
     */
    fun getExerciseRunStream(id: Int): Flow<ExerciseRun?>

    /**
     * Insert exerciseRun in the data source
     */
    suspend fun insertExerciseRun(exerciseRun: ExerciseRun)

    /**
     * Delete exerciseRun from the data source
     */
    suspend fun deleteExerciseRun(exerciseRun: ExerciseRun)

    /**
     * Update exerciseRun in the data source
     */
    suspend fun updateExerciseRun(exerciseRun: ExerciseRun)
}