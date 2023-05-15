package com.example.homefitness.ui.screen.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.homefitness.data.Exercise
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object ExerciseListDestination : NavigationDestination {
    override val route = "exercise_list"
//    override val titleRes = "Home"
    const val planIdArg = "planId"
    val routeWithArgs = "$route/{$planIdArg}"
}

@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onAdd: (Int) -> Unit = {},
    onEdit: (Int) -> Unit = {},
    onRun: (Int,Int) -> Unit = {id: Int, screen: Int ->},
){
    val planName by viewModel.planName.collectAsState()
    val exerciseListUiState by viewModel.exerciseListUiState.collectAsState()
    val selectedList by viewModel.selectedItem.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val planId by viewModel.planId.collectAsState()
    val runPlanId by viewModel.runPlanId.collectAsState()
    val insertRunUiState by viewModel.insertRunUiState.collectAsState()
    val deletePlanUiState by viewModel.deletePlanUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = planName,
            onBack = onBack,
            onDelete = viewModel::promptDelete,
            onRun = viewModel::onRun,
            topBarState = TopBarState.DELETE_RUN,
        ) }
    ) {
        when(val uiState = exerciseListUiState){
            ExerciseListUiState.Initial -> {
                SpinnerScreen(msg = "Loading", paddingValues = it)
            }
            is ExerciseListUiState.Success -> {

                when(val deleteUiState = deletePlanUiState){
                    DeletePlanUiState.Confirmation -> {
                        ConfirmationDialog(title = "Confirm Delete", text = "Deleting plan ${planName}. Action in irreversible",
                            onConfirm = viewModel::deletePlan,
                            onCancel = viewModel::dismissDeletePrompt
                        )
                    }
                    DeletePlanUiState.Deleted ->  {
                        LaunchedEffect(uiState){
                            onBack()
                        }
                    }
                    DeletePlanUiState.Deleting -> {
                        SpinnerScreen(msg = "Deleting Plan", paddingValues = it)
                    }
                    DeletePlanUiState.Initial -> {
                        when (val insertUiState = insertRunUiState){

                            InsertRunUiState.Idle -> {
                                Box(modifier = Modifier.padding(it)){
                                    Column() {
                                        ExerciseList(exerciseList = uiState.exerciseList)
                                    }
                                }
                            }

                            is InsertRunUiState.InsertedRun -> {
                                LaunchedEffect(insertRunUiState){
                                    onRun(insertUiState.runId,insertUiState.exScreen)
                                    viewModel.setInsertRunUiState(InsertRunUiState.Idle)
                                }
                            }

                            InsertRunUiState.InsertingRun -> {
                                SpinnerScreen(msg = "Loading Exercise to Run", paddingValues = it)
                            }
                        }
                    }
                }

            }
        }

    }
}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExerciseList(
    exerciseList: List<Exercise>,
    onItemClick:(Int) -> Unit = {},
){
    if (exerciseList.isEmpty()) {
        Text(
            text = "No exercise found",
            textAlign = TextAlign.Center
        )
    }else{
        LazyColumn() {
            itemsIndexed(exerciseList){index, item ->
                ListItem(
                    text = { Text(item.name) },
                    secondaryText = { Text("set: "+item.set.toString()+" rep: "+item.rep.toString())},
                    trailing = {},
                    modifier = Modifier.clickable {
                        onItemClick(item.exerciseId)
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(onDismissRequest = { /* Do nothing */ },
        title = { Text(title) },
        text = { Text(text) },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        })
}