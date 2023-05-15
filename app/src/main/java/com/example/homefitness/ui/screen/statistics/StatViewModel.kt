package com.example.homefitness.ui.screen.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

class StatViewModel(
private val planRepositoryImpl: PlanRepositoryImpl,
private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl
) : ViewModel(){

    val statUiState: StateFlow<StatUiState> =
        exerciseRunRepositoryImpl.getAllExerciseRunsStream().map {
            val run = it.filter { it.runState in listOf(RunState.DONE,RunState.PAUSED) }
            val tdy = run.filter {it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(LocalDate.now())}
            val wk = run.filter {it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now().minusWeeks(1))}
            val mth = run.filter {it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(LocalDate.now().minusDays(30))}
            val monthStat = extStat(mth)
            val weekStat = extStat(wk)
            val dayStat = extStat(tdy)

            StatUiState.Success(
                StatData(
                    dayStat,
                    weekStat,
                    monthStat,
                )
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = StatUiState.Initial
            )

    suspend fun extStat(runList:List<ExerciseRun>):Stat{
        val planCount = runList.count{it.runState == RunState.DONE}
        var exCount = 0
        var calSum = 0f
        runList.forEach {run->
            exerciseRepositoryImpl.getExercisesByPlanStream(run.planId)
                .filterNotNull()
                .first()
                .let{ exList->
                    if (run.runState == RunState.DONE){
                        exCount += exList.count() // if run done, add all ex count
                    }
                    if (run.runState == RunState.PAUSED){
                        exCount += run.order-1 // if run paused, add done ex count
                    }
                    exList.forEach { ex->
                        if (run.runState == RunState.DONE){
                            calSum += ex.calorie*ex.rep*ex.set // if run done, add total rep*cal
                        }
                        if (run.runState == RunState.PAUSED){ // if run paused
                            if(ex.runOrder < run.order){
                                calSum += ex.calorie*ex.rep*ex.set // if ex, done total rep*cal
                            }
                            if (ex.runOrder == run.order){
                                calSum += ex.calorie*run.repDone // if doing ex, repdone*cal
                            }
                        }
                    }
                }
        }
        return Stat(planCount,exCount,calSum)
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}



sealed interface StatUiState{
    object Initial: StatUiState
    data class Success(val data: StatData = StatData()) : StatUiState
}
data class StatData(
    val dayStat : Stat = Stat(),
    val weekStat: Stat = Stat(),
    val monthStat: Stat = Stat(),
)

data class Stat(
    val planCount: Int = 0,
    val exCount: Int = 0,
    val calSum: Float = 0f,
)