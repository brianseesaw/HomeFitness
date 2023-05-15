package com.example.homefitness.util

data class RepCounter(
    private val onState: String = "",
    private val offState: String = "",
    var count:Int = 0,
    private val maxCount:Int = 0,
    private val setCount:Int = 0,
    private var currState: String = "",
    private var prevState: String = offState,
) {

    fun incrementCount(newState: String, onReachedMax:() -> Unit, onReachedSet:() -> Unit) {
        if(newState in listOf(onState, offState)){
            currState = newState
            if (
                currState != prevState && // if pose change
                currState == onState) // if pose is on
            {
                count++
                if(count!=0){
                    if (count%setCount == 0){
                        if (count==maxCount){
                            onReachedMax()
                        }else{
                            onReachedSet()
                        }
                    }
                }

            }
            prevState = currState
        }
    }
}