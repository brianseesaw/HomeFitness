package com.example.homefitness.ui.screen.exercise.run

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homefitness.data.Exercise
import com.example.homefitness.data.ExerciseRun
import com.example.homefitness.ui.LoadingSpinner
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.DrawPose
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.*
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import com.ujizin.camposer.state.rememberCameraState
import org.koin.androidx.compose.getViewModel
import kotlin.math.ceil

object RepExCamDestination : NavigationDestination {
    override val route = "rep_exercise_detector"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    const val orenArg = "oren"
    val routeWithArgs = "$route?$runIdArg={$runIdArg}&$orenArg={$orenArg}"
}

@ExperimentalGetImage
@Composable
fun RepExCamScreen(
    viewModel: RepExCamViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onBackToList: () -> Unit = {},
    onNextExercise: (Int, Int) -> Unit = { s: Int, i: Int -> },
    orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
){
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as Activity
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    val hasDetector by viewModel.hasDetector.collectAsState()
    val landscape by viewModel.landscape.collectAsState()
    val repDone by viewModel.repDone.collectAsState()
    val ex by viewModel.ex.collectAsState()
    val run by viewModel.run.collectAsState()
    val repExercisePoseDetectorUiState by viewModel.repExercisePoseDetectorUiState.collectAsState()

    val cameraUiState by viewModel.cameraUiState.collectAsStateWithLifecycle()

    val repCounter by viewModel.repCounter.collectAsState()
    val isBadPose by viewModel.isBadPose.collectAsState()
    val enablePoseClassifier by viewModel.enablePoseClassifier.collectAsState()

    val soundProbabilities by viewModel.soundProbabilities.collectAsState()

    activity.requestedOrientation = orientation

    BackHandler(true) {
        viewModel.backDialog()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (orientation!=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED){
                if (event == Lifecycle.Event.ON_START) {
                    activity.requestedOrientation = orientation
                } else if (event == Lifecycle.Event.ON_STOP) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
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

        when(val uiState = repExercisePoseDetectorUiState){
            RepExDetectorUiState.Initial -> {
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            RepExDetectorUiState.PlanDone -> {
                LaunchedEffect(repExercisePoseDetectorUiState){
//                    viewModel.freeRes()
                    onBackToList()
                }
            }
            RepExDetectorUiState.Saved -> {}
            RepExDetectorUiState.Saving -> {
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
            RepExDetectorUiState.Success -> {
                DetectorScreen(
                    paddingValues = it,
                    run = run,
                    ex = ex,
                    uiState = cameraUiState,
                    isBadPose = isBadPose,
                    onAnalyzeImage = viewModel::analyzeImage,
                    enablePoseClassifier = enablePoseClassifier,
                    rep = repCounter.count,
                    landscape = landscape,
                    context = context
                )
            }
            is RepExDetectorUiState.NextExercise -> {
                LaunchedEffect(repExercisePoseDetectorUiState) {
//                    viewModel.freeRes()
                    onNextExercise(run.runId, uiState.exScreen)
                }
            }
            RepExDetectorUiState.NavigateBack -> {
                LaunchedEffect(repExercisePoseDetectorUiState){
                    viewModel.freeRes()
                    onBackToList()
                }
            }
            RepExDetectorUiState.BackDialog -> {
                BackConfirmDialog(onConfirm = viewModel::confirmBackDialog, onCancel = viewModel::dismissBackDialog)
            }
            RepExDetectorUiState.PlanDoneDialog -> {
                PlanDoneDialog {
                    viewModel.dismissPlanDoneDialog()
                }
            }
        }
    }
}

@Composable
fun DetectorScreen(
    paddingValues: PaddingValues,
    run: ExerciseRun,
    ex: Exercise,
    uiState: CameraUiState,
    isBadPose: Boolean,
    onAnalyzeImage: (ImageProxy) -> Unit,
    enablePoseClassifier: Boolean,
    rep: Int,
    landscape: Boolean,
    context: Context,
){
    when (val result: CameraUiState = uiState) {
        is CameraUiState.Ready -> {
            val cameraState = rememberCameraState()
//            LaunchedEffect(cameraState.isStreaming){
//
//            }

            CameraSection(
                cameraState = cameraState,
                onAnalyzeImage = onAnalyzeImage,
            )

            LaunchedEffect(result.throwable) {
                if (result.throwable != null) {
                    Toast.makeText(context, result.throwable.message, Toast.LENGTH_SHORT).show()
                }
            }

            result.pose?.let {
                DrawPose(pose = it,isBadPose)
            }

            Column(Modifier.padding(paddingValues)) {
                if(enablePoseClassifier){
                    result.poseType1?.let {
                        DisplayCard(formatProb((it)))
                    }
                }
                DisplayCard("name: ${ex.name}")
                DisplayCard("set: ${(ceil(rep.toDouble()/ex.rep.toDouble())).toInt()}/${ex.set}")
                DisplayCard("rep: ${rep%ex.rep}/${ex.rep}")
                DisplayCard("rep done: $rep/${ex.set*ex.rep}")
            }
        }

        is CameraUiState.Initial -> {
            SpinnerScreen(msg = "Loading Camera", paddingValues = paddingValues)
        }
    }
}