package com.example.homefitness.ui.screen.plan

import android.R
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import org.koin.androidx.compose.getViewModel
import java.text.SimpleDateFormat
import java.util.Date

object ResumePlanListDestination : NavigationDestination {
    override val route = "resume_plan_list"
    //    override val titleRes = "Home"
    const val planListTypeArg = "planListType"
//    val routeWithArgs = "$route/{$planListTypeArg}"
}

@Composable
fun ResumePlanListScreen(
    viewModel: ResumePlanListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onItemClick: (Int,Int) -> Unit = { i: Int, i1: Int -> },
){
    val uiState by viewModel.resumePlanListUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = "Resume Plan",
            onBack = onBack,
            topBarState = TopBarState.NONE,
        ) }
    ) {
        when(val result = uiState){
            ResumePlanListUiState.Initial -> {
                SpinnerScreen("Loading",it)
            }
            is ResumePlanListUiState.Success -> {
                Box(modifier = Modifier.padding(it)){
                    RunPlanList(
                        headerList = result.headerList,
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
fun RunPlanList(
    headerList: List<Int>,
    planList: List<RunWithPlan>,
    onItemClick:(Int,Int) -> Unit = { i: Int, i1: Int -> },
){
    if (planList.isEmpty()) {
        Text(
            text = "No plan found",
            textAlign = TextAlign.Center
        )
    }else{
        Column() {
            LazyColumn() {
                itemsIndexed(planList){index, item ->
                    if (index in headerList){
                        ListItem(
                            text = { Text(formatDate(item.run.date))},
                            modifier = Modifier.background(Color.Gray)
                        )
                    }
                    ListItem(
                        text = { Text(item.plan.name) },
                        secondaryText = { Text(formatDateTime(item.run.date)) },
                        trailing = {},
                        modifier = Modifier.clickable {
                            onItemClick(item.run.runId,item.plan.planId)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun formatDateTime(date: Date) : String{
    val simpleDateFormat = SimpleDateFormat("d MMM yyyy HH:mm")
    return simpleDateFormat.format(date).toString()
}

@SuppressLint("SimpleDateFormat")
fun formatDate(date: Date) : String{
    val simpleDateFormat = SimpleDateFormat("EEE, d MMM yyyy")
    return simpleDateFormat.format(date).toString()
}