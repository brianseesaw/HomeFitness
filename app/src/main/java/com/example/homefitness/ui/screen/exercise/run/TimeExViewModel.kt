package com.example.homefitness.ui.screen.exercise.run

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.core.os.HandlerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homefitness.classifier.SoundClassifierProvider
import com.example.homefitness.data.*
import com.example.homefitness.ui.screen.exercise.CameraUiState
import com.example.homefitness.util.AlertPlayer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.time.Duration
import java.util.*
import kotlin.concurrent.timerTask

class TimeExViewModel (
    savedStateHandle: SavedStateHandle,
    private val exerciseRepositoryImpl: ExerciseRepositoryImpl,
    private val exerciseRunRepositoryImpl: ExerciseRunRepositoryImpl,
    private var soundClassifierProvider:SoundClassifierProvider,
    private val alertPlayer: AlertPlayer,

    ): ViewModel(){
    private val runIdArg: Int = checkNotNull(savedStateHandle[TimeExDestination.runIdArg])

    private val _ex = MutableStateFlow(Exercise())
    val ex = _ex.asStateFlow()

    private val _run = MutableStateFlow(ExerciseRun())
    val run = _run.asStateFlow()

    private val _repDone = MutableStateFlow(0)
    val repDone = _repDone.asStateFlow()

    private val _timeExUiState = MutableStateFlow<TimeExUiState>(TimeExUiState.Initial)
    val timeExerciseUiState = _timeExUiState.asStateFlow()

    private val _inExercise: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val inExercise: StateFlow<Boolean> get() = _inExercise

    private val _soundProbabilities = MutableStateFlow<List<Category>>(emptyList())
    val soundProbabilities = _soundProbabilities.asStateFlow()

    private val _time = MutableStateFlow(Duration.ZERO)
    val time = _time.asStateFlow()

    private var handler: Handler // background thread handler to run classification
    private var handlerThread: HandlerThread = HandlerThread("backgroundThread")
    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null

    private var timer: Timer? = null

    private var timerTimeStamp = SystemClock.elapsedRealtime()

    private val _classifierEnabled = MutableStateFlow(true)
    val classifierEnabled = _classifierEnabled.asStateFlow()

    init {
        // Create a handler to run classification in a background thread
//        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)
        getExercise()
        setSoundClassifierEnabled(true)
    }

    fun getExercise(){
        viewModelScope.launch{
            exerciseRunRepositoryImpl.getExerciseRunStream(runIdArg)
                .onStart { _timeExUiState.update { TimeExUiState.Initial } }
                .filterNotNull()
                .first()
                .let {run->
                    _run.update { run }
                    _repDone.update { run.repDone }
                    exerciseRepositoryImpl.getExercisesByPlanOrderStream(run.planId,run.order)
                        .filterNotNull()
                        .first()
                        .firstOrNull()?.let {ex->
                            _ex.update { ex }
                            _time.update { Duration.ofSeconds((ex.rep - (run.repDone % ex.rep)).toLong() )}
                        }
                }
        }.invokeOnCompletion {
            _timeExUiState.update { TimeExUiState.Success }
        }
    }

    fun setSoundClassifierEnabled(value: Boolean) {
        _classifierEnabled.value = value
        if (classifierEnabled.value){
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
//        handlerThread.quitSafely()
        audioRecord?.stop()
        audioRecord = null
        audioClassifier?.close()
        audioClassifier = null
    }

    fun changeExerciseState(sound: String){
            if (inExercise.value){
                if (sound =="stop"){
                    _inExercise.update { false }
                    cancelTimer()
                    updateExercise(TimeExUiState.Success)
                }
            }else{
                if (sound == "go"){
                    _inExercise.update { true }
                    startCountDown()
                }
            }
    }

    //    timer
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
        stopAudioClassification()
        alertPlayer.onPlayComplete {startAudioClassification()}
        updateExercise(TimeExUiState.Success)
    }

    fun updateExercise(completeState: TimeExUiState){
        _repDone.update { repDone.value-(repDone.value % ex.value.rep) + (ex.value.rep - time.value.seconds.toInt())}
        _time.update { Duration.ofSeconds((ex.value.rep - (repDone.value % ex.value.rep)).toLong() )}

        if (repDone.value >= (ex.value.rep * ex.value.set)){
            finishExercise() // done all rep
        }else{
            _timeExUiState.update { TimeExUiState.Saving }
            viewModelScope.launch {
                _run.update { run.value.copy(
                    repDone=repDone.value,
                    runState = RunState.PAUSED,
                ) }
                exerciseRunRepositoryImpl.updateExerciseRun(run.value) // update rep
            }.invokeOnCompletion {
                _timeExUiState.update { completeState }
            }
        }
    }

    fun finishExercise(){
        viewModelScope.launch{
            exerciseRepositoryImpl.getExercisesByPlanStream(run.value.planId)
                .onStart {
                    _timeExUiState.update { TimeExUiState.Saving }
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
                _timeExUiState.update { TimeExUiState.PlanDoneDialog }
            }
            if(run.value.runState == RunState.NEW) {
                _timeExUiState.update { TimeExUiState.NextExercise(ex.value.toExerciseScreen()) }
            }
        }
    }


    fun dismissPlanDoneDialog() = _timeExUiState.update { TimeExUiState.PlanDone } // dismiss plan done dialog and navigate back
    fun backDialog() {
        stopAudioClassification()
        _inExercise.update { false }
        cancelTimer()
        _timeExUiState.update { TimeExUiState.BackDialog }
    } // show back dialog
    fun confirmBackDialog() {
        updateExercise(TimeExUiState.NavigateBack) // update exercise repdone and navigate back
    }
    fun dismissBackDialog() {
        startAudioClassification()
        _timeExUiState.update { TimeExUiState.Success }
    }

    fun freeRes(){
        stopAudioClassification()
        cancelTimer()
    }
    override fun onCleared() {
        super.onCleared()
        freeRes()
    }

    companion object {
        private const val DefaultClassificationInterval = 500L
        private const val LOG_TAG = "AudioDemo"
        private const val MINIMUM_DISPLAY_THRESHOLD: Float = 0.3f
        private const val MINIMUM_CONFIDENCE_THRESHOLD: Float = 0.9f

        private val detectorExercises = listOf("pushup","plank","lunge","squat")
    }
}

sealed interface TimeExUiState{
    object Initial: TimeExUiState
    object Saving: TimeExUiState
    object Saved: TimeExUiState
    object Success: TimeExUiState
    object PlanDoneDialog: TimeExUiState
    object PlanDone: TimeExUiState
    object BackDialog: TimeExUiState
    object NavigateBack: TimeExUiState
    data class NextExercise(val exScreen: Int = 0): TimeExUiState
//    data class Success(val exerciseList: List<Exercise> = listOf()): TimeExerciseUiState
}

