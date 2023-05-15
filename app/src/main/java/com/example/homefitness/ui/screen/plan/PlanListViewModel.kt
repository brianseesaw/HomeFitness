package com.example.homefitness.ui.screen.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.TopBarState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlanListViewModel(
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl
) : ViewModel(){

    private val _selectedItem = MutableStateFlow<List<Int>>(listOf())
    val selectedItem = _selectedItem.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState>(TopBarState.ADD)
    val topBarState = _topBarState.asStateFlow()

    val myPlanListUiState: StateFlow<MyPlanListUiState> =
        planRepositoryImpl.getPlanByStateStream(PlanState.MY).map {
            MyPlanListUiState.Success(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = MyPlanListUiState.Initial
        )

    fun setTopBarState(value: TopBarState){
        _topBarState.value = value
    }

    fun onItemClick(id:Int){
        if (selectedItem.value.contains(id)){
            _selectedItem.value -= id
        }else{
            _selectedItem.value += id
        }

        updateTopBar()
    }

    fun updateTopBar(){
        if(selectedItem.value.isEmpty()){
            setTopBarState(TopBarState.ADD)
        }
        val selectedCount = selectedItem.value.count()
        if (selectedCount==1) setTopBarState(TopBarState.EDIT_DELETE)
        if (selectedCount>1) setTopBarState(TopBarState.DELETE)
    }

    fun deletePlan(){
        when(val uistate = myPlanListUiState.value){
            MyPlanListUiState.Initial -> TODO()
            is MyPlanListUiState.Success -> {
                uistate.planList
                    .filter { it.planId in selectedItem.value }
                    .forEach { plan->
                        viewModelScope.launch {
                            planRepositoryImpl.deletePlan(plan)
                            exerciseRepositoryImpl.getExercisesByPlanStream(plan.planId)
                                .filterNotNull()
                                .first()
                                .forEach {
                                    exerciseRepositoryImpl.deleteExercise(it) // delete exercise
                                }
                            _selectedItem.value -= plan.planId
                        }
                    }
                updateTopBar()
            }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

//sealed interface MyPlanListUiState{
//    object Initial: MyPlanListUiState
//    data class Success(val planList: List<Plan> = listOf()): MyPlanListUiState
//}




