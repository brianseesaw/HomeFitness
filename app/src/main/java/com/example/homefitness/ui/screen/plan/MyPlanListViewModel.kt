package com.example.homefitness.ui.screen.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.ExerciseRepositoryImpl
import com.example.homefitness.data.Plan
import com.example.homefitness.data.PlanRepositoryImpl
import com.example.homefitness.data.PlanState
import com.example.homefitness.ui.screen.TopBarState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MyPlanListViewModel(
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl
) : ViewModel(){

    val myPlanListUiState: StateFlow<MyPlanListUiState> =
        planRepositoryImpl.getPlanByStateStream(PlanState.MY).map {
            MyPlanListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = MyPlanListUiState.Initial
        )


    fun onItemClick(id:Int){

    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface MyPlanListUiState{
    object Initial: MyPlanListUiState
    data class Success(val planList: List<Plan> = listOf()): MyPlanListUiState
}