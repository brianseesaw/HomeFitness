package com.example.homefitness.util

import android.content.Context
import android.media.MediaPlayer
import com.example.homefitness.R

class AlertPlayer(
    private val context: Context,
){
    private var mediaPlayer = MediaPlayer.create(context, R.raw.typewriterbell)

    init {
        mediaPlayer.isLooping = false
    }

    fun onPlayComplete(onComplete: (Boolean) -> Unit){
        mediaPlayer = MediaPlayer.create(context, R.raw.typewriterbell)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            onComplete(false)
        }
    }

    fun onPlayError(onComplete: (Boolean) -> Unit){
        mediaPlayer = MediaPlayer.create(context, R.raw.error)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            onComplete(false)
        }
    }

}