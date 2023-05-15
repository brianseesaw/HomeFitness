package com.example.homefitness.ui.screen

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun DrawPose(
    pose: Pose,
    isBad: Boolean,
){

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

    DrawLine(start = leftShoulder, end = rightShoulder, isBad)
    DrawLine(start = leftShoulder, end = leftElbow, isBad)
    DrawLine(start = leftElbow, end = leftWrist, isBad)
    DrawLine(start = rightShoulder, end = rightElbow, isBad)
    DrawLine(start = rightElbow, end = rightWrist, isBad)
    DrawLine(start = leftShoulder, end = leftHip, isBad)
    DrawLine(start = rightShoulder, end = rightHip, isBad)
    DrawLine(start = leftHip, end = rightHip, isBad)
    DrawLine(start = rightHip, end = rightKnee, isBad)
    DrawLine(start = rightKnee, end = rightAnkle, isBad)
    DrawLine(start = leftHip, end = leftKnee, isBad)
    DrawLine(start = leftKnee, end = leftAnkle, isBad)

}

@Composable
fun DrawLine(
    start:PointF?,
    end:PointF?,
    isBad:Boolean,
){
    if(start != null && end != null){
        val context = LocalContext.current
        val device = Offset(
            context.resources.displayMetrics.widthPixels.toFloat(),
            0f
        )
        val startOffset = device.minus(Offset(start.x,-start.y))
        val endOffset = device.minus(Offset(end.x,-end.y))
        val lineColor = if(isBad) Color.Red else Color.Blue
        Canvas(
            modifier = Modifier
                .fillMaxSize(),
            onDraw = {
                drawLine(
                    lineColor,
                    startOffset,
                    endOffset,
                    strokeWidth = 30f
                )
            }
        )
    }
}