package com.example.homefitness.ui.screen.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.homefitness.ui.navigation.NavigationDestination
import com.example.homefitness.ui.screen.BottomNavBar
import com.example.homefitness.ui.screen.TopBar
import com.example.homefitness.ui.screen.TopBarState
import com.example.homefitness.ui.screen.plan.SpinnerScreen
import com.example.homefitness.ui.theme.HomeFitnessTheme
import org.koin.androidx.compose.getViewModel

object StatDestination : NavigationDestination {
    override val route = "stat"
//    override val titleRes = "Home"
}

@Composable
fun StatScreen(
    viewModel: StatViewModel = getViewModel(),
    navController: NavController,
    onBotNavClick: (String) -> Unit = {}
){

    val statUiState by viewModel.statUiState.collectAsState()
    Scaffold(
        topBar = { TopAppBar(
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            title = { Text("Statistics") },
        ) },
        bottomBar = { BottomNavBar(navController,onBotNavClick) }
    ) {
        when(val res = statUiState){
            StatUiState.Initial -> {
                SpinnerScreen(msg = "Loading data", paddingValues = it)
            }
            is StatUiState.Success -> {
                Box(modifier = Modifier.padding(it)){
                    StatContent(statUiState = res.data)
                }
            }
        }

    }
}

@Composable
fun StatContent(
    statUiState: StatData
){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp)){
            Text("Today")
        }
        Spacer(modifier = Modifier.size(10.dp))

        StatText(num = "${statUiState.dayStat.planCount}", lbl = " plan")
        StatText(num = "${statUiState.dayStat.exCount}", lbl = " exercise")
        StatText(num = String.format("%.2f", statUiState.dayStat.calSum), lbl = " calorie")

        Spacer(modifier = Modifier.size(30.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp)){
        Text("Last 7 days")
        }

        Spacer(modifier = Modifier.size(10.dp))

        StatText(num = "${statUiState.weekStat.planCount}", lbl = " plan")
        StatText(num = "${statUiState.weekStat.exCount}", lbl = " exercise")
        StatText(num = String.format("%.2f", statUiState.weekStat.calSum), lbl = " calorie")

        Spacer(modifier = Modifier.size(30.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp)){
            Text("Last 30 days",)
        }

        Spacer(modifier = Modifier.size(10.dp))

        StatText(num = "${statUiState.monthStat.planCount}", lbl = " plan")
        StatText(num = "${statUiState.monthStat.exCount}", lbl = " exercise")
        StatText(num = String.format("%.2f", statUiState.monthStat.calSum), lbl = " calorie")
    }
}

@Composable
fun StatText(
    num: String,
    lbl: String,
){
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 40.sp)) {
            append(num)
        }
        append(lbl)}
    )
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HomeFitnessTheme {
        Scaffold {
            Box(modifier = Modifier.padding(it)){

//                StatContent(
//                    StatUiState(
//                        1,1,100f,1,1,120f,1,1,150f
//                    )
//                )
            }
        }
    }
}