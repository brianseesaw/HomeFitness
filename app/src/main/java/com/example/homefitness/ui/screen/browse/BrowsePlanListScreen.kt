package com.example.homefitness.ui.screen.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.BottomNavBar
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.*
import org.koin.androidx.compose.getViewModel

object BrowsePlanListDestination : NavigationDestination {
    override val route = "browse_plan_list"
    //    override val titleRes = "Home"
}

@Composable
fun BrowsePlanListScreen(
    viewModel: BrowsePlanListViewModel = getViewModel(),
    navController: NavController,
    onBotNavClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onItemClick: (Int) -> Unit = {},
){
    val myPlanListUiState by viewModel.browsePlanListUiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            title = { Text("Browse") },
            ) },
        bottomBar = {
            BottomNavBar(navController,onBotNavClick)
        }
    ) {
        when(val result = myPlanListUiState){
            BrowsePlanListUiState.Initial -> {
                SpinnerScreen("Loading",it)
            }
            is BrowsePlanListUiState.Success -> {

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