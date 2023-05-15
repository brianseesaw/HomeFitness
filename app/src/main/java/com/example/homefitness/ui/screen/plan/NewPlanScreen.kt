package com.example.homefitness.ui.screen.plan

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.exercise.custom.ExerciseInput
import com.example.homefitness.ui.screen.exercise.custom.SaveCancelBtn
import org.koin.androidx.compose.getViewModel

object NewPlanDestination : NavigationDestination {
    override val route = "new_plan"
    //    override val titleRes = "Home"
}

@Composable
fun NewPlanScreen(
    viewModel: NewPlanViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onSave: (Int) -> Unit = {},
){
    val planName by viewModel.planName.collectAsState()
    val exName by viewModel.exName.collectAsState()
    val set by viewModel.set.collectAsState()
    val rep by viewModel.rep.collectAsState()
    val type by viewModel.type.collectAsState()
    val calorie by viewModel.calorie.collectAsState()
    val newPlanListUiState by viewModel.newPlanListUiState.collectAsState()
    val expanded by viewModel.expanded.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Add New Plan",
                onBack = onBack,
                topBarState = TopBarState.NONE)
        }
    ) {

        when(val uiState: NewPlanListUiState = newPlanListUiState){
            NewPlanListUiState.Error -> {
                ErrorDialog("Invalid Input",onDismiss = viewModel::dismissError)
            }
            NewPlanListUiState.Input -> {
                Box(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ){
                    Column (
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                            ){
//                        Text(uiState.toString())
                        OutlinedTextField(
                            value = planName,
                            onValueChange = viewModel::setPlanName ,
                            label = { Text("Plan name") },
                            enabled = true,
                            singleLine = true
                        )
                        ExerciseInput(
                            name = exName,
                            set = set,
                            rep = rep,
                            calorie = calorie,
                            type = type,
                            expanded = expanded,
                            onNameChange = viewModel::setExName,
                            onSetChange = viewModel::setSet,
                            onRepChange = viewModel::setRep,
                            onCalorieChange = viewModel::setCalorie,
                            onChangeType = viewModel::changeType,
                            onSetExpanded = viewModel::setExpanded
                        )
                        SaveCancelBtn(viewModel::addPlan,onBack)
                    }
                }
            }
            is NewPlanListUiState.Saved -> {
                LaunchedEffect(newPlanListUiState){
                    Log.d("After insert NEW_PLAN",uiState.planId.toString())
                    onSave(uiState.planId)
//                    viewModel.setUiState(NewPlanListUiState.Input)
                }
            }
            NewPlanListUiState.Saving -> {
                SpinnerScreen("Saving",it)
            }
        }

    }
}

@Composable
fun ErrorDialog(
    errorMsg : String,
    onDismiss: () -> Unit = {},
){
    AlertDialog(
        onDismissRequest = onDismiss
        ,
        title = {Text(text = "Error")},
        text = {Text(errorMsg)},
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text("Dismiss")
                }
            }
        }
    )
}