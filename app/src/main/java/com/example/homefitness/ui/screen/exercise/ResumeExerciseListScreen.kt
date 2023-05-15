package com.example.homefitness.ui.screen.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.homefitness.data.Exercise
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object ResumeExListDestination : NavigationDestination {
    override val route = "resume_exercise_list"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    const val planIdArg = "planId"
    val routeWithArgs = "$route/{$runIdArg}/{$planIdArg}"
}

@Composable
fun ResumeExerciseListScreen(
    viewModel: ResumeExerciseListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onRun: (Int,Int) -> Unit = {id: Int, screen: Int ->},
){
    val planName by viewModel.planName.collectAsState()
    val exerciseListUiState by viewModel.exerciseListUiState.collectAsState()
    val selectedList by viewModel.selectedItem.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val planId by viewModel.planId.collectAsState()
    val runPlanId by viewModel.runPlanId.collectAsState()
    val runExUiState by viewModel.runExUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = planName,
            onBack = onBack,
            onRun = viewModel::onRun,
            topBarState = TopBarState.RUN,
        ) }
    ) {
        when(val uiState = exerciseListUiState){
            ResumeExListUiState.Initial -> {
                SpinnerScreen(msg = "Loading", paddingValues = it)
            }
            is ResumeExListUiState.Success -> {
                when(val runUistate = runExUiState){
                    RunExUiState.Idle -> {
                        Box(modifier = Modifier.padding(it)) {
                            Column() {
                                ExerciseWithProgList(exerciseList = uiState.exerciseList)
                            }
                        }
                    }
                    is RunExUiState.Run -> {
                        LaunchedEffect(exerciseListUiState){
                            onRun(runUistate.runId,runUistate.exScreen)
                            viewModel.setRunUiState(RunExUiState.Idle)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExerciseWithProgList(
    exerciseList: List<ExerciseWithProg>,
    onItemClick:(Int) -> Unit = {},
){
    if (exerciseList.isEmpty()) {
        Text(
            text = "No exercise found",
            textAlign = TextAlign.Center
        )
    }else{
        LazyColumn() {
            itemsIndexed(exerciseList){index, item ->
                ListItem(
                    text = { Text(item.exercise.name) },
                    secondaryText = { Text("set: "+item.exercise.set.toString()+" rep: "+item.exercise.rep.toString()) },
                    trailing = {
                        CircularProgressIndicator(progress = item.prog, color = if (item.prog==1f) Color.Green else Color.DarkGray)
                        Text("${(item.prog*100).toInt()}")
                    },
                    modifier = Modifier.clickable {
                        onItemClick(item.exercise.exerciseId)
                    }
                )
                Divider()
            }
        }
    }
}