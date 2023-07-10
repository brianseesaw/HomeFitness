package com.example.homefitness.ui.screen.exercise.run

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.homefitness.data.Exercise
import com.example.homefitness.data.ExerciseRun
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.DrawPose
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.CameraSection
import com.example.homefitness.ui.screen.exercise.CameraUiState
import com.example.homefitness.ui.screen.exercise.DisplayCard
import com.example.homefitness.ui.screen.exercise.formatProb
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import com.ujizin.camposer.state.rememberCameraState
import org.koin.androidx.compose.getViewModel
import org.tensorflow.lite.support.label.Category
import java.time.Duration
import kotlin.math.ceil

object TimeExCamDestination : NavigationDestination {
    override val route = "time_exercise_detector"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    const val orenArg = "oren"
    val routeWithArgs = "$route?$runIdArg={$runIdArg}&$orenArg={$orenArg}"
}

@ExperimentalGetImage
@Composable
fun TimeExCamScreen(
    viewModel: TimeExCamViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onBackToList: () -> Unit = {},
    onNextExercise: (Int, Int) -> Unit = { s: Int, i: Int -> },
    orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
){
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as Activity
    val repDone by viewModel.repDone.collectAsState()
    val ex by viewModel.ex.collectAsState()
    val run by viewModel.run.collectAsState()
    val exerciseUiState by viewModel.timeExerciseUiState.collectAsState()
    val soundProbabilities by viewModel.soundProbabilities.collectAsState()
    val time by viewModel.time.collectAsState()
    val cameraUiState by viewModel.cameraUiState.collectAsState()
    val isBadPose by viewModel.isBadPose.collectAsState()
    val enablePoseClassifier by viewModel.enablePoseClassifier.collectAsState()
    val isLandscape by viewModel.isLandscape.collectAsState()

    activity.requestedOrientation = orientation

    BackHandler(true) {
        viewModel.backDialog()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
//            if (orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
//                if (event == Lifecycle.Event.ON_START) {
//                    activity.requestedOrientation = orientation
//                } else if (event == Lifecycle.Event.ON_STOP) {
//                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//                }
//            }
        }

        lifecycleOwner.lifecycle.addObserver(observer) // Add the observer to the lifecycle

        // When the effect leaves the Composition, remove the observer
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = { TopBar(
            title = ex.name,
            onBack = viewModel::backDialog,
            topBarState = TopBarState.NONE,
        ) }
    ) {

        when(val uiState = exerciseUiState){
            TimeExDetectorUiState.Initial -> {
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            TimeExDetectorUiState.PlanDone -> {
                LaunchedEffect(exerciseUiState){
                    onBackToList()
                }
            }
            TimeExDetectorUiState.Saved -> {}
            TimeExDetectorUiState.Saving -> {
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
            TimeExDetectorUiState.Success -> {
                DetectorWithTimerScreen(
                    cameraUiState = cameraUiState,
                    isBadPose = isBadPose,
                    onAnalyzeImage = viewModel::analyzeImage,
                    enablePoseClassifier = enablePoseClassifier,
                    time = time,
                    repDone = repDone,
                    soundProb = soundProbabilities,
                    run = run,
                    ex = ex,
                    paddingValues = it,
                )
            }
            is TimeExDetectorUiState.NextExercise -> {
                LaunchedEffect(exerciseUiState) {
                    onNextExercise(run.runId, uiState.exScreen)
                }
            }
            TimeExDetectorUiState.BackDialog -> {
                BackConfirmDialog(onConfirm = viewModel::confirmBackDialog, onCancel = viewModel::dismissBackDialog)
            }
            TimeExDetectorUiState.NavigateBack -> {
                LaunchedEffect(exerciseUiState){
                    viewModel.freeRes()
                    onBackToList()
                }
            }
            TimeExDetectorUiState.PlanDoneDialog -> {
                PlanDoneDialog {
                    viewModel.dismissPlanDoneDialog()
                }
            }
        }

    }
}

@Composable
fun DetectorWithTimerScreen(
    cameraUiState: CameraUiState,
    isBadPose: Boolean,
    onAnalyzeImage: (ImageProxy) -> Unit,
    enablePoseClassifier: Boolean,
    time: Duration,
    repDone: Int,
    soundProb: List<Category>,
    run: ExerciseRun,
    ex: Exercise,
    paddingValues: PaddingValues,
){
    when (val result: CameraUiState = cameraUiState) {
        is CameraUiState.Ready -> {
            val cameraState = rememberCameraState()
            CameraSection(
                cameraState = cameraState,
                onAnalyzeImage = onAnalyzeImage,
            )

            val context = LocalContext.current
            LaunchedEffect(result.throwable) {
                if (result.throwable != null) {
                    Toast.makeText(context, result.throwable.message, Toast.LENGTH_SHORT).show()
                }
            }

            result.pose?.let {
                DrawPose(pose = it,isBadPose)
            }

            Column() {
                if(enablePoseClassifier){
                    result.poseType1?.let {
                        DisplayCard(formatProb((it)))
                    }
                }

                val currRepDone = (repDone.toDouble()-(repDone.toDouble() % ex.rep.toDouble()) + (ex.rep.toDouble() - time.seconds.toDouble()))
                DisplayCard("name: ${ex.name}")
                if(currRepDone.toInt()%ex.rep==0 && currRepDone.toInt()!=(ex.rep*ex.set)){
                    DisplayCard("set: ${(ceil(currRepDone/ex.rep.toDouble())).toInt()+1}/${ex.set}")
                }else{
                    DisplayCard("set: ${(ceil(currRepDone/ex.rep.toDouble())).toInt()}/${ex.set}")
                }
                DisplayCard("time left: ${time.seconds} sec")
                DisplayCard(formatProb((soundProb.firstOrNull())))
            }
        }

        is CameraUiState.Initial -> Unit
    }
}