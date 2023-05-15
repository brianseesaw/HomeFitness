package com.example.homefitness.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * from exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * from exercises WHERE exerciseId = :id")
    fun getExercise(id: Int): Flow<Exercise>

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise)

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT * from exercises WHERE planId = :planId ORDER BY runOrder ASC")
    fun getExercisesByPlan(planId: Int): Flow<List<Exercise>>

    @Query("SELECT * from exercises WHERE planId = :planId AND runOrder = :runOrder")
    fun getExercisesByPlanOrder(planId: Int,runOrder: Int): Flow<List<Exercise>>


}