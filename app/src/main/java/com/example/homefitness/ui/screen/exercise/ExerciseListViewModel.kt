package com.example.homefitness.ui.screen.exercise

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ExerciseListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
): ViewModel() {

    private val planIdArg: Int = checkNotNull(savedStateHandle[ExerciseListDestination.planIdArg])

    var tempPlanId = 0

    private val _selectedItem = MutableStateFlow<List<Int>>(listOf())
    val selectedItem = _selectedItem.asStateFlow()

//    val selected = mutableStateListOf<Int>()

    private val _topBarState = MutableStateFlow<TopBarState>(TopBarState.ADD_RUN)
    val topBarState = _topBarState.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Input)
    val uiState = _uiState.asStateFlow()

    private val _planId = MutableStateFlow(planIdArg)
    val planId = _planId.asStateFlow()

    private val _runPlanId = MutableStateFlow(planIdArg)
    val runPlanId = _runPlanId.asStateFlow()

    private val _insertRunUiState: MutableStateFlow<InsertRunUiState> = MutableStateFlow(InsertRunUiState.Idle)
    val insertRunUiState = _insertRunUiState.asStateFlow()

    private val _deletePlanUiState: MutableStateFlow<DeletePlanUiState> = MutableStateFlow(DeletePlanUiState.Initial)
    val deletePlanUiState = _deletePlanUiState.asStateFlow()

    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    val exerciseListUiState: StateFlow<ExerciseListUiState> =
        exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg).map {
            ExerciseListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = ExerciseListUiState.Initial
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

    fun setUiState(value: UiState){
        _uiState.value = value
    }

    fun setTopBarState(value: TopBarState){
        _topBarState.value = value
    }

    fun setPlanId(value:Int){
        _planId.value = value
    }

    fun setRunPlanId(value:Int){
        _runPlanId.value = value
    }

    fun setInsertRunUiState(value: InsertRunUiState){
        _insertRunUiState.value = value
    }

    fun setDeletePlanUiState(value: DeletePlanUiState){
        _deletePlanUiState.value = value
    }


    fun onRun(){
        insertRun()
    }

    fun deleteExercise(){
        when (val uiState = exerciseListUiState.value){
            ExerciseListUiState.Initial -> {}

            is ExerciseListUiState.Success -> {
                uiState.exerciseList
                    .filter { it.exerciseId in selectedItem.value }
                    .forEach {
                        viewModelScope.launch{
                            exerciseRunRepositoryImpl.getExerciseRunsByPlanStream(planIdArg)
                                .filterNotNull()
                                .first()
                                .filter { it.runState in listOf(RunState.NEW,RunState.CONFIGURED,RunState.PAUSED)  }
                                .forEach {
                                    exerciseRunRepositoryImpl.updateExerciseRun(it.copy(runState = RunState.CHANGED)) // update paused run to invalidate run
                                }
                            exerciseRepositoryImpl.deleteExercise(it) // delete exercise
                        }
                        _selectedItem.value -= it.exerciseId
                }
            }
        }

    }

    fun insertRun(){
        var runID = 0
        _insertRunUiState.update { InsertRunUiState.InsertingRun }
        viewModelScope.launch {
            exerciseRunRepositoryImpl.insertExerciseRun(
                ExerciseRun(
                    order = 1,
                    repDone = 0,
                    planId = planIdArg,
                    date = Date(),
                    runState = RunState.NEW
                )
            )
            exerciseRunRepositoryImpl.getExerciseRunsByStateStream(RunState.NEW)
                .filterNotNull()
                .first()
                .firstOrNull()?.let {
                    runID = it.runId
                    exerciseRunRepositoryImpl.updateExerciseRun(it.copy(runState = RunState.CONFIGURED))
                }
        }.invokeOnCompletion {
            when(val uiState = exerciseListUiState.value){
                is ExerciseListUiState.Success ->{
                    uiState.exerciseList.firstOrNull()?.let {
                        val insertUiState = InsertRunUiState.InsertedRun(runID,it.toExerciseScreen())
                        _insertRunUiState.update { insertUiState }
                    }
                }
                else -> {}
            }
        }
    }

    fun deletePlan(){
        viewModelScope.launch {
            planRepositoryImpl.getPlanStream(planIdArg)
                .onStart { _deletePlanUiState.update { DeletePlanUiState.Deleting } }
                .filterNotNull()
                .first()
                .let {
                    planRepositoryImpl.deletePlan(it)
                    exerciseRepositoryImpl.getExercisesByPlanStream(it.planId)
                        .filterNotNull()
                        .first()
                        .forEach {
                            exerciseRepositoryImpl.deleteExercise(it)
                        }
                    exerciseRunRepositoryImpl.getExerciseRunsByPlanStream(it.planId)
                        .filterNotNull()
                        .first()
                        .forEach {
                            exerciseRunRepositoryImpl.deleteExerciseRun(it)
                        }
                }
        }.invokeOnCompletion {
            _deletePlanUiState.update { DeletePlanUiState.Deleted }
        }
    }

    fun promptDelete() = _deletePlanUiState.update { DeletePlanUiState.Confirmation }

    fun dismissDeletePrompt() = _deletePlanUiState.update { DeletePlanUiState.Initial }

    companion object {
        private val detectorExercises = listOf("pushup","plank","lunge","squat")
        const val TIMEOUT_MILLIS = 5_000L
    }
}


sealed interface ExerciseListUiState{
    object Initial: ExerciseListUiState
    data class Success(val exerciseList: List<Exercise> = listOf()): ExerciseListUiState
}

sealed interface DeletePlanUiState{
    object Initial: DeletePlanUiState
    object Confirmation: DeletePlanUiState
    object Deleting: DeletePlanUiState
    object Deleted: DeletePlanUiState
}

sealed interface InsertRunUiState{
    object Idle: InsertRunUiState
    object InsertingRun: InsertRunUiState
    data class InsertedRun(val runId: Int = 0, val exScreen: Int = 0): InsertRunUiState
}