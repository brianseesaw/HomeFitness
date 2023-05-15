package com.example.homefitness.ui.screen.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowseExerciseListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
): ViewModel() {

    private val planIdArg: Int = checkNotNull(savedStateHandle[BrowseExListDestination.planIdArg])


    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    private val _order = MutableStateFlow(0)
    val order = _order.asStateFlow()

    private var _addBrowsePlanUiState = MutableStateFlow<AddBrowsePlanUiState>(AddBrowsePlanUiState.Initial)
    val addBrowsePlanUiState = _addBrowsePlanUiState.asStateFlow()

    val exerciseListUiState: StateFlow<BrowseExListUiState> =
        exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg).map {
            BrowseExListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = BrowseExListUiState.Initial
        )
    
    fun addToMyPlan(){
        viewModelScope.launch {
            planRepositoryImpl.getPlanStream(planIdArg)// get plan name
                .filterNotNull()
                .first()
                .let { plan->
                    planRepositoryImpl.insertPlan(Plan(name = plan.name, planState = PlanState.EMPTY))
                }
            planRepositoryImpl.getPlanByStateStream(PlanState.EMPTY)
                .filterNotNull()
                .first()
                .firstOrNull()
                ?.let { plan->
                    exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg)
                    .filterNotNull()
                    .first()
                    .let { res->
                        res.forEach {
                            exerciseRepositoryImpl.insertExercise(
                                Exercise(
                                    name = it.name,
                                    rep = it.rep,
                                    set = it.set,
                                    type = it.type,
                                    runOrder = it.runOrder,
                                    planId = plan.planId,
                                    calorie = it.calorie
                                )
                            )
                        }
                    }
                    planRepositoryImpl.updatePlan(plan.copy(planState = PlanState.MY))
                }
        }.invokeOnCompletion {
            _addBrowsePlanUiState.update { AddBrowsePlanUiState.Added }
        }
    }
    
    init {
        viewModelScope.launch {
            planRepositoryImpl.getPlanStream(planIdArg)// get plan name
                .filterNotNull()
                .first()
                .let { plan->
                    _planName.update { plan.name }
                }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}


sealed interface BrowseExListUiState{
    object Initial: BrowseExListUiState
    data class Success(val exerciseList: List<Exercise> = listOf()): BrowseExListUiState
}

sealed interface AddBrowsePlanUiState{
    object Initial: AddBrowsePlanUiState
    object Added: AddBrowsePlanUiState
    object Adding: AddBrowsePlanUiState
}