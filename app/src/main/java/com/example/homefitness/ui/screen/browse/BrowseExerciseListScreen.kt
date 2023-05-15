package com.example.homefitness.ui.screen.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.ExerciseList
import com.example.homefitness.ui.screen.exercise.ExerciseWithProgList
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object BrowseExListDestination : NavigationDestination {
    override val route = "browse_exercise_list"
    //    override val titleRes = "Home"
    const val planIdArg = "planId"
    val routeWithArgs = "$route/{$planIdArg}"
}

@Composable
fun BrowseExerciseListScreen(
    viewModel: BrowseExerciseListViewModel = getViewModel(),
    onBack: () -> Unit = {},
){
    val planName by viewModel.planName.collectAsState()
    val exerciseListUiState by viewModel.exerciseListUiState.collectAsState()
    val addPlanUiState by viewModel.addBrowsePlanUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = planName,
            onBack = onBack,
            onAdd = viewModel::addToMyPlan,
            topBarState = TopBarState.ADD_TO_MY,
        ) }
    ) {
        when(val uiState = exerciseListUiState){
            BrowseExListUiState.Initial -> {
                SpinnerScreen(msg = "Loading", paddingValues = it)
            }
            is BrowseExListUiState.Success -> {
                when(addPlanUiState){
                    AddBrowsePlanUiState.Added -> {
                        AddedPlanDialog {
                            onBack()
                        }
                    }
                    AddBrowsePlanUiState.Adding -> {
                        SpinnerScreen(msg = "Adding plan to My Plan", paddingValues = it)
                    }
                    AddBrowsePlanUiState.Initial -> {
                        Box(modifier = Modifier.padding(it)) {
                            Column() {
                                ExerciseList(exerciseList = uiState.exerciseList)
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun AddedPlanDialog(
    onConfirm: () -> Unit,
){
    AlertDialog(onDismissRequest = { /* Do nothing */ },
        title = { Text("Congratulations") },
        text = { Text("You have added this exercise plan to My Plan") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },)
}