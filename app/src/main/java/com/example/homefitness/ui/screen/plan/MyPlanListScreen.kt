package com.example.homefitness.ui.screen.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.homefitness.data.Plan
import com.example.homefitness.ui.LoadingSpinner
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.theme.HomeFitnessTheme
import org.koin.androidx.compose.getViewModel

object MyPlanListDestination : NavigationDestination {
    override val route = "my_plan_list"
    //    override val titleRes = "Home"
    const val planListTypeArg = "planListType"
//    val routeWithArgs = "$route/{$planListTypeArg}"
}

@Composable
fun MyPlanListScreen(
    viewModel: MyPlanListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onItemClick: (Int) -> Unit = {},
){
    val myPlanListUiState by viewModel.myPlanListUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = "My Plan",
            onBack = onBack,
            onAdd = onAdd,
            topBarState = TopBarState.ADD,
        ) }
    ) {
        when(val result = myPlanListUiState){
            MyPlanListUiState.Initial -> {
                SpinnerScreen("Loading",it)
            }
            is MyPlanListUiState.Success -> {

                Box(modifier = Modifier.padding(it)){
                    PlanList(
                        planList = result.planList,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlanListSelectable(
    selectedList: List<Int>,
    planList: List<Plan>,
    onItemClick:(Int) -> Unit = {},
){
    if (planList.isEmpty()) {
        Text(
            text = "No plan found",
            textAlign = TextAlign.Center
        )
    }else{
        Column() {
            Text(selectedList.joinToString())
            LazyColumn() {
                itemsIndexed(planList){index, item ->
                    ListItem(
                        text = { Text(item.name) },
                        //                secondaryText = { Text(item.)},
                        trailing = {
                            Checkbox(
                                checked = selectedList.contains(item.planId),
                                onCheckedChange = null // null recommended for accessibility with screenreaders
                            )
                        },
                        modifier = Modifier.clickable {
                            onItemClick(item.planId)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlanList(
    planList: List<Plan>,
    onItemClick:(Int) -> Unit = {},
){
    if (planList.isEmpty()) {
        Text(
            text = "No plan found",
            textAlign = TextAlign.Center
        )
    }else{
        Column() {
            LazyColumn() {
//                planList.sortedBy { it.date }.
                itemsIndexed(planList){index, item ->

                    ListItem(
                        text = { Text(item.name) },
                        trailing = {},
                        modifier = Modifier.clickable {
                            onItemClick(item.planId)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}


@Composable
fun SpinnerScreen(
    msg: String,
    paddingValues: PaddingValues
){
    Row(modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize(),
        horizontalArrangement =  Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoadingSpinner()
        Text(msg)
    }
}

