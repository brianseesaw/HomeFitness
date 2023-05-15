package com.example.homefitness.ui.screen.exercise

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Size
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homefitness.data.ExerciseType
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.run.ExerciseContent
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import com.ujizin.camposer.CameraPreview
import com.ujizin.camposer.state.*
import org.koin.androidx.compose.getViewModel
import org.tensorflow.lite.support.label.Category


object ExerciseDestination : NavigationDestination {
    override val route = "exercise"
//    override val titleRes = "Home"
    const val runIdArg = "runId"
    val routeWithArgs = "$route?$runIdArg={$runIdArg}"
}

@ExperimentalGetImage
@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onStop: () -> Unit = {},
){
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as Activity
    val configuration = LocalConfiguration.current

    val hasDetector by viewModel.hasDetector.collectAsState()
    val portrait by viewModel.landscape.collectAsState()
    val repDone by viewModel.repDone.collectAsState()
    val ex by viewModel.ex.collectAsState()
    val run by viewModel.run.collectAsState()
    val exerciseUiState by viewModel.exerciseUiState.collectAsState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val repCounter by viewModel.repCounter.collectAsState()
    val isBadPose by viewModel.isBadPose.collectAsState()
    val enableClassifier by viewModel.enablePoseClassifier.collectAsState()

    val time by viewModel.time.collectAsState()

    val soundProbabilities by viewModel.soundProbabilities.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if(portrait){
                if (event == Lifecycle.Event.ON_START) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else if (event == Lifecycle.Event.ON_STOP) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = { TopBar(
            title = ExerciseDestination.route,
            onBack = onBack,
            topBarState = TopBarState.NONE,
        ) }
    ) {

        when(exerciseUiState){
            ExerciseUiState.Initial -> {
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            ExerciseUiState.PlanDone -> {
                LaunchedEffect(exerciseUiState){
                    onStop()
                    viewModel.setExerciseUiState(ExerciseUiState.Initial)
                }
            }
            ExerciseUiState.Saved -> {}
            ExerciseUiState.Saving -> {
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
            ExerciseUiState.Success -> {
                if(hasDetector){
                    if(ex.type == ExerciseType.TIME){
                    }else if(ex.type == ExerciseType.REP){
//                        DetectorScreen(uiState = uiState, isBadPose = isBadPose, onAnalyzeImage = viewModel::analyzeImage, enablePoseClassifier = enableClassifier, rep = repCounter.count)
                    }

                }else{

                    if(ex.type == ExerciseType.TIME){
//                        ExerciseContentWithTimer(
//                            paddingValues = it,
//                            run = run,
//                            ex = ex,
////                            repDoneTemp = run.repDone,
////                            repDone = repDone,
//                            time = time,
//                            probabilities = soundProbabilities
//                        )
                    }else if(ex.type == ExerciseType.REP){
                        ExerciseContent(
                            paddingValues = it,
                            run = run,
                            ex = ex,
                            repDoneTemp = run.repDone,
                            repDone =repDone,
                            onFinish = viewModel::finishExercise,
                            onPause = viewModel::updateExercise,
                            onRepDoneChange = viewModel::setRepDone)
                    }
                }
            }
        }

    }
}






@Composable
fun DisplayCard(
    txt:String
){
    Card(backgroundColor = Color.White) {
        Text(text = txt, fontSize = 40.sp)
    }
}



@Composable
fun CameraSection(
//    context: Context,
    cameraState: CameraState,
    onAnalyzeImage: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current

    val imageAnalyzer = cameraState.rememberImageAnalyzer(
        imageAnalysisTargetSize = ImageAnalysisTargetSize(Size(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels)),
        analyze = onAnalyzeImage)

    CameraPreview(
        cameraState = cameraState,
        camSelector = CamSelector.Front,
        imageAnalyzer = imageAnalyzer,
        isFocusOnTapEnabled = false,
        isPinchToZoomEnabled = false,
        implementationMode = ImplementationMode.Performance
    )
}

fun formatProb(cat: Category?): String {
    if (cat == null) return "empty"
    return "${cat.label} (${String.format("%.2f", cat.score)})"
}

