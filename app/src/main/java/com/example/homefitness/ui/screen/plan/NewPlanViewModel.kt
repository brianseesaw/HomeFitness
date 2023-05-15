package com.example.homefitness.ui.screen.plan

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewPlanViewModel(
    private val exerciseRepository: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl
): ViewModel() {

//    private var _uiState = MutableStateFlow(UiState.Input)
//    val uiState = _uiState.asStateFlow()

    private val _exName = MutableStateFlow("")
    val exName = _exName.asStateFlow()

    private val _planName = MutableStateFlow("")
    val planName = _planName.asStateFlow()

    private val _rep = MutableStateFlow("")
    val rep = _rep.asStateFlow()

    private val _set = MutableStateFlow("")
    val set = _set.asStateFlow()

    private val _calorie = MutableStateFlow("")
    val calorie = _calorie.asStateFlow()

    private val _type = MutableStateFlow(ExerciseType.REP)
    val type = _type.asStateFlow()

    private var _expanded = MutableStateFlow(false)
    val expanded = _expanded.asStateFlow()

    private val _newPlanListUiState: MutableStateFlow<NewPlanListUiState> = MutableStateFlow(
        value = NewPlanListUiState.Input
    )
    val newPlanListUiState = _newPlanListUiState.asStateFlow()

    fun setExName(value:String){
        _exName.value = value
        if (exName.value == "plank"){
            setType(ExerciseType.TIME)
        }
    }

    fun setPlanName(value:String){
        _planName.value = value
    }

    fun setSet(value:String){
        _set.value = value
    }

    fun setRep(value:String){
        _rep.value = value
    }

    fun setType(value: ExerciseType){
        _type.value = value
    }

    fun setCalorie(value:String){
        _calorie.value = value
    }

    fun setUiState(value: NewPlanListUiState){
        _newPlanListUiState.value = value
    }

    fun setExpanded(value: Boolean){
        _expanded.value = value
    }

    fun changeType(){
        if (type.value == ExerciseType.REP){
            setType(ExerciseType.TIME)
        }else{
            setType(ExerciseType.REP)
        }
    }

    fun addPlan(){
        if (validateInput()) {
            _newPlanListUiState.update { NewPlanListUiState.Saving}
            insertPlan()
        }else{
            _newPlanListUiState.update { NewPlanListUiState.Error }
//            setUiState(UiState.Error)
        }
    }

    fun insertPlan(){
        var planId = 0
        viewModelScope.launch {
            planRepositoryImpl.insertPlan( //insert plan
                Plan(
                    name = planName.value,
                    planState = PlanState.EMPTY
                )
            )

            planRepositoryImpl.getPlanByStateStream(PlanState.EMPTY)
                .filterNotNull()
                .first()
                .firstOrNull()?.let {
                    if (exName.value == "plank"){
                        setType(ExerciseType.TIME)
                    }
                    exerciseRepository.insertExercise( // insert exercise
                        Exercise(
                            name = exName.value,
                            rep = rep.value.toIntOrNull() ?: 0,
                            set = set.value.toIntOrNull() ?: 0,
                            type = type.value,
                            runOrder = 1,
                            planId = it.planId,
                            calorie = calorie.value.toFloatOrNull() ?: 0f
                        )
                    )

                    planRepositoryImpl.updatePlan(it.copy(planState = PlanState.MY)) // update plan state
//                    planRepositoryImpl.updatePlan(it.copy(planState = PlanState.RUN)) // update plan state test resume
//                    planRepositoryImpl.updatePlan(it.copy(planState = PlanState.DONE)) // update plan state test history
//                    planRepositoryImpl.updatePlan(it.copy(planState = PlanState.BROWSE)) // update plan state test BROWSE
                    planId = it.planId

                }
            }.invokeOnCompletion { _newPlanListUiState.update { NewPlanListUiState.Saved(planId) } }
    }

    private fun validateInput():Boolean{
        return planName.value.isNotBlank()
            &&
                exName.value.isNotBlank() &&
                rep.value.isDigitsOnly() &&
                rep.value.isNotBlank() &&
                set.value.isDigitsOnly() &&
                set.value.isNotBlank() &&
                calorie.value.toFloatOrNull() != null &&
                calorie.value.isNotBlank()
    }

    fun dismissError(){
        _newPlanListUiState.update { NewPlanListUiState.Input }
    }
    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

enum class UiState {
    Saved, Input, Error, Run, Edit
}


sealed interface NewPlanListUiState{
    object Input: NewPlanListUiState
    object Error: NewPlanListUiState
    object Saving: NewPlanListUiState
    data class Saved(val planId: Int = 0): NewPlanListUiState
}