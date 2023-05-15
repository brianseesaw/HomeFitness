package com.example.homefitness.ui.screen.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import org.koin.androidx.compose.getViewModel

object HistoryPlanListDestination : NavigationDestination {
    override val route = "history_plan_list"
    //    override val titleRes = "Home"
    const val planListTypeArg = "planListType"
//    val routeWithArgs = "$route/{$planListTypeArg}"
}

@Composable
fun HistoryPlanListScreen(
    viewModel: HistoryPlanListViewModel = getViewModel(),
    onBack: () -> Unit = {},
    onItemClick: (Int,Int) -> Unit,
){
    val uiState by viewModel.historyPlanListUiState.collectAsState()

    Scaffold(
        topBar = { TopBar(
            title = "History",
            onBack = onBack,
            topBarState = TopBarState.NONE,
        ) }
    ) {
        when(val result = uiState){
            HistoryPlanListUiState.Initial -> {
                SpinnerScreen("Loading",it)
            }
            is HistoryPlanListUiState.Success -> {
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