package com.example.homefitness.ui.screen.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.ExerciseRepositoryImpl
import com.example.homefitness.data.Plan
import com.example.homefitness.data.PlanRepositoryImpl
import com.example.homefitness.data.PlanState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BrowsePlanListViewModel (
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl
) : ViewModel(){

    val browsePlanListUiState: StateFlow<BrowsePlanListUiState> =
        planRepositoryImpl.getPlanByStateStream(PlanState.BROWSE).map {
            BrowsePlanListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = BrowsePlanListUiState.Initial
        )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface BrowsePlanListUiState{
    object Initial: BrowsePlanListUiState
    data class Success(val planList: List<Plan> = listOf()): BrowsePlanListUiState
}