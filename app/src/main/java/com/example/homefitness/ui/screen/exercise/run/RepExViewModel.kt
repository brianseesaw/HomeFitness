package com.example.homefitness.ui.screen.exercise.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RepExViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,

    ): ViewModel(){
    private val runIdArg: Int = checkNotNull(savedStateHandle[RepExDestination.runIdArg])

    private val _ex = MutableStateFlow(Exercise())
    val ex = _ex.asStateFlow()

    private val _run = MutableStateFlow(ExerciseRun())
    val run = _run.asStateFlow()
    
    private val _repDone = MutableStateFlow(0)
    val repDone = _repDone.asStateFlow()

    private val _repExUiState = MutableStateFlow<RepExUiState>(RepExUiState.Initial)
    val repExerciseUiState = _repExUiState.asStateFlow()
    
    init {
        getExercise()
    }

    fun getExercise(){
        viewModelScope.launch{
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .onStart { _repExUiState.update { RepExUiState.Initial } }
                .filterNotNull()
                .first()
                .let {run->
                    _run.update { run }
                    _repDone.update { run.repDone }
                    exerciseRepositoryImpl.getExercisesByPlanOrderStream(run.planId,run.order)
                        .filterNotNull()
                        .first()
                        .firstOrNull()?.let {ex->
                            _ex.update { ex }
                        }
                }
        }.invokeOnCompletion {
            _repExUiState.update { RepExUiState.Success }
        }
    }

    fun setRepDone(value:Int){
        _repDone.value = value
    }

    fun updateExercise(completeState: RepExUiState){

        if (repDone.value == (ex.value.rep * ex.value.set)){
            finishExercise() // done all rep
        }else{
            _repExUiState.update { RepExUiState.Saving }
            viewModelScope.launch {
                _run.update { run.value.copy(
                    repDone=repDone.value,
                    runState = RunState.PAUSED,
                ) }
                exerciseRunRepositoryImpl.updateExerciseRun(run.value) // update rep
            }.invokeOnCompletion {
                _repExUiState.update { completeState }
            }
        }
    }

    fun finishExercise(){
        viewModelScope.launch{
            exerciseRepositoryImpl.getExercisesByPlanStream(run.value.planId)
                .onStart { _repExUiState.update { RepExUiState.Saving } }
                .filterNotNull()
                .first()
                .let{exList->
                    if(run.value.order == exList.count()){ // if count==runOrder all exercise in plan was ran
                        _run.update {
                            run.value.copy(  // update run as done
                                repDone=0,
                                runState = RunState.DONE)}
                    }else{
                        _run.update {
                            run.value.copy(  // update to run next exercise
                                repDone=0,
                                order = run.value.order+1,
                                runState = RunState.NEW)}
                        _ex.update { exList.first { it.runOrder == run.value.order }}
                    }
                    exerciseRunRepositoryImpl.updateExerciseRun(run.value)
                }
        }.invokeOnCompletion {
            if(run.value.runState == RunState.DONE) {
                _repExUiState.update { RepExUiState.PlanDoneDialog }
            }
            if(run.value.runState == RunState.NEW) {
                _repExUiState.update { RepExUiState.NextExercise(ex.value.toExerciseScreen()) }
            }
        }
    }

    fun onPauseClick() = updateExercise(RepExUiState.Success) // update exercise repdone when pause clicked
    fun dismissPlanDoneDialog() = _repExUiState.update { RepExUiState.PlanDone } // dismiss plan done dialog and navigate back
    fun backDialog() = _repExUiState.update { RepExUiState.BackDialog } // show back dialog
    fun confirmBackDialog() {
        updateExercise(RepExUiState.NavigateBack) // update exercise repdone and navigate back
    }
    fun dismissBackDialog() = _repExUiState.update { RepExUiState.Success }
    
    companion object{

        private val detectorExercises = listOf("pushup","plank","lunge","squat")
    }
}

sealed interface RepExUiState{
    object Initial: RepExUiState
    object Saving: RepExUiState
    object Saved: RepExUiState
    object Success: RepExUiState
    object PlanDoneDialog: RepExUiState
    object PlanDone: RepExUiState
    object BackDialog: RepExUiState
    object NavigateBack: RepExUiState
    data class NextExercise(val exScreen: Int = 0): RepExUiState
//    data class Success(val exerciseList: List<Exercise> = listOf()): RepExerciseUiState
}