package com.example.homefitness.ui.screen.exercise.run

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.core.os.HandlerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.classifier.PoseClassifier
import com.example.homefitness.classifier.SoundClassifierProvider
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.exercise.CameraUiState
import com.example.homefitness.util.AlertPlayer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class TimeExCamViewModel  (
    savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
    private var soundClassifierProvider: SoundClassifierProvider,
    private val poseClassifier: PoseClassifier,
    private val alertPlayer: AlertPlayer,
    ): ViewModel(){
    private val runIdArg: Int = checkNotNull(savedStateHandle[TimeExCamDestination.runIdArg])

    private val _ex = MutableStateFlow(Exercise())
    val ex = _ex.asStateFlow()

    private val _run = MutableStateFlow(ExerciseRun())
    val run = _run.asStateFlow()

    private val _repDone = MutableStateFlow(0)
    val repDone = _repDone.asStateFlow()

    private val _timeExerciseUiState = MutableStateFlow<TimeExDetectorUiState>(
        TimeExDetectorUiState.Initial
    )
    val timeExerciseUiState = _timeExerciseUiState.asStateFlow()

    private val _inExercise: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val inExercise: StateFlow<Boolean> get() = _inExercise

    private val _soundProbabilities = MutableStateFlow<List<Category>>(emptyList())
    val soundProbabilities = _soundProbabilities.asStateFlow()

    private val _time = MutableStateFlow(Duration.ZERO)
    val time = _time.asStateFlow()

    private var handler: Handler // background thread handler to run classification
    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null

    private var timer: Timer? = null

    private var timerTimeStamp = SystemClock.elapsedRealtime()

    private val _soundClassifierEnabled = MutableStateFlow(true)
    val soundClassifierEnabled = _soundClassifierEnabled.asStateFlow()

    // pose classifier start
    private var _alertPlaying = MutableStateFlow(false)
    val alertPlaying = _alertPlaying.asStateFlow()

    private var _isBadPose = MutableStateFlow(false)
    val isBadPose = _isBadPose.asStateFlow()

    private var _enablePoseClassifier = MutableStateFlow(false)
    val enablePoseClassifier = _enablePoseClassifier.asStateFlow()

    private var _alertTimeStamp = MutableStateFlow(System.currentTimeMillis())
    val alertTimeStamp = _alertTimeStamp.asStateFlow()

    // pose classifier end

    // pose detector start
    private val _cameraUiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState.Initial)
    val cameraUiState: StateFlow<CameraUiState> get() = _cameraUiState
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector: PoseDetector
    private val classificationExecutor = Executors.newSingleThreadExecutor()
    // pose detector end

    init {
        // Create a handler to run sound classification in a background thread
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)
        getExercise()
        poseDetector = PoseDetection.getClient(options)
        poseClassifier.setModel("plank") // default model to plank
        initCamera()
        setSoundClassifierEnabled(true)
    }

    private fun initCamera() {
        viewModelScope.launch {
            _cameraUiState.value = CameraUiState.Ready()
        }
    }

    fun getExercise(){
        viewModelScope.launch{
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .onStart { _timeExerciseUiState.update { TimeExDetectorUiState.Initial } }
                .filterNotNull()
                .first()
                .let {run->
                    _run.update { run }
                    _repDone.update { run.repDone }
                    exerciseRepositoryImpl.getExercisesByPlanOrderStream(run.planId,run.order)
                        .filterNotNull()
                        .first()
                        .firstOrNull()?.let {ex->
                            setExercise(ex)
                            _time.update { Duration.ofSeconds((ex.rep - (run.repDone % ex.rep)).toLong() )}
                        }
                }
        }.invokeOnCompletion {
            _timeExerciseUiState.update { TimeExDetectorUiState.Success }
        }
    }

    fun setExercise(value: Exercise){
        _ex.value = value
//        _landscape.update { ex.value.name in landscapeExercises }

        if(ex.value.name in detectorExercises) poseClassifier.setModel(ex.value.name)
    }

    fun setSoundClassifierEnabled(value: Boolean) {
        _soundClassifierEnabled.value = value
        if (soundClassifierEnabled.value){
            startAudioClassification()
        }else{
            stopAudioClassification()
        }
    }

    fun startAudioClassification() {
        if (audioClassifier != null) {
            return
        }else{
            soundClassifierProvider.create()
            audioClassifier = soundClassifierProvider.audioClassifier
        }

        audioClassifier?.let {classifier ->
            val audioTensor = classifier.createInputTensorAudio()

            val record = classifier.createAudioRecord() // Initialize the audio recorder
            record.startRecording()

            val run = object : Runnable {// Define the classification runnable
            override fun run() {
                val startTime = System.currentTimeMillis()

                audioTensor.load(record)// Load the latest audio sample
                val output = classifier.classify(audioTensor)

                val filteredModelOutput = output[0].categories.filter {// Filter classification result above threshold
                    it.score > MINIMUM_CONFIDENCE_THRESHOLD
                }.sortedByDescending { // sort result
                    it.score
                }

                filteredModelOutput.firstOrNull()?.let {
                    changeExerciseState(it.label)
                }

                val finishTime = System.currentTimeMillis()
                Log.d("AUDIO CLASSIFY LATENCY", "Latency = ${finishTime - startTime} ms")

                _soundProbabilities.update { filteredModelOutput }

                // Rerun the classification after a certain interval
                handler.postDelayed(this, DefaultClassificationInterval)
            }
            }

            // Start the classification process
            handler.post(run)
            audioRecord = record
        }
    }

    fun stopAudioClassification() {
        handler.removeCallbacksAndMessages(null)
        audioRecord?.stop()
        audioRecord = null
        audioClassifier?.close()
        audioClassifier = null
//        soundClassifierProvider.close()
    }

    fun changeExerciseState(sound: String){
        if (inExercise.value){
            if (sound =="stop"){
                _inExercise.update { false }
                _enablePoseClassifier.update { false }
                cancelTimer()
                updateExercise(TimeExDetectorUiState.Success)
            }
        }else{
            if (sound == "go"){
                _inExercise.update { true }
                _enablePoseClassifier.update { true }
                startCountDown()
            }
        }
    }

    //    timer start
    fun startCountDown() {
        if (timer != null) {
            cancelTimer()
        }

        timer = Timer()

        timerTimeStamp = SystemClock.elapsedRealtime()

        timer?.schedule(
            timerTask {
                val sysTime = SystemClock.elapsedRealtime()
                val elapsed = sysTime - timerTimeStamp
                val duration = time.value.minusMillis(elapsed)

                if(duration <= Duration.ZERO){
                    cancelTimer()
                    endSet()
                }else{
                    _time.update { duration }
                }

                timerTimeStamp = sysTime
            },
            Date(),
            DefaultClassificationInterval
        )
    }

    fun cancelTimer() {
        timer?.cancel()
    }

    fun endSet(){
        _inExercise.update { false }
        _enablePoseClassifier.update { false }
        stopAudioClassification()
        alertPlayer.onPlayComplete {startAudioClassification()}
        updateExercise(TimeExDetectorUiState.Success)
    }
    // timer end

    // pose detector + classifier start
    class PoseWithClassification(val pose: Pose, val classificationResult: List<Category>) // class to hold detected pose and classification result

    @WorkerThread
    @ExperimentalGetImage
    fun analyzeImage(imageProxy: ImageProxy) {
        viewModelScope.launch {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                poseDetector
                    .process(inputImage)
                    .continueWith(classificationExecutor){ task->
                        val pose = task.result
                        val classificationResult: List<Category> =
                            if(enablePoseClassifier.value) // if enabled
                                poseClassifier.classify(task.result) // classify pose
                            else
                                emptyList()
                        PoseWithClassification(pose,classificationResult)

                    }
                    .addOnSuccessListener { results ->

                        var poseType1: Category? = null
                        var poseType2: Category? = null
                        var poseType3: Category? = null

                        results.classificationResult
                            .filter { it.score > MINIMUM_CONFIDENCE_THRESHOLD } // filter list
                            .sortedByDescending { it.score } // sort by score
                            .let{ list ->
                                list.firstOrNull()?.let {
                                    poseType1 = it
                                    if (it.label in badPose){
                                        detectedBad()
                                    }else{
                                        _isBadPose.update{false}
                                    }
                                }
                                list.elementAtOrNull(1)?.let{poseType2 = it}
                                list.elementAtOrNull(2)?.let{poseType3 = it}
                            }

                        _cameraUiState.update {
                            CameraUiState.Ready(
                                pose = results.pose,
                                poseType1 = poseType1,
                                poseType2 = poseType2,
                                poseType3 = poseType3
                            )
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener { ex ->
                        _cameraUiState.update {
                            CameraUiState.Ready(
                                pose = null,
                                poseType1 = null,
                                poseType2 = null,
                                poseType3 = null
                            )
                        }
                        Log.e("analyzeImage",ex.message.toString())
                        imageProxy.close()
                    }
                    .addOnCompleteListener{
                        imageProxy.close()
                    }
            }
        }
    }

    fun detectedBad(){
        _isBadPose.update { true }
        if (!alertPlaying.value && // check if alert is already playing
            System.currentTimeMillis() - alertTimeStamp.value > TimeUnit.SECONDS.toMillis(4) // check time passed since last alert
        ){
            _alertPlaying.update{true}
            _alertTimeStamp.update { System.currentTimeMillis() }
            if (alertPlaying.value){
                stopAudioClassification()
                alertPlayer.onPlayError {
                    _alertPlaying.update{false} // play alert
                    startAudioClassification()
                }
            }
        }
    }
    // pose detector + classifier end

    fun updateExercise(completeState: TimeExDetectorUiState){
        _repDone.update { repDone.value-(repDone.value % ex.value.rep) + (ex.value.rep - time.value.seconds.toInt())}
        _time.update { Duration.ofSeconds((ex.value.rep - (repDone.value % ex.value.rep)).toLong() )}

        if (repDone.value >= (ex.value.rep * ex.value.set)){
            finishExercise() // done all rep
        }else{
            _timeExerciseUiState.update { TimeExDetectorUiState.Saving }
            viewModelScope.launch {
                _run.update { run.value.copy(
                    repDone=repDone.value,
                    runState = RunState.PAUSED,
                ) }
                exerciseRunRepositoryImpl.updateExerciseRun(run.value) // update rep
            }.invokeOnCompletion {
                _timeExerciseUiState.update { completeState }
            }
        }
    }

    fun finishExercise(){
        viewModelScope.launch{
            exerciseRepositoryImpl.getExercisesByPlanStream(run.value.planId)
                .onStart {
                    _timeExerciseUiState.update { TimeExDetectorUiState.Saving }
                    freeRes()
                }
                .filterNotNull()
                .first()
                .let {exList->
                    if(run.value.order == exList.count()){ // if count==runOrder all exercise in plan was ran
                        _run.update {
                            run.value.copy(  // update run as done
                                repDone=0,
                                runState = RunState.DONE)}
                    }else{
                        _run.update {
                            run.value.copy(  // update to run next exercise
                                repDone=0,
                                order = run.value.order+1,
                                runState = RunState.NEW)}

                        _ex.update { exList.first { it.runOrder == run.value.order }}
                    }
                    exerciseRunRepositoryImpl.updateExerciseRun(run.value)
                }

        }.invokeOnCompletion {
            if(run.value.runState == RunState.DONE) {
                _timeExerciseUiState.update { TimeExDetectorUiState.PlanDoneDialog }
            }
            if(run.value.runState == RunState.NEW) {
                _timeExerciseUiState.update { TimeExDetectorUiState.NextExercise(ex.value.toExerciseScreen()) }
            }
        }
    }


    fun freeRes(){
        Log.d("TIMEEXCAM VM FREE RES","VM CLEARED")
        cancelTimer()
//        classificationExecutor.shutdownNow() //TODO()
        poseClassifier.close()
        poseDetector.close()
        stopAudioClassification()
    }

    fun dismissPlanDoneDialog() = _timeExerciseUiState.update { TimeExDetectorUiState.PlanDone } // dismiss plan done dialog and navigate back
    fun backDialog() {
        stopAudioClassification()
        _inExercise.update { false }
        _enablePoseClassifier.update { false }
        cancelTimer()
        _cameraUiState.update { CameraUiState.Initial }
        _timeExerciseUiState.update { TimeExDetectorUiState.BackDialog }
    } // show back dialog
    fun confirmBackDialog() {
        updateExercise(TimeExDetectorUiState.NavigateBack) // update exercise repdone and navigate back
    }
    fun dismissBackDialog() {
        startAudioClassification()
        _timeExerciseUiState.update { TimeExDetectorUiState.Success }
        _cameraUiState.update { CameraUiState.Ready() }
    }


    override fun onCleared() {
        Log.d("TIMEEXCAM VM CLEARED","VM CLEARED")
        super.onCleared()
        freeRes()
    }

    companion object {
        private const val DefaultClassificationInterval = 500L
        private const val LOG_TAG = "AudioDemo"
        private const val MINIMUM_DISPLAY_THRESHOLD: Float = 0.3f
        private const val MINIMUM_CONFIDENCE_THRESHOLD: Float = 0.9f
        private val badPose = listOf("pushup_bad","plank_bad","lunge_bad","squat_bad")
        private val detectorExercises = listOf("pushup","plank","lunge","squat")

        private val landscapeExercises = listOf("pushup","plank")
    }
}

sealed interface TimeExDetectorUiState{
    object Initial: TimeExDetectorUiState
    object Saving: TimeExDetectorUiState
    object Saved: TimeExDetectorUiState
    object Success: TimeExDetectorUiState
    object PlanDoneDialog: TimeExDetectorUiState
    object PlanDone: TimeExDetectorUiState
    object BackDialog: TimeExDetectorUiState
    object NavigateBack: TimeExDetectorUiState
    data class NextExercise(val exScreen: Int = 0): TimeExDetectorUiState
//    data class Success(val exerciseList: List<Exercise> = listOf()): TimeExercisePoseDetectorUiState
}

