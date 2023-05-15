package com.example.homefitness.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * from plans ORDER BY name ASC")
    fun getAllPlans(): Flow<List<Plan>>

    @Query("SELECT * from plans WHERE planId = :id")
    fun getPlan(id: Int): Flow<Plan>

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(plan: Plan)

    @Update
    suspend fun update(plan: Plan)

    @Delete
    suspend fun delete(plan: Plan)

    @Query("SELECT * from plans WHERE planState = :planState")
    fun getPlanByState(planState: PlanState): Flow<List<Plan>>

}