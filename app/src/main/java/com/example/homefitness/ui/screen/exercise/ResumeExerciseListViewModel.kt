package com.example.homefitness.ui.screen.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ResumeExerciseListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
): ViewModel() {

    private val runIdArg: Int = checkNotNull(savedStateHandle[ResumeExListDestination.runIdArg])
    private val planIdArg: Int = checkNotNull(savedStateHandle[ResumeExListDestination.planIdArg])

    private val _selectedItem = MutableStateFlow<List<Int>>(listOf())
    val selectedItem = _selectedItem.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState>(TopBarState.ADD_RUN)
    val topBarState = _topBarState.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Input)
    val uiState = _uiState.asStateFlow()

    private val _runExUiState: MutableStateFlow<RunExUiState> = MutableStateFlow(RunExUiState.Idle)
    val runExUiState = _runExUiState.asStateFlow()

    private val _planId = MutableStateFlow(runIdArg)
    val planId = _planId.asStateFlow()

    private val _runId = MutableStateFlow(runIdArg)
    val runPlanId = _runId.asStateFlow()

    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    private val _order = MutableStateFlow(0)
    val order = _order.asStateFlow()

    val exerciseListUiState: StateFlow<ResumeExListUiState> =
        exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg).map {exList->

            val res: MutableList<ExerciseWithProg> = mutableListOf()
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .filterNotNull()
                .first()
                .let {run->
                    _order.update { run.order }
                    exList.forEach {ex->
                        if(run.order>ex.runOrder){
                            res.add(ExerciseWithProg(ex,1f)) // already done
                        }else if (run.order==ex.runOrder){
                            res.add(ExerciseWithProg(ex, run.repDone.toFloat()/(ex.set.toFloat()*ex.rep.toFloat()))) //doing
                        }else if (run.order<ex.runOrder){
                            res.add(ExerciseWithProg(ex,0f)) // not reached
                        }
                    }
                }

            ResumeExListUiState.Success(res)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = ResumeExListUiState.Initial
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

    fun setRunUiState(value: RunExUiState){
        _runExUiState.value = value
    }

    fun setTopBarState(value: TopBarState){
        _topBarState.value = value
    }

    fun setPlanId(value:Int){
        _planId.value = value
    }

    fun setRunPlanId(value:Int){
        _runId.value = value
    }

    fun onRun(){
        when(val uiState = exerciseListUiState.value){
            is ResumeExListUiState.Success ->{
                uiState.exerciseList.first { it.exercise.runOrder == order.value }.let {ex->
                    val insertUiState = RunExUiState.Run(runIdArg,ex.exercise.toExerciseScreen())
                    _runExUiState.update { insertUiState }
                }
            }
            else -> {}
        }
    }

    companion object {
        private val detectorExercises = listOf("pushup","plank","lunge","squat")
        private const val TIMEOUT_MILLIS = 5_000L
    }
}


sealed interface ResumeExListUiState{
    object Initial: ResumeExListUiState
    data class Success(val exerciseList: List<ExerciseWithProg> = listOf()): ResumeExListUiState
}

sealed interface RunExUiState{
    object Idle: RunExUiState
    data class Run(val runId: Int = 0, val exScreen: Int = 0): RunExUiState
}

data class ExerciseWithProg(val exercise: Exercise, val prog: Float)