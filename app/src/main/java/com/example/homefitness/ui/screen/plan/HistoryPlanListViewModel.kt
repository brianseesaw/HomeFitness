package com.example.homefitness.ui.screen.plan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class HistoryPlanListViewModel(
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl
) : ViewModel(){

    val historyPlanListUiState: StateFlow<HistoryPlanListUiState> =
        exerciseRunRepositoryImpl.getAllExerciseRunsStream().map {
            val planList: MutableList<RunWithPlan> = mutableListOf()
            val dateList: MutableList<LocalDate> = mutableListOf()
            val headerList: MutableList<Int> = mutableListOf()
            it.filter {
                it.runState in listOf(RunState.DONE,RunState.PAUSED)
//                &&
//                it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now())
            }.sortedByDescending {
                it.date
            }.forEachIndexed { index,run->
                planRepositoryImpl.getPlanStream(run.planId)
                    .filterNotNull()
                    .first()
                    .let { plan->
                        planList += RunWithPlan(run,plan)
                    }

                val date = run.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                if (!dateList.contains(date)){
                    dateList += date
                    headerList += index
                }
            }
            HistoryPlanListUiState.Success(planList.toList(),headerList.toList())
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = HistoryPlanListUiState.Initial
            )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface HistoryPlanListUiState{
    object Initial: HistoryPlanListUiState
    data class Success(val planList: List<RunWithPlan> = listOf(),val headerList: List<Int> = listOf()): HistoryPlanListUiState
}
