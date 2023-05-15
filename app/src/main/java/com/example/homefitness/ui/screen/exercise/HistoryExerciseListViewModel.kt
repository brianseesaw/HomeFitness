package com.example.homefitness.ui.screen.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryExerciseListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
): ViewModel() {

    private val runIdArg: Int = checkNotNull(savedStateHandle[HisExListDestination.runIdArg])
    private val planIdArg: Int = checkNotNull(savedStateHandle[HisExListDestination.planIdArg])


    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    private val _order = MutableStateFlow(0)
    val order = _order.asStateFlow()

    val exerciseListUiState: StateFlow<HisExListUiState> =
        exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg).map {exList->

            val res: MutableList<ExerciseWithProg> = mutableListOf()
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .filterNotNull()
                .first()
                .let {run->
                    _order.update { run.order }
                    exList.forEach {ex->
                        if (run.runState==RunState.DONE){
                            res.add(ExerciseWithProg(ex,1f))
                        } else if (run.runState==RunState.PAUSED){
                            if(run.order>ex.runOrder){
                                res.add(ExerciseWithProg(ex,1f)) // already done
                            }else if (run.order==ex.runOrder){
                                res.add(ExerciseWithProg(ex, run.repDone.toFloat()/(ex.set.toFloat()*ex.rep.toFloat()))) //doing
                            }else if (run.order<ex.runOrder){
                                res.add(ExerciseWithProg(ex,0f)) // not reached
                            }
                        }
                    }
                }

            HisExListUiState.Success(res)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = HisExListUiState.Initial
        )

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


sealed interface HisExListUiState{
    object Initial: HisExListUiState
    data class Success(val exerciseList: List<ExerciseWithProg> = listOf()): HisExListUiState
}