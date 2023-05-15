package com.example.homefitness.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.BottomNavBar

object HomeDestination : NavigationDestination {
    override val route = "home"
//    override val titleRes = "Home"
}


@Composable
fun HomeScreen(
    navController: NavController,
    onBotNavClick: (String) -> Unit = {},
    onMyPlanClick: () -> Unit = {},
    onResumePlanClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
){
    Scaffold(
        topBar = { TopAppBar(
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            title = { Text("Home") },
        ) },
        bottomBar = {
            BottomNavBar(navController,onBotNavClick)}
    ) {
        Column(modifier = Modifier.padding(it)){
            ClickableCard("My Plan",onMyPlanClick)
            ClickableCard("Resume Plan",onResumePlanClick)
            ClickableCard("History",onHistoryClick)
        }
    }
}

@Composable
fun ClickableCard(
    cardMsg: String,
    onCardClick: () -> Unit = {},
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable { onCardClick() },
        elevation = 10.dp
    ) {
        Text(modifier = Modifier.padding(15.dp),text = cardMsg)
    }
}