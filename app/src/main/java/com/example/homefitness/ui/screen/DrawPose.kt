package com.example.homefitness.ui.screen

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.rotationMatrix
import com.example.homefitness.ui.screen.exercise.DisplayCard
import com.example.homefitness.ui.screen.exercise.formatProb
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.ceil

@Composable
fun DrawPose(
    pose: Pose,
    isBad: Boolean,
){
    val configuration = LocalConfiguration.current
    val device = Offset(
        configuration.screenWidthDp.toFloat(),
        configuration.screenHeightDp.toFloat(),
    )

    val leftShoulder : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position;
    val rightShoulder : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position;
    val leftElbow : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position;
    val rightElbow : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position;
    val leftHip : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position;
    val rightHip : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position;
    val leftKnee : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position;
    val rightKnee : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position;
    val leftAnkle : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position;
    val rightAnkle : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position;
    val leftWrist : PointF? = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.position;
    val rightWrist : PointF? = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position;
    val nose : PointF? = pose.getPoseLandmark(PoseLandmark.NOSE)?.position;

    DrawLine(start = leftShoulder, end = rightShoulder, device, isBad)
    DrawLine(start = leftShoulder, end = leftElbow, device, isBad)
    DrawLine(start = leftElbow, end = leftWrist, device, isBad)
    DrawLine(start = rightShoulder, end = rightElbow, device, isBad)
    DrawLine(start = rightElbow, end = rightWrist, device, isBad)
    DrawLine(start = leftShoulder, end = leftHip, device, isBad)
    DrawLine(start = rightShoulder, end = rightHip, device, isBad)
    DrawLine(start = leftHip, end = rightHip, device, isBad)
    DrawLine(start = rightHip, end = rightKnee, device, isBad)
    DrawLine(start = rightKnee, end = rightAnkle, device, isBad)
    DrawLine(start = leftHip, end = leftKnee, device, isBad)
    DrawLine(start = leftKnee, end = leftAnkle, device, isBad)

}

@Composable
fun DrawLine(
    start:PointF?,
    end:PointF?,
    device:Offset,
    isBad:Boolean,
){
    if(start != null && end != null){

        val startOffset = Offset((start.x*device.x/256),start.y*device.y/256)
        val endOffset = Offset((end.x*device.x/256),end.y*device.y/256)
        val lineColor = if(isBad) Color.Red else Color.Blue
        Canvas(
            modifier = Modifier
                .fillMaxSize(),
            onDraw = {
                scale(-1f,1f){ // scale to mirror
                    drawLine(
                        lineColor,
                        startOffset,
                        endOffset,
                        strokeWidth = 30f
                    )
                }

            }
        )
//
//        Column() {
//            DisplayCard(device.x.toString())
//            DisplayCard(device.y.toString())
//            DisplayCard(context.resources.displayMetrics.widthPixels.toString())
//            DisplayCard(context.resources.displayMetrics.heightPixels.toString())
//        }
    }
}