package com.example.homefitness.classifier

import android.content.Context
import com.google.mlkit.vision.pose.Pose
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.label.Category

class PoseClassifier(
    private val context: Context,
): AutoCloseable {
    private val options = Interpreter.Options().apply {
        numThreads = CPU_NUM_THREADS
    }

    var interpreter: Interpreter
    private lateinit var labels: List<String>
    private lateinit var input: IntArray
    private lateinit var output: IntArray

    init {
        interpreter = Interpreter(
            FileUtil.loadMappedFile(
                context, PushUpModel.MODEL_FILENAME
            ), options
        )

        input = interpreter.getInputTensor(0).shape()
        output = interpreter.getOutputTensor(0).shape()

        labels = FileUtil.loadLabels(context, PushUpModel.LABELS_FILENAME)
    }

    fun setModel (exerciseName: String){ // set model base on exercise
        when(exerciseName){
            "pushup"->{
                setInterpreter(PushUpModel.MODEL_FILENAME)
                setLabels(PushUpModel.LABELS_FILENAME)
            }
            "squat"->{
                setInterpreter(SquatModel.MODEL_FILENAME)
                setLabels(SquatModel.LABELS_FILENAME)
            }
            "plank"->{
                setInterpreter(PlankModel.MODEL_FILENAME)
                setLabels(PlankModel.LABELS_FILENAME)
            }
            "lunge"->{
                setInterpreter(LungeModel.MODEL_FILENAME)
                setLabels(LungeModel.LABELS_FILENAME)
            }
            else->{
                setInterpreter(PushUpModel.MODEL_FILENAME)
                setLabels(PushUpModel.LABELS_FILENAME)
            }
        }
    }

    fun setInterpreter (file:String){
        interpreter = Interpreter(
        FileUtil.loadMappedFile(
            context, file
        ), options
        )
        input = interpreter.getInputTensor(0).shape()
        output = interpreter.getOutputTensor(0).shape()
    }

    fun setLabels (file: String){
        labels = FileUtil.loadLabels(context, file)
    }

    fun classify(pose: Pose?): List<Category> {

        val inputVector = FloatArray(input[1])
        val outputTensor = FloatArray(output[1])
        val output = mutableListOf<Category>()

        pose?.let {
            if (pose.allPoseLandmarks.isNotEmpty()){

                pose.allPoseLandmarks.forEachIndexed { index, poseLandmark -> // convert pose landmarks to input vector for model
                    inputVector[index * 3] = poseLandmark.position3D.x
                    inputVector[index * 3 + 1] = poseLandmark.position3D.y
                    inputVector[index * 3 + 2] = poseLandmark.position3D.z
                }

                interpreter.run(arrayOf(inputVector), arrayOf(outputTensor)) // run inference
                outputTensor.forEachIndexed { index, score ->
                    output.add(Category(labels[index], score))
                }
            }

        }

        return output
    }

    override fun close() {
        interpreter.close()
    }

    companion object {

            object PushUpModel{
                const val MODEL_FILENAME = "pushup.tflite"
                const val LABELS_FILENAME = "pushup.txt"
            }

            object PlankModel{
                const val MODEL_FILENAME = "plank.tflite"
                const val LABELS_FILENAME = "plank.txt"
            }

            object LungeModel{
                const val MODEL_FILENAME = "lunge.tflite"
                const val LABELS_FILENAME = "lunge.txt"
            }

            object SquatModel{
                const val MODEL_FILENAME = "squat.tflite"
                const val LABELS_FILENAME = "squat.txt"
            }
        //        private const val MODEL_FILENAME = "classifier.tflite"
        //        private const val MODEL_FILENAME = "pose_classifier_no_angle.tflite"
        private const val CPU_NUM_THREADS = 4
    }
}

