package com.example.homefitness.data

import kotlinx.coroutines.flow.Flow

class ExerciseRepositoryImpl(private val exerciseDao: ExerciseDao) : ExerciseRepository{

    override fun getAllExercisesStream(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    override fun getExerciseStream(id: Int): Flow<Exercise?> = exerciseDao.getExercise(id)

    override suspend fun insertExercise(exercise: Exercise) = exerciseDao.insert(exercise)

    override suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    override suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    fun getExercisesByPlanStream(planId: Int): Flow<List<Exercise>> = exerciseDao.getExercisesByPlan(planId)

    fun getExercisesByPlanOrderStream(planId: Int, runOrder: Int): Flow<List<Exercise>> = exerciseDao.getExercisesByPlanOrder(planId,runOrder)
}