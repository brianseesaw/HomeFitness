package com.example.homefitness.classifier

import android.content.Context
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class SoundClassifierProvider(private val context:Context) {

    var audioClassifier: AudioClassifier

    init {
        audioClassifier = AudioClassifier.createFromFile(context, MODEL_FILE)
    }

    fun create(){
        if(audioClassifier.isClosed){
            audioClassifier = AudioClassifier.createFromFile(context, MODEL_FILE)
        }
    }

    fun close(){
        audioClassifier.close()
    }

    companion object {
        private const val MODEL_FILE = "browserfft-speech.tflite"
    }
}