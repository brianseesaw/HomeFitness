package com.example.homefitness.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.homefitness.ui.screen.browse.BrowsePlanListDestination
import com.example.homefitness.ui.screen.statistics.StatDestination
import com.example.homefitness.ui.screen.home.HomeDestination
import com.example.homefitness.ui.theme.HomeFitnessTheme


@Composable
fun BottomNavBar(
    navController: NavController,
    onBotNavClick: (String) -> Unit = {}
){

    val bottomNavigationItems = listOf(
        BottomNavigationScreens.Browse,
        BottomNavigationScreens.Home,
        BottomNavigationScreens.Activity,
    )

    BottomNavigation (
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ){
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        bottomNavigationItems.forEachIndexed { index, item ->
            BottomNavigationItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label)},
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {onBotNavClick(item.route)}
            )
        }
    }
}

@Composable
fun TopBar(
    title : String,
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRun: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    topBarState: TopBarState,
           ){
    TopAppBar(
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if(topBarState != TopBarState.NONE){
                if(topBarState == TopBarState.ADD){
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add")
                    }
                }
                if(topBarState == TopBarState.ADD_TO_MY){
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add")
                    }
                }
                if(topBarState == TopBarState.EDIT_DELETE){
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
                if(topBarState == TopBarState.EDIT_DELETE_MOVE){
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Filled.ArrowCircleDown, contentDescription = "MoveDown")
                    }
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.ArrowCircleUp, contentDescription = "MoveUp")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
                if(topBarState == TopBarState.RUN){
                    IconButton(onClick = onRun) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = "Run")
                    }
                }
                if(topBarState == TopBarState.ADD_RUN){
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add")
                    }
                    IconButton(onClick = onRun) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = "Run")
                    }
                }
                if(topBarState == TopBarState.DELETE){
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
                if(topBarState == TopBarState.DELETE_RUN){
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = onRun) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = "Run")
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PrevTest() {
    HomeFitnessTheme() {
        Scaffold(
            topBar = {
                TopBar("Add Exercise",topBarState=TopBarState.NONE)
            },
            bottomBar = {
//            BottomNavBar({})
            }
        ) {contentPadding ->
            Box(modifier = Modifier.padding(contentPadding)){

            }
        }
    }
}

sealed class BottomNavigationScreens(
    val route: String, val icon: ImageVector, val label: String) {
    object Browse : BottomNavigationScreens(BrowsePlanListDestination.route, Icons.Filled.Search, "Browse")
    object Home : BottomNavigationScreens(HomeDestination.route, Icons.Filled.Home,"Home")
    object Activity : BottomNavigationScreens(StatDestination.route, Icons.Filled.Analytics,"Statistics")
}

enum class TopBarState{
    EMPTY, //
    ADD, // not selected allow add (my plan list)
    DELETE, // >1 plan/exercise selected allow delete (my plan/exercise list)
    EDIT_DELETE, // 1 plan selected allow edit, delete (my plan/exercise list)
    EDIT_DELETE_MOVE, // 1 plan selected allow edit, move, delete (exercise list)
    RUN, // 1 selected allow run (resume exercise list)
    ADD_RUN, // not selected allow add exercise, run plan (my exercise list)
    ADD_TO_MY, // add browse plan to my plan (browse exercise list)
    NONE, // view exercise list (resume, history, browse)
    DELETE_RUN, // delete or run plan
}

