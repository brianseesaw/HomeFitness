package com.example.homefitness.ui.screen.exercise.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.homefitness.data.Exercise
import com.example.homefitness.data.ExerciseRun
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.DisplayCard
import com.example.homefitness.ui.screen.exercise.formatProb
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel
import org.tensorflow.lite.support.label.Category
import java.time.Duration
import kotlin.math.ceil

object TimeExDestination : NavigationDestination {
    override val route = "time_exercise"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    val routeWithArgs = "$route?$runIdArg={$runIdArg}"
}

@Composable
fun TimeExScreen(
    viewModel: TimeExViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onBackToList: () -> Unit = {},
    onNextExercise: (Int, Int) -> Unit = { s: Int, i: Int -> },
){
    val repDone by viewModel.repDone.collectAsState()
    val ex by viewModel.ex.collectAsState()
    val run by viewModel.run.collectAsState()
    val repExerciseUiState by viewModel.timeExerciseUiState.collectAsState()
    val soundProbabilities by viewModel.soundProbabilities.collectAsState()
    val time by viewModel.time.collectAsState()
    val inExercise by viewModel.inExercise.collectAsState()

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
            TimeExUiState.Initial -> {
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            TimeExUiState.PlanDone -> {
                LaunchedEffect(repExerciseUiState){
                    onBackToList()
                }
            }
            TimeExUiState.Saved -> {}
            TimeExUiState.Saving -> {
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
            TimeExUiState.Success -> {
                ExerciseContentWithTimer(
                    paddingValues = it,
                    repDone = repDone,
                    run = run,
                    ex = ex,
                    time = time,
                    probabilities = soundProbabilities,
                    inExercise = inExercise,
                )
            }
            is TimeExUiState.NextExercise -> {
                LaunchedEffect(repExerciseUiState) {
                    onNextExercise(run.runId, uiState.exScreen)
                }
            }
            TimeExUiState.BackDialog -> {
                BackConfirmDialog(onConfirm = viewModel::confirmBackDialog, onCancel = viewModel::dismissBackDialog)
            }
            TimeExUiState.NavigateBack -> {
                LaunchedEffect(repExerciseUiState){
                    viewModel.freeRes()
                    onBackToList()
                }
            }
            TimeExUiState.PlanDoneDialog -> {
                PlanDoneDialog {
                    viewModel.dismissPlanDoneDialog()
                }
            }
        }

    }
}

@Composable
fun ExerciseContentWithTimer(
    paddingValues: PaddingValues,
    run: ExerciseRun,
    ex: Exercise,
//    repDoneTemp: Int,
    repDone: Int,
    time: Duration,
    probabilities: List<Category>,
    inExercise: Boolean,
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
            val currRepDone = (repDone.toDouble()-(repDone.toDouble() % ex.rep.toDouble()) + (ex.rep.toDouble() - time.seconds.toDouble()))
            DisplayCard("name: ${ex.name}")
//            DisplayCard("repDone: ${repDone}")

            if(currRepDone.toInt()%ex.rep==0 && currRepDone.toInt()!=(ex.rep*ex.set)){
                DisplayCard("set: ${(ceil(currRepDone/ex.rep.toDouble())).toInt()+1}/${ex.set}")
            }else{
                DisplayCard("set: ${(ceil(currRepDone/ex.rep.toDouble())).toInt()}/${ex.set}")
            }
            DisplayCard("time left: ${time.seconds} sec")
            DisplayCard("status: "+(if(inExercise) "Running" else "Paused"))
//            DisplayCard(formatProb((probabilities.firstOrNull())))
        }
    }
}

