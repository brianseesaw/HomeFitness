package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllExercisesStream(): Flow<List<Exercise>>

    /**
     * Retrieve an exercise from the given data source that matches with the [id].
     */
    fun getExerciseStream(id: Int): Flow<Exercise?>

    /**
     * Insert exercise in the data source
     */
    suspend fun insertExercise(exercise: Exercise)

    /**
     * Delete exercise from the data source
     */
    suspend fun deleteExercise(exercise: Exercise)

    /**
     * Update exercise in the data source
     */
    suspend fun updateExercise(exercise: Exercise)
}