package com.example.homefitness.ui.screen.exercise.run


import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RepExCamViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val planRepositoryImpl: PlanRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
    private val poseClassifier: PoseClassifier,
    private val alertPlayer: AlertPlayer,
    private val soundClassifierProvider: SoundClassifierProvider,
):ViewModel() {

    private val runIdArg: Int = checkNotNull(savedStateHandle[RepExCamDestination.runIdArg])
     val oren: Int = checkNotNull(savedStateHandle[RepExCamDestination.orenArg])
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

    private val _repExDetectorUiState = MutableStateFlow<RepExDetectorUiState>(
        RepExDetectorUiState.Initial
    )
    val repExercisePoseDetectorUiState = _repExDetectorUiState.asStateFlow()

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
    private val _cameraUiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState.Initial)
    val cameraUiState: StateFlow<CameraUiState> get() = _cameraUiState
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private var poseDetector: PoseDetector?
    private val poseClassificationExecutor = Executors.newSingleThreadExecutor()
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


    init {
        Log.d("REPEXCAM VM INIT","VM INIT")
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)
        getExercise()
        poseDetector = PoseDetection.getClient(options)
//        poseClassifier.setModel("pushup") // default model to pushup

        setSoundClassifierEnabled(true)
        initCamera()
    }

    private fun initCamera() {
        viewModelScope.launch {
            _cameraUiState.value = CameraUiState.Ready()
        }
    }

    fun getExercise(){
        viewModelScope.launch{
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .onStart { _repExDetectorUiState.update { RepExDetectorUiState.Initial } }
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
            _repExDetectorUiState.update { RepExDetectorUiState.Success }
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

    fun setExerciseUiState(value: RepExDetectorUiState){
        _repExDetectorUiState.value = value
    }

    fun setExercise(value: Exercise){
        _ex.value = value
        _landscape.update { ex.value.name in landscapeExercises }

        _repCounter.update {
            when (ex.value.name){
                "pushup"-> RepCounter(
                    onState = "pushup_down",
                    offState = "pushup_up",
                    maxCount = ex.value.rep*ex.value.set,
                    setCount = ex.value.rep,
                    count = run.value.repDone)
                "lunge"-> RepCounter(
                    onState = "lunge",
                    offState = "stand",
                    maxCount = ex.value.rep*ex.value.set,
                    setCount = ex.value.rep,
                    count = run.value.repDone)
                "squat"-> RepCounter(
                    onState = "squat",
                    offState = "stand",
                    maxCount = ex.value.rep*ex.value.set,
                    setCount = ex.value.rep,
                    count = run.value.repDone)
                else -> RepCounter()
            }
        }

        if(ex.value.name in detectorExercises) poseClassifier.setModel(ex.value.name)
    }

    fun endSet(){
        stopAudioClassification()
        alertPlayer.onPlayComplete {startAudioClassification()}
    }

    fun updateExercise(completeState: RepExDetectorUiState){

        _repDone.update { repCounter.value.count }

        if (repDone.value == (ex.value.rep * ex.value.set)){
            finishExercise() // done all rep
        }else{
            _repExDetectorUiState.update { RepExDetectorUiState.Saving }
            viewModelScope.launch {
                _run.update { run.value.copy(
                    repDone=repDone.value,
                    runState = RunState.PAUSED,
                ) }
                exerciseRunRepositoryImpl.updateExerciseRun(run.value) // update rep
            }.invokeOnCompletion {
                _repExDetectorUiState.update { completeState }
            }
        }
    }

    fun finishExercise(){
        viewModelScope.launch{
            exerciseRepositoryImpl.getExercisesByPlanStream(run.value.planId)
                .onStart {
                    stopAudioClassification()
                    alertPlayer.onPlayComplete {} // play sound
                    freeRes()
                    _repExDetectorUiState.update { RepExDetectorUiState.Saving }
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
                _repExDetectorUiState.update { RepExDetectorUiState.PlanDoneDialog }
            }
            if(run.value.runState == RunState.NEW) {
                _repExDetectorUiState.update { RepExDetectorUiState.NextExercise(ex.value.toExerciseScreen()) }
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
                val startTime = System.currentTimeMillis()
                if (poseDetector==null){
                    poseDetector = PoseDetection.getClient(options)
                }
                poseDetector?.let {detector->
                    detector.process(inputImage)
                    .continueWith(poseClassificationExecutor){ task->
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

                        results.classificationResult
                            .filter { it.score > MINIMUM_CONFIDENCE_THRESHOLD } // filter list
                            .sortedByDescending { it.score } // sort by score
                            .let{ list ->
                                list.firstOrNull()?.let {
                                    poseType1 = it
                                    if (it.label in badPose){
                                        detectedBad()
                                    }else{
                                        repCounter.value.incrementCount(// increment count,
                                            it.label,
                                            {finishExercise()}, //call finish exercise on reach max count
                                            {endSet()} //call end set exercise on reach set count
                                        )
                                        _isBadPose.update{false}
                                    }
                                }
                            }

                        _cameraUiState.update {
                            CameraUiState.Ready(
                                pose = results.pose,
                                poseType1 = poseType1,
                            )
                        }

                        val finishTime = System.currentTimeMillis()
                        Log.d("POSE CLASSIFY LATENCY", "Latency = ${finishTime - startTime} ms")
                        imageProxy.close()
                    }
                    .addOnFailureListener { ex ->
                        _cameraUiState.update {
                            CameraUiState.Ready()
                        }
                        Log.e("analyzeImage",ex.message.toString())
                        imageProxy.close()
                    }
                    .addOnCompleteListener{
                        imageProxy.close()
                    }
                }
                if(poseDetector==null) imageProxy.close()
            }
        }
    }

    fun detectedBad(){
        _isBadPose.update { true }
        if (!alertPlaying.value && // check if alert is already playing
            System.currentTimeMillis() - alertTimeStamp.value > TimeUnit.SECONDS.toMillis(3) // check time passed since last alert
        ){
            _alertPlaying.update{true}
            _alertTimeStamp.update { System.currentTimeMillis() }
            if (alertPlaying.value){
                stopAudioClassification()
                alertPlayer.onPlayError {// play alert
                    _alertPlaying.update{false}
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
            soundClassifierProvider.create()
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
            _enablePoseClassifier.update { true }
            poseClassifier.setModel(ex.value.name)
        }else if(label == "stop"){
            _enablePoseClassifier.update { false }
            poseClassifier.close()
            _isBadPose.update { false }
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
    // audio classification end

    fun freeRes(){
        _cameraUiState.update { CameraUiState.Initial }
        Log.d("REPEXCAM VM FREE RES","VM CLEARED")
        poseDetector?.close()
        poseDetector = null
        poseClassifier.close()
        stopAudioClassification()
//        poseClassificationExecutor.shutdown();
    }

    fun dismissPlanDoneDialog() = _repExDetectorUiState.update { RepExDetectorUiState.PlanDone } // dismiss plan done dialog and navigate back
    fun backDialog() {
        _cameraUiState.update { CameraUiState.Initial }
        stopAudioClassification()
        togglePoseClassifier("stop")
        _repExDetectorUiState.update { RepExDetectorUiState.BackDialog } // show back dialog
    }
    fun confirmBackDialog() {
        viewModelScope.launch {
            freeRes()
        }.invokeOnCompletion {
            updateExercise(RepExDetectorUiState.NavigateBack) // update exercise repdone and navigate back
        }
    }
    fun dismissBackDialog() {
        startAudioClassification()
        _repExDetectorUiState.update { RepExDetectorUiState.Success }
        _cameraUiState.update { CameraUiState.Ready() }
    }

    override fun onCleared() {
        Log.d("REPEXCAM VM CLEARED","VM CLEARED")
        super.onCleared()
        freeRes()
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

sealed interface RepExDetectorUiState{
    object Initial: RepExDetectorUiState
    object Saving: RepExDetectorUiState
    object Saved: RepExDetectorUiState
    object Success: RepExDetectorUiState
    object PlanDoneDialog: RepExDetectorUiState
    object PlanDone: RepExDetectorUiState
    object BackDialog: RepExDetectorUiState
    object NavigateBack: RepExDetectorUiState
    data class NextExercise(val exScreen: Int = 0): RepExDetectorUiState
//    data class Success(val exerciseList: List<Exercise> = listOf()): RepExercisePoseDetectorUiState
}