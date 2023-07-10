package com.example.homefitness.ui.screen.exercise

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
import com.example.homefitness.util.AlertPlayer
import com.example.homefitness.util.RepCounter
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

class ExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
    private val classifier: PoseClassifier,
    private val alertPlayer: AlertPlayer,
    private val soundClassifierProvider: SoundClassifierProvider,
):ViewModel() {

    private val runIdArg: Int = checkNotNull(savedStateHandle[ExerciseDestination.runIdArg])

    private val _ex = MutableStateFlow(Exercise())
    val ex = _ex.asStateFlow()

    private val _run = MutableStateFlow(ExerciseRun())
    val run = _run.asStateFlow()

    private val _repDone = MutableStateFlow(0)
    val repDone = _repDone.asStateFlow()

    private val _type = MutableStateFlow(ExerciseType.REP)
    val type = _type.asStateFlow()

    private var _landscape = MutableStateFlow(false)
    val landscape = _landscape.asStateFlow()

    private var _hasDetector = MutableStateFlow(false)
    val hasDetector = _hasDetector.asStateFlow()

    private val _exerciseUiState = MutableStateFlow<ExerciseUiState>(ExerciseUiState.Initial)
    val exerciseUiState = _exerciseUiState.asStateFlow()

    // pose classifier start
    private var _alertPlaying = MutableStateFlow(false)
    val alertPlaying = _alertPlaying.asStateFlow()

    private var _isBadPose = MutableStateFlow(false)
    val isBadPose = _isBadPose.asStateFlow()

    private var _enablePoseClassifier = MutableStateFlow(false)
    val enablePoseClassifier = _enablePoseClassifier.asStateFlow()

    private var _alertTimeStamp = MutableStateFlow(System.currentTimeMillis())
    val alertTimeStamp = _alertTimeStamp.asStateFlow()

    private var _repCounter = MutableStateFlow(RepCounter())
    val repCounter = _repCounter.asStateFlow()
    // pose classifier end

    // pose detector start
    private val _uiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState.Initial)
    val uiState: StateFlow<CameraUiState> get() = _uiState
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector: PoseDetector
    private val classificationExecutor = Executors.newSingleThreadExecutor()
    // pose detector end

    // sound classifier start
    private val _classificationInterval = MutableStateFlow(CommandClassificationInterval) // How often should classification run in milliseconds
    val classificationInterval = _classificationInterval.asStateFlow()

    private val _soundProbabilities = MutableStateFlow<List<Category>>(emptyList())
    val soundProbabilities = _soundProbabilities.asStateFlow()

    private var handler: Handler // background thread handler to run classification
    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null

    private val _soundClassifierEnabled = MutableStateFlow(true)
    val soundClassifierEnabled = _soundClassifierEnabled.asStateFlow()
    // sound classifier end

//    timer start
    private val _time = MutableStateFlow(Duration.ZERO)
    val time = _time.asStateFlow()

    private var timer: Timer? = null

    private var timerTimeStamp = SystemClock.elapsedRealtime()
//    timer end

    init {
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)
        getExercise()
        poseDetector = PoseDetection.getClient(options)
        initCamera()
