package com.example.homefitness.ui.screen.exercise.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chargemap.compose.numberpicker.NumberPicker
import com.example.homefitness.data.Exercise
import com.example.homefitness.data.ExerciseRun
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.ConfirmationDialog
import com.example.homefitness.ui.screen.exercise.DisplayCard
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel
import kotlin.math.ceil

object RepExDestination : NavigationDestination {
    override val route = "rep_exercise"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    val routeWithArgs = "$route?$runIdArg={$runIdArg}"
}

@Composable
fun RepExScreen(
    viewModel: RepExViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onBackToList: () -> Unit = {},
    onNextExercise: (Int, Int) -> Unit = { s: Int, i: Int -> },
){
    val repDone by viewModel.repDone.collectAsState()
    val ex by viewModel.ex.collectAsState()
    val run by viewModel.run.collectAsState()
    val repExerciseUiState by viewModel.repExerciseUiState.collectAsState()

    BackHandler(true) {
        viewModel.backDialog()
    }

    Scaffold(
        topBar = { TopBar(
            title = ex.name,
            onBack = viewModel::backDialog,
            topBarState = TopBarState.NONE,
        ) }
    ) {

        when(val uiState = repExerciseUiState){
            RepExUiState.Initial -> {
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            RepExUiState.PlanDone -> {
                LaunchedEffect(repExerciseUiState){
                    onBackToList()
                }
            }
            RepExUiState.Saved -> {}
            RepExUiState.Saving -> {
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
            RepExUiState.Success -> {
                ExerciseContent(
                    paddingValues = it,
                    run = run,
                    ex = ex,
                    repDoneTemp = run.repDone,
                    repDone =repDone,
                    onFinish = viewModel::finishExercise,
                    onPause = viewModel::onPauseClick,
                    onRepDoneChange = viewModel::setRepDone)
            }
            is RepExUiState.NextExercise -> {
                LaunchedEffect(repExerciseUiState) {
                    onNextExercise(run.runId, uiState.exScreen)
                }
            }
            RepExUiState.PlanDoneDialog -> {
                PlanDoneDialog {
                    viewModel.dismissPlanDoneDialog()
                }
            }
            RepExUiState.BackDialog -> {
                BackConfirmDialog(onConfirm = viewModel::confirmBackDialog, onCancel = viewModel::dismissBackDialog)
            }
            RepExUiState.NavigateBack -> {
                LaunchedEffect(repExerciseUiState){
                    onBackToList()
                }
            }
        }
    }
}

@Composable
fun ExerciseContent(
    paddingValues: PaddingValues,
    run: ExerciseRun,
    ex: Exercise,
    repDoneTemp: Int,
    repDone: Int,
    onFinish: () -> Unit = {},
    onPause: () -> Unit = {},
//    onRepDoneChange: (String) -> Unit = {},
    onRepDoneChange: (Int) -> Unit = {},
){
    Box(
        modifier = Modifier
            .padding(paddingValues))
    {
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .align(Alignment.TopStart)
        ){
//            DisplayCard("runId: ${run.runId}")
//            DisplayCard("planId: ${run.planId}")
//            DisplayCard("exId: ${ex.exerciseId}")
            DisplayCard("exercise: ${ex.name}")

            if(repDone%ex.rep==0 && repDone !=(ex.rep*ex.set)){
                DisplayCard("set: ${(ceil(repDone.toDouble()/ex.rep.toDouble())).toInt()+1}/${ex.set}")
            }else{
                DisplayCard("set: ${(ceil(repDone.toDouble()/ex.rep.toDouble())).toInt()}/${ex.set}")
            }
            if(repDone%ex.rep==0){
                DisplayCard("rep: ${ex.rep}/${ex.rep}")
            }else{
                DisplayCard("rep: ${repDone%ex.rep}/${ex.rep}")
            }
            DisplayCard("rep done: $repDone/${ex.set*ex.rep}")
            NumberPicker(value = repDone, onValueChange = onRepDoneChange, range = repDoneTemp..(ex.set*ex.rep))
            Row() {
                OutlinedButton(onClick = onFinish) {
                    Text("Done Exercise")
                }
                OutlinedButton(onClick = onPause) {
                    Text("Pause Exercise")
                }
            }

        }
    }
}

@Composable
fun BackConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
){
    ConfirmationDialog(
        title = "Return",
        text = "Return back to plan list? Your progress will be saved and accessible in Resume Plan within today.",
        onConfirm = onConfirm,
        onCancel = onCancel)
}

@Composable
fun PlanDoneDialog(
    onConfirm: () -> Unit,
){
    AlertDialog(onDismissRequest = { /* Do nothing */ },
        title = { Text("Congratulations") },
        text = { Text("You have completed this exercise plan") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },)
}