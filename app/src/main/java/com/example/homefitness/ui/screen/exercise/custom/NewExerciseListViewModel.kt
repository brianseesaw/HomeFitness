package com.example.homefitness.ui.screen.exercise.custom

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.TopBarState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewExerciseListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
): ViewModel() {

    private val planIdArg: Int = checkNotNull(savedStateHandle[NewExerciseListDestination.planIdArg])

    private val _selectedItem = MutableStateFlow<List<Int>>(listOf())
    val selectedItem = _selectedItem.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState>(TopBarState.ADD)
    val topBarState = _topBarState.asStateFlow()

    private val _saveUiState = MutableStateFlow<SaveExerciseListUiState>(SaveExerciseListUiState.Initial)
    val saveUiState = _saveUiState.asStateFlow()

    private val _planId = MutableStateFlow(planIdArg)
    val planId = _planId.asStateFlow()

    private val _runPlanId = MutableStateFlow(planIdArg)
    val runPlanId = _runPlanId.asStateFlow()

    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    val exerciseListUiState: StateFlow<NewExerciseListUiState> =
        exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg).map {
            NewExerciseListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = NewExerciseListUiState.Initial
        )

    init {
        Log.d("INIT NEW_EX_LIST",planIdArg.toString())
        viewModelScope.launch {
            planRepositoryImpl.getPlanStream(planIdArg)// get plan name
                .filterNotNull()
                .first()
                .let {  plan->
                    _planName.update { plan.name }
                }
        }
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

    fun onItemClick(id:Int){
        if (selectedItem.value.contains(id)){
            _selectedItem.value -= id
        }else{
            Log.d("add","add")
            _selectedItem.value += id
        }

        updateTopBar()
    }

    fun deleteExercise(){
        when (val uiState = exerciseListUiState.value){
            NewExerciseListUiState.Initial -> {}

            is NewExerciseListUiState.Success -> {
                uiState.exerciseList
                    .filter { it.exerciseId in selectedItem.value }
                    .forEach {
                        viewModelScope.launch{
                            exerciseRunRepositoryImpl.getExerciseRunsByPlanStream(planIdArg)
                                .filterNotNull()
                                .first()
                                .filter { it.runState in listOf(
                                    RunState.NEW,
                                    RunState.CONFIGURED,
                                    RunState.PAUSED)  }
                                .forEach {
                                    exerciseRunRepositoryImpl.updateExerciseRun(it.copy(runState = RunState.CHANGED)) // update paused run to invalidate run
                                }
                            exerciseRepositoryImpl.deleteExercise(it) // delete exercise
                        }
                        _selectedItem.value -= it.exerciseId
                    }
                updateTopBar()
            }
        }

    }

    fun dismissSave() = _saveUiState.update { SaveExerciseListUiState.Initial }

    fun confirmSave() = _saveUiState.update { SaveExerciseListUiState.Back }

    fun promptSave() = _saveUiState.update { SaveExerciseListUiState.Confirmation }


    fun moveExerciseUp(){
        moveExercise(true)
    }

    fun moveExerciseDown(){
        moveExercise(false)
    }

    fun moveExercise(moveUp: Boolean){
        selectedItem.value.firstOrNull()?.let {selected->
            viewModelScope.launch {
                val list = exerciseRepositoryImpl.getExercisesByPlanStream(planIdArg)
                    .filterNotNull()
                    .first()
                val selectedExercise = list.first { it.exerciseId == selected }
                val selectedOrder = selectedExercise.runOrder
                if(selectedOrder==1){ // first
                    if (!moveUp){
                        list.first{it.runOrder==2}.let {
                            exerciseRepositoryImpl.updateExercise(it.copy(runOrder = 1)) // move next ex up
                        }
                        exerciseRepositoryImpl.updateExercise(selectedExercise.copy(runOrder = 2)) // move first ex down
                    }
                }else if(selectedOrder==list.count()){ // last
                    if (moveUp){
                        list.first{it.runOrder==list.count()-1}.let {
                            exerciseRepositoryImpl.updateExercise(it.copy(runOrder = list.count())) // move prev ex down
                        }
                        exerciseRepositoryImpl.updateExercise(selectedExercise.copy(runOrder = list.count()-1)) // move last ex up
                    }
                }else{
                    if (moveUp){ // move up
                        list.first{it.runOrder==selectedExercise.runOrder-1}.let {
                            exerciseRepositoryImpl.updateExercise(it.copy(runOrder = selectedExercise.runOrder)) // move prev ex down
                        }
                        exerciseRepositoryImpl.updateExercise(selectedExercise.copy(runOrder = selectedExercise.runOrder-1)) // move selected ex up
                    }else{ // move down
                        list.first{it.runOrder==selectedExercise.runOrder+1}.let {
                            exerciseRepositoryImpl.updateExercise(it.copy(runOrder = selectedExercise.runOrder)) // move next ex up
                        }
                        exerciseRepositoryImpl.updateExercise(selectedExercise.copy(runOrder = selectedExercise.runOrder+1)) // move selected ex down
                    }
                }
            }
        }
    }

    fun updateTopBar(){
        if(selectedItem.value.isEmpty()){
            setTopBarState(TopBarState.ADD)
        }
        val selectedCount = selectedItem.value.count()
        if (selectedCount==1) setTopBarState(TopBarState.EDIT_DELETE_MOVE)
        if (selectedCount>1) setTopBarState(TopBarState.DELETE)
    }

    companion object {
        private val detectorExercises = listOf("pushup","plank","lunge","squat")
        private const val TIMEOUT_MILLIS = 5_000L
    }
}


sealed interface NewExerciseListUiState{
    object Initial: NewExerciseListUiState
    data class Success(val exerciseList: List<Exercise> = listOf()): NewExerciseListUiState
}

sealed interface SaveExerciseListUiState{
    object Initial: SaveExerciseListUiState
    object Confirmation: SaveExerciseListUiState
    object Back: SaveExerciseListUiState
}