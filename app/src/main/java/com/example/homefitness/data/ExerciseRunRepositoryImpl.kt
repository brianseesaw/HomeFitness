package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

class ExerciseRunRepositoryImpl(private val exerciseRunDao: ExerciseRunDao) : ExerciseRunRepository{

    override fun getAllExerciseRunsStream(): Flow<List<ExerciseRun>> = exerciseRunDao.getAllExerciseRuns()

    override fun getExerciseRunStream(id: Int): Flow<ExerciseRun?> = exerciseRunDao.getExerciseRun(id)

    override suspend fun insertExerciseRun(exerciseRun: ExerciseRun) = exerciseRunDao.insert(exerciseRun)

    override suspend fun deleteExerciseRun(exerciseRun: ExerciseRun) = exerciseRunDao.delete(exerciseRun)

    override suspend fun updateExerciseRun(exerciseRun: ExerciseRun) = exerciseRunDao.update(exerciseRun)

    fun getExerciseRunsByPlanStream(planId: Int): Flow<List<ExerciseRun>> = exerciseRunDao.getExerciseRunsByPlan(planId)

    fun getExerciseRunsByStateStream(runState: RunState): Flow<List<ExerciseRun>> = exerciseRunDao.getExerciseRunsByState(runState)
}