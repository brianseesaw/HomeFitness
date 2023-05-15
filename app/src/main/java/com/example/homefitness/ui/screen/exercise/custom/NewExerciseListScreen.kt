package com.example.homefitness.ui.screen.exercise.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.homefitness.data.Exercise
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.exercise.ConfirmationDialog
import com.example.homefitness.ui.screen.exercise.DeletePlanUiState
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object NewExerciseListDestination : NavigationDestination {
    override val route = "new_exercise_list"
    //    override val titleRes = "Home"
    const val planIdArg = "planId"
    val routeWithArgs = "$route/{$planIdArg}"
}

@Composable
fun NewExerciseListScreen(
    viewModel: NewExerciseListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onAdd: (Int) -> Unit = {},
    onEdit: (Int) -> Unit = {},
){
    val planName by viewModel.planName.collectAsState()
    val exerciseListUiState by viewModel.exerciseListUiState.collectAsState()
    val selectedList by viewModel.selectedItem.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val planId by viewModel.planId.collectAsState()
    val runPlanId by viewModel.runPlanId.collectAsState()
    val saveUiState by viewModel.saveUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = planName,
            onBack = viewModel::promptSave,
            onAdd = {onAdd(planId)},
            onEdit = {
                selectedList.firstOrNull()?.let{
                    onEdit(it)
                }
            },
            onDelete = viewModel::deleteExercise,
            onMoveUp = viewModel::moveExerciseUp,
            onMoveDown = viewModel::moveExerciseDown,
            topBarState = topBarState,
        ) }
    ) {
        when(val uiState = exerciseListUiState){
            NewExerciseListUiState.Initial -> {
                SpinnerScreen(msg = "Loading", paddingValues = it)
            }
            is NewExerciseListUiState.Success -> {
                when(saveUiState){
                    SaveExerciseListUiState.Back -> {
                        LaunchedEffect(saveUiState){
                            onBack()
                        }
                    }
                    SaveExerciseListUiState.Confirmation -> {
                        ConfirmationDialog(title = "Confirm Leave", text = "Finish configuring plan ${planName}? You will not be able to edit or delete exercise in this plan after finish.",
                        onConfirm = viewModel::confirmSave,
                        onCancel = viewModel::dismissSave
                        )
                    }
                    SaveExerciseListUiState.Initial -> {
                        Box(modifier = Modifier
                            .padding(it)
                            .fillMaxSize()){
//                    Column(Modifier.padding(it).align(Alignment.TopStart)) {
                            Column(
                                Modifier
                                    .padding(it)
                                    .align(Alignment.TopStart)) {
                                ExerciseListSelectable(selectedList = selectedList, exerciseList = uiState.exerciseList, onItemClick = viewModel::onItemClick)
                            }
                            SaveBtn(onSave = viewModel::promptSave, modifier = Modifier.align(Alignment.BottomCenter))

                        }
                    }
                }

            }
        }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExerciseListSelectable(
    selectedList: List<Int>,
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
                    secondaryText = { Text("set: "+item.set.toString()+" rep: "+item.rep.toString()) },
                    trailing = {
                        Checkbox(
                            checked = selectedList.contains(item.exerciseId),
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                    },
                    modifier = Modifier.clickable {
                        onItemClick(item.exerciseId)
                    }
                )
                Divider()
            }
        }
    }
}
