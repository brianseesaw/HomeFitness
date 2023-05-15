package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllPlansStream(): Flow<List<Plan>>

    /**
     * Retrieve an plan from the given data source that matches with the [id].
     */
    fun getPlanStream(id: Int): Flow<Plan?>

    /**
     * Insert plan in the data source
     */
    suspend fun insertPlan(plan: Plan)

    /**
     * Delete plan from the data source
     */
    suspend fun deletePlan(plan: Plan)

    /**
     * Update plan in the data source
     */
    suspend fun updatePlan(plan: Plan)
}