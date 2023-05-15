package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

class PlanRepositoryImpl(private val planDao: PlanDao):PlanRepository {
    override fun getAllPlansStream(): Flow<List<Plan>> = planDao.getAllPlans()

    override fun getPlanStream(id: Int): Flow<Plan?> = planDao.getPlan(id)

    override suspend fun insertPlan(plan: Plan) = planDao.insert(plan)

    override suspend fun deletePlan(plan: Plan) = planDao.delete(plan)

    override suspend fun updatePlan(plan: Plan) = planDao.update(plan)

    fun getPlanByStateStream(planState: PlanState): Flow<List<Plan>>  = planDao.getPlanByState(planState)
}