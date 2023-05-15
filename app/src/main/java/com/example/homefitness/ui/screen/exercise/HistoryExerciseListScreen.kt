package com.example.homefitness.ui.screen.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object HisExListDestination : NavigationDestination {
    override val route = "history_exercise_list"
    //    override val titleRes = "Home"
    const val runIdArg = "runId"
    const val planIdArg = "planId"
    val routeWithArgs = "$route/{$runIdArg}/{$planIdArg}"
}

@Composable
fun HistoryExerciseListScreen(
    viewModel: HistoryExerciseListViewModel = getViewModel(),
    onBack: () -> Unit = {},
){
    val planName by viewModel.planName.collectAsState()
    val exerciseListUiState by viewModel.exerciseListUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = planName,
            onBack = onBack,
            topBarState = TopBarState.EMPTY,
        ) }
    ) {
        when(val uiState = exerciseListUiState){
            HisExListUiState.Initial -> {
                SpinnerScreen(msg = "Loading", paddingValues = it)
            }
            is HisExListUiState.Success -> {
                Box(modifier = Modifier.padding(it)) {
                    Column() {
                        ExerciseWithProgList(exerciseList = uiState.exerciseList)
                    }
                }
            }
        }
    }
}