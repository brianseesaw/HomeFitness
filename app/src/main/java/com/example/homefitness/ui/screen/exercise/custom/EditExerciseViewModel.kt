package com.example.homefitness.ui.screen.exercise.custom

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.Exercise
import com.example.homefitness.data.ExerciseRepositoryImpl
import com.example.homefitness.data.ExerciseType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepositoryImpl
    ): ViewModel() {
     val exerciseIdArg: Int = checkNotNull(savedStateHandle[EditExerciseDestination.exerciseIdArg])
    private val planIdArg: Int = checkNotNull(savedStateHandle[EditExerciseDestination.planIdArg])

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _rep = MutableStateFlow("")
    val rep = _rep.asStateFlow()

    private val _set = MutableStateFlow("")
    val set = _set.asStateFlow()

    private val _calorie = MutableStateFlow("")
    val calorie = _calorie.asStateFlow()

    private val _order = MutableStateFlow("")
    val order = _order.asStateFlow()

    private val _planId = MutableStateFlow(planIdArg)
    val planId = _planId.asStateFlow()

    private val _type = MutableStateFlow(ExerciseType.REP)
    val type = _type.asStateFlow()

//    private var _uiState = MutableStateFlow(UiState.Input)
//    val uiState = _uiState.asStateFlow()

    private var _expanded = MutableStateFlow(false)
    val expanded = _expanded.asStateFlow()

    private var _editExerciseUiState = MutableStateFlow<EditExerciseUiState>(EditExerciseUiState.Initial)
    val editExerciseUiState = _editExerciseUiState.asStateFlow()

    init {
//        Log.d("reach edit","reach edit init $planId $exerciseId")
        if(exerciseIdArg!=0){ // edit exercise
            viewModelScope.launch{
                exerciseRepository.getExerciseStream(exerciseIdArg)
                    .onStart { _editExerciseUiState.update { EditExerciseUiState.Initial } }
                    .filterNotNull()
                    .first()
                    .let {
                        setName(it.name)
                        setRep(it.rep.toString())
                        setSet(it.set.toString())
                        setCalorie(it.calorie.toString())
                        setPlanId(it.planId)
                        setType(it.type)
                    }
            }.invokeOnCompletion {
                _editExerciseUiState.update { EditExerciseUiState.Input }
            }
        }else{
            _editExerciseUiState.update { EditExerciseUiState.Input }
        }
    }

    fun setName(value:String){
        _name.value = value
        if (name.value == "plank"){
            setType(ExerciseType.TIME)
        }
    }

    fun setSet(value:String){
        _set.value = value
    }

    fun setRep(value:String){
        _rep.value = value
    }

    fun setType(value:ExerciseType){
        _type.value = value
    }

    fun setCalorie(value:String){
        _calorie.value = value
    }

    fun setPlanId(value:Int){
        _planId.value = value
    }

//    fun setUiState(value: UiState){
//        _uiState.value = value
//    }

    fun setExpanded(value: Boolean){
        _expanded.value = value
    }

    fun setEditExerciseUiState(value: EditExerciseUiState){
        _editExerciseUiState.value = value
    }

    fun changeType(){
        if (type.value == ExerciseType.REP){
            setType(ExerciseType.TIME)
        }else{
            setType(ExerciseType.REP)
        }
    }

    fun getParam(): String{
        return "$exerciseIdArg $planIdArg"
    }

    fun dismissError(){
        _editExerciseUiState.update { EditExerciseUiState.Input }
    }


    fun saveExercise() {
        if (validateInput()) {
            if (exerciseIdArg == 0) { // new
                insertExercise()
            }else {
                updateExercise()
            }
//            setUiState(UiState.Saved)
        }else{
            _editExerciseUiState.update { EditExerciseUiState.Error }
        }
    }

    fun insertExercise(){
        viewModelScope.launch {

            if (name.value == "plank"){
                setType(ExerciseType.TIME)
            }

            exerciseRepository.getExercisesByPlanStream(planIdArg)
                .onStart { _editExerciseUiState.update { EditExerciseUiState.Saving } }
                .filterNotNull()
                .first()
                .count().let {cnt->
                    exerciseRepository.insertExercise( // insert new exercise
                        Exercise(
                            name = name.value,
                            rep = rep.value.toIntOrNull() ?: 0,
                            set = set.value.toIntOrNull() ?: 0,
                            type = type.value,
                            runOrder = cnt+1,
                            planId = planId.value,
                            calorie = calorie.value.toFloatOrNull() ?: 0f
                        )
                    )
                }
        }.invokeOnCompletion {
            _editExerciseUiState.update { EditExerciseUiState.Saved }
        }
    }

    fun updateExercise(){

        if (name.value == "plank"){
            setType(ExerciseType.TIME)
        }

        viewModelScope.launch {
            exerciseRepository.getExerciseStream(exerciseIdArg)
                .onStart {_editExerciseUiState.update { EditExerciseUiState.Saving } }
                .filterNotNull()
                .first()
                .let {
                    exerciseRepository.updateExercise( // update exercise
                        it.copy(
                            name = name.value,
                            rep = rep.value.toIntOrNull() ?: 0,
                            set = set.value.toIntOrNull() ?: 0,
                            type = type.value,
                            calorie = calorie.value.toFloatOrNull() ?: 0f,
                        )
                    )
                }
        }.invokeOnCompletion {
            _editExerciseUiState.update { EditExerciseUiState.Saved }
        }
    }

    private fun validateInput():Boolean{
        return name.value.isNotBlank() &&
                rep.value.isDigitsOnly() &&
                rep.value.isNotBlank() &&
                set.value.isDigitsOnly() &&
                set.value.isNotBlank() &&
                calorie.value.toFloatOrNull() != null &&
                calorie.value.isNotBlank()
    }

}

sealed interface EditExerciseUiState{
    object Initial: EditExerciseUiState
    object Error: EditExerciseUiState
    object Input: EditExerciseUiState
    object Saving: EditExerciseUiState
    object Saved: EditExerciseUiState
}