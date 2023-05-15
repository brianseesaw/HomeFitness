package com.example.homefitness.ui.screen.exercise.custom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import com.example.homefitness.data.ExerciseType
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.ErrorDialog
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import org.koin.androidx.compose.getViewModel

object EditExerciseDestination : NavigationDestination {
    override val route = "edit_exercise"
//    override val titleRes = "Home"
    const val exerciseIdArg = "exerciseId"
    const val planIdArg = "planId"
    val routeWithArgs = "$route?$exerciseIdArg={$exerciseIdArg}&$planIdArg={$planIdArg}"
}

@Composable
fun EditExerciseScreen(
    viewModel: EditExerciseViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
){
    val name by viewModel.name.collectAsState()
    val set by viewModel.set.collectAsState()
    val rep by viewModel.rep.collectAsState()
    val type by viewModel.type.collectAsState()
    val calorie by viewModel.calorie.collectAsState()
//    val uiState by viewModel.uiState.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val editExerciseUiState by viewModel.editExerciseUiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Exercise",
                onBack = onBack,
                topBarState = TopBarState.NONE)
        }
    ) {
        when(editExerciseUiState){
            EditExerciseUiState.Error -> {
                ErrorDialog("Invalid Input",onDismiss = viewModel::dismissError)
            }
            EditExerciseUiState.Initial -> {
                Text(viewModel.exerciseIdArg.toString())
                SpinnerScreen(msg = "Loading Exercise", paddingValues = it)
            }
            EditExerciseUiState.Input -> {
                Box(
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ){
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExerciseInput(
                            name = name,
                            set = set,
                            rep = rep,
                            type = type,
                            calorie = calorie,
                            expanded = expanded,
                            onNameChange = viewModel::setName,
                            onSetChange = viewModel::setSet,
                            onRepChange = viewModel::setRep,
                            onCalorieChange = viewModel::setCalorie,
                            onChangeType = viewModel::changeType,
                            onSetExpanded = viewModel::setExpanded
                        )
                        SaveCancelBtn(viewModel::saveExercise,onBack)
                    }
                }
            }
            EditExerciseUiState.Saved -> {
                LaunchedEffect(editExerciseUiState){
                    onSave()
                    viewModel.setEditExerciseUiState(EditExerciseUiState.Input)
                }
            }
            EditExerciseUiState.Saving ->{
                SpinnerScreen(msg = "Saving Changes", paddingValues = it)
            }
        }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExerciseInput(
    name : String,
    set: String,
    rep: String,
    calorie: String,
    type: ExerciseType,
    expanded: Boolean, //dropdown expanded state
    onNameChange: (String) -> Unit = {},
    onSetChange: (String) -> Unit = {},
    onRepChange: (String) -> Unit = {},
    onCalorieChange: (String) -> Unit = {},
    onChangeType: () -> Unit = {},
    onSetExpanded: (Boolean) -> Unit = {},
){

    val exerciseList = listOf("pushup","lunge","squat","plank")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onSetExpanded
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Exercise Name") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
        )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    onSetExpanded(false)
                }
            ) {
                exerciseList.forEach { selectionOption ->
                    DropdownMenuItem(
                        onClick = {
                            onNameChange(selectionOption)
                            onSetExpanded(false)
                        }
                    ) {
                        Text(text = selectionOption)
                    }
                }
            }

    }

    NumberTextField(set,onSetChange,"set",true)
    NumberTextField(rep,onRepChange,if(type==ExerciseType.REP) "rep" else "sec",true)
    NumberTextField(calorie,onCalorieChange,"calorie per rep",true)
    Row(verticalAlignment = Alignment.CenterVertically){
        Text(if(type==ExerciseType.REP) "Rep-based" else "Time-based")
        IconButton(onClick = onChangeType ) {
            Icon(Icons.Filled.ChangeCircle, contentDescription = "Change type")
        }
    }

}

@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit = {},
    label: String,
    enabled: Boolean,
){
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(label) },
//        shape = MaterialTheme.shapes.extraSmall,
//        colors = TextFieldDefaults.outlinedTextFieldColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer
//        ),
//        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true
    )
}

@Composable
fun LimitedNumberTextField(
    max: Int,
    value: String,
    onValueChange: (String) -> Unit = {},
    label: String,
    enabled: Boolean,
){
    OutlinedTextField(
        value = value,
        onValueChange = {
            if((it.toIntOrNull() ?: 0) <= max && it.isDigitsOnly())    {
                onValueChange(it)
            }
                        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(label) },
//        shape = MaterialTheme.shapes.extraSmall,
//        colors = TextFieldDefaults.outlinedTextFieldColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer
//        ),
//        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true
    )
}

@Composable
fun SaveCancelBtn(
//    modifier: Modifier,
    onSave: () -> Unit = {},
    onBack: () -> Unit = {},
){
    Row(verticalAlignment = Alignment.CenterVertically){
        IconButton(onClick = onSave ) {
            Icon(Icons.Filled.Done, contentDescription = "Save")
        }
        IconButton(onClick = onBack ) {
            Icon(Icons.Filled.Close, contentDescription = "Back")
        }
    }
}

@Composable
fun SaveBtn(
    modifier: Modifier,
    onSave: () -> Unit = {},
){
    IconButton(onClick = onSave, modifier = modifier) {
        Icon(Icons.Outlined.Done, contentDescription = "Save")
    }
}