//        classificationExecutor = Executors.newSingleThreadExecutor()
//        setExercise(Exercise(name = "plank"))

    }

    private fun initCamera() {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Ready()
        }
    }

    fun getExercise(){
        viewModelScope.launch{
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .onStart { _exerciseUiState.update { ExerciseUiState.Initial } }
                .filterNotNull()
                .first()
                .let {run->
                    _run.update { run }
                    _repDone.update { run.repDone }
                    exerciseRepositoryImpl.getExercisesByPlanOrderStream(run.planId,run.order)
                        .filterNotNull()
                        .first()
                        .firstOrNull()?.let {
                            setExercise(it)
                        }
                }
        }.invokeOnCompletion {
            _exerciseUiState.update { ExerciseUiState.Success }
        }
    }

    fun setSoundClassifierEnabled(value: Boolean) {
        _soundClassifierEnabled.value = value
        if (soundClassifierEnabled.value){
            startAudioClassification()
        }else{
            stopAudioClassification()
        }
    }

    fun setRepDone(value:Int){
        _repDone.value = value
    }

    fun setExerciseUiState(value: ExerciseUiState){
        _exerciseUiState.value = value
    }

    fun setExercise(value: Exercise){
        _ex.value = value
        _landscape.update { ex.value.name in landscapeExercises }
        _hasDetector.update { ex.value.name in detectorExercises }

        if(hasDetector.value){
            classifier.setModel(ex.value.name)
            setSoundClassifierEnabled(true)
            if(ex.value.type == ExerciseType.TIME){

                _time.update { Duration.ofSeconds((ex.value.rep - (run.value.repDone % ex.value.rep)).toLong() )}

            }else if (ex.value.type == ExerciseType.REP){

                _repCounter.update {
                    when (ex.value.name){
                        "pushup"-> RepCounter(onState = "pushup_down", offState = "pushup_up", maxCount = ex.value.rep*ex.value.set, count = run.value.repDone)
                        "lunge"-> RepCounter(onState = "lunge", offState = "stand", maxCount = ex.value.rep*ex.value.set, count = run.value.repDone)
                        "squat"-> RepCounter(onState = "squat", offState = "stand", maxCount = ex.value.rep*ex.value.set, count = run.value.repDone)
                        else -> RepCounter()
                    }
                }

            }
        }else{
            if(ex.value.type == ExerciseType.TIME){

                setSoundClassifierEnabled(true)
                _time.update { Duration.ofSeconds((ex.value.rep - (run.value.repDone % ex.value.rep)).toLong() )}

            }else if (ex.value.type == ExerciseType.REP){

                setSoundClassifierEnabled(false)

            }
        }
    }

    fun updateExercise(){
        if(hasDetector.value){
            if(ex.value.type==ExerciseType.REP){
                _repDone.update { repCounter.value.count }
            }else if(ex.value.type==ExerciseType.TIME){
                _repDone.update { repDone.value-(repDone.value % ex.value.rep) + (ex.value.rep -time.value.seconds.toInt())}
            }
        }else{
            if(ex.value.type==ExerciseType.REP){

            }else if(ex.value.type==ExerciseType.TIME){
                _repDone.update { (repDone.value-(repDone.value % ex.value.rep)) + (ex.value.rep -time.value.seconds.toInt())}
            }
        }

        if (repDone.value == (ex.value.rep * ex.value.set)){
            finishExercise() // done all rep
        }else{
            _exerciseUiState.update { ExerciseUiState.Saving }
            viewModelScope.launch {
                _run.update { run.value.copy(
                    repDone=repDone.value,
                    runState = RunState.PAUSED,
                ) }
                exerciseRunRepositoryImpl.updateExerciseRun(run.value) // update rep
            }.invokeOnCompletion {
                _exerciseUiState.update { ExerciseUiState.Success }
            }
        }
    }

    fun finishExercise(){
        viewModelScope.launch{
            exerciseRepositoryImpl.getExercisesByPlanStream(run.value.planId)
                .onStart { _exerciseUiState.update { ExerciseUiState.Saving } }
                .filterNotNull()
                .first()
                .count().let {
                    if(run.value.order == it){ // if count==runOrder all exercise in plan was ran
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
                    }
                    exerciseRunRepositoryImpl.updateExerciseRun(run.value)
                }

        }.invokeOnCompletion {
            if(run.value.runState == RunState.DONE) {
                _exerciseUiState.update { ExerciseUiState.PlanDone }
            }
            if(run.value.runState == RunState.NEW) {
                _exerciseUiState.update { ExerciseUiState.Success }
                getExercise()
            }
        }
    }


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
                                classifier.classify(task.result) // classify pose
                            else
                                emptyList()
                        PoseWithClassification(pose,classificationResult)

                    }
                    .addOnSuccessListener { results ->

                        var poseType1: Category = Category("", 0F)
                        var poseType2: Category = Category("", 0F)
                        var poseType3: Category = Category("", 0F)

                        results.classificationResult
                            .filter { it.score > MINIMUM_CONFIDENCE_THRESHOLD } // filter list
                            .sortedByDescending { it.score } // sort by score
                            .let{ list ->
                                list.firstOrNull()?.let {
                                    poseType1 = it
                                    if (it.label in badPose){
                                        detectedBad()
                                    }else{
//                                        repCounter.value.incrementCount(it.label) { finishExercise() } // increment count, call finish exercise if reach max count
                                        _isBadPose.update{false}
                                    }
                                }
                            }

                        _uiState.update {
                            CameraUiState.Ready(
                                pose = results.pose,
                                poseType1 = poseType1,
                            )
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener { ex ->
                        _uiState.update {
                            CameraUiState.Ready()
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

    // audio classification start
    fun startAudioClassification() {
        if (audioClassifier != null) {
            return // If the audio classifier is initialized and running, do nothing.
        }else{
            audioClassifier = soundClassifierProvider.audioClassifier // get classifier from provider
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
                    togglePoseClassifier(it.label)
                }

                val finishTime = System.currentTimeMillis()
                Log.d("AUDIO CLASSIFY LATENCY", "Latency = ${finishTime - startTime} ms")

                _soundProbabilities.update { filteredModelOutput }

                // Rerun the classification after a certain interval
                handler.postDelayed(this, classificationInterval.value)
            }
            }

            handler.post(run) // Start the classification process

            audioRecord = record // save audiorecord obj
        }
    }

    fun togglePoseClassifier(label:String){
        if(label == "go") {
//            _enablePoseClassifier.update { true }
            startCountDown()
            updateExercise()
        }else if(label == "stop"){
            cancelTimer()
//            _enablePoseClassifier.update { false }
        }
    }

    fun stopAudioClassification() {
        handler.removeCallbacksAndMessages(null)
        audioRecord?.stop()
        audioRecord = null
        audioClassifier = null
    }
    // audio classification end

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
                    updateExercise()
                }else{
                    _time.update { duration }
                }

                timerTimeStamp = sysTime
            },
            Date(),
            classificationInterval.value // use interval same as sound classification to sync timer
        )
    }
    fun cancelTimer() {
        timer?.cancel()
    }

//    timer end

    override fun onCleared() {
        super.onCleared()
        classificationExecutor.shutdown()
//        classifier.close()
        stopAudioClassification()
        cancelTimer()
    }

    companion object {
        private val badPose = listOf("pushup_bad","plank_bad","lunge_bad","squat_bad")
        private val detectorExercises = listOf("pushup","plank","lunge","squat")
        private val landscapeExercises = listOf("pushup","plank")

        private const val MINIMUM_CONFIDENCE_THRESHOLD: Float = 0.9f
        private const val TIMEOUT_MILLIS = 5_000L
        const val CommandClassificationInterval = 500L // interval to classify command
    }
}

sealed interface ExerciseUiState{
    object Initial: ExerciseUiState
    object Saving: ExerciseUiState
    object Saved: ExerciseUiState
    object Success: ExerciseUiState
    object PlanDone: ExerciseUiState
//    data class Success(val exerciseList: List<Exercise> = listOf()): ExerciseUiState
}

sealed interface CameraUiState {
    object Initial : CameraUiState
    data class Ready(
        val pose: Pose? = null,
        val throwable: Throwable? = null,
        var poseType1 : Category? = null,
    ) : CameraUiState
}