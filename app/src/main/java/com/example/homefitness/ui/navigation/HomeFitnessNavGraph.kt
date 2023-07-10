/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.homefitness.ui.navigation

import android.content.pm.ActivityInfo
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.homefitness.ui.screen.browse.BrowseExListDestination
import com.example.homefitness.ui.screen.browse.BrowseExerciseListScreen
import com.example.homefitness.ui.screen.browse.BrowsePlanListDestination
import com.example.homefitness.ui.screen.browse.BrowsePlanListScreen
import com.example.homefitness.ui.screen.statistics.StatDestination
import com.example.homefitness.ui.screen.statistics.StatScreen
import com.example.homefitness.ui.screen.exercise.*
import com.example.homefitness.ui.screen.exercise.custom.EditExerciseDestination
import com.example.homefitness.ui.screen.exercise.custom.EditExerciseScreen
import com.example.homefitness.ui.screen.exercise.custom.NewExerciseListDestination
import com.example.homefitness.ui.screen.exercise.custom.NewExerciseListScreen
import com.example.homefitness.ui.screen.exercise.run.*
import com.example.homefitness.ui.screen.home.HomeDestination
import com.example.homefitness.ui.screen.home.HomeScreen
import com.example.homefitness.ui.screen.plan.*

/**
 * Provides Navigation graph for the application.
 */
@ExperimentalGetImage
@Composable
fun NavigationAppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
        modifier = modifier
    ) {
        composable(route = HomeDestination.route) {
            HomeScreen(
                navController = navController,
                onBotNavClick = {navController.navigateBottom(it)},
                onMyPlanClick = {navController.navigate(MyPlanListDestination.route)},
                onResumePlanClick = {navController.navigate(ResumePlanListDestination.route)},
                onHistoryClick = {navController.navigate(HistoryPlanListDestination.route)}
            )
        }
        composable(route = StatDestination.route) {
            StatScreen(
                navController = navController,
                onBotNavClick = {navController.navigateBottom(it)}
            )
        }

        //browse
        composable(route = BrowsePlanListDestination.route) {
            BrowsePlanListScreen(
                navController = navController,
                onBotNavClick = {navController.navigateBottom(it)},
                onItemClick = { navController.navigate("${BrowseExListDestination.route}/$it") },
            )
        }
        composable(
            route = BrowseExListDestination.routeWithArgs,
            arguments = listOf(navArgument(BrowseExListDestination.planIdArg) {
                type = NavType.IntType
            })
        ){
            BrowseExerciseListScreen(
                onBack = { navController.popBackStack() },
            )
        }
        //browse

        composable(route = MyPlanListDestination.route){
            MyPlanListScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(NewPlanDestination.route) },
                onItemClick = { navController.navigate("${ExerciseListDestination.route}/$it") },
            )
        }

        // custom plan
        composable(route = NewPlanDestination.route){
            NewPlanScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.navigate("${NewExerciseListDestination.route}/$it") },
            )
        }
        composable(
            route = NewExerciseListDestination.routeWithArgs,
            arguments = listOf(navArgument(NewExerciseListDestination.planIdArg) {
                type = NavType.IntType
            })
        ){
            NewExerciseListScreen(
                onBack = { navController.popBackStack(MyPlanListDestination.route,false) },
                onAdd = { navController.navigate("${EditExerciseDestination.route}?${EditExerciseDestination.planIdArg}=$it") },
                onEdit = { navController.navigate("${EditExerciseDestination.route}?${EditExerciseDestination.exerciseIdArg}=$it") },
            )
        }
        composable(
            route = EditExerciseDestination.routeWithArgs,
            arguments = listOf(
                navArgument(EditExerciseDestination.exerciseIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                },
                navArgument(EditExerciseDestination.planIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                }
            )
        ){
            EditExerciseScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        // custom plan

        //resume plan
        composable(route = ResumePlanListDestination.route){
            ResumePlanListScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { runId,planId-> navController.navigate("${ResumeExListDestination.route}/$runId/$planId")},
            )
        }
        composable(
            route = ResumeExListDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ResumeExListDestination.planIdArg) {
                    type = NavType.IntType
                },
                navArgument(ResumeExListDestination.runIdArg) {
                    type = NavType.IntType
                })
        ){
            ResumeExerciseListScreen(
                onBack = { navController.popBackStack() },
                onRun = { id,screen->
                    navController.navigateExercise(id, screen)
                },
            )
        }
        //resume plan

        //history plan
        composable(route = HistoryPlanListDestination.route){
            HistoryPlanListScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { runId,planId-> navController.navigate("${HisExListDestination.route}/$runId/$planId")},
            )
        }
        composable(
            route = HisExListDestination.routeWithArgs,
            arguments = listOf(
                navArgument(HisExListDestination.planIdArg) {
                    type = NavType.IntType
                },
                navArgument(HisExListDestination.runIdArg) {
                    type = NavType.IntType
                })
        ){
            HistoryExerciseListScreen(
                onBack = { navController.popBackStack() },
            )
        }
        //history plan

        composable(
            route = ExerciseListDestination.routeWithArgs,
            arguments = listOf(navArgument(ExerciseListDestination.planIdArg) {
                type = NavType.IntType
            })
        ){
            ExerciseListScreen(
                onBack = { navController.popBackStack() },
                onRun = { id,screen->
                    navController.navigateExercise(id, screen)
                        },
            )
        }

        //exercise composable
        composable(
            route = RepExDestination.routeWithArgs,
            arguments = listOf(
                navArgument(RepExDestination.runIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                },
            )
        ){
            RepExScreen(
                onBack = { navController.popBackStack() },
                onBackToList = { navController.navigateBackFromExercise() },
                onNextExercise = { id,screen->
                    navController.navigateExercise(id, screen)
                }
            )
        }
        composable(
            route = TimeExDestination.routeWithArgs,
            arguments = listOf(
                navArgument(TimeExDestination.runIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                })
        ){
            TimeExScreen(
                onBack = { navController.popBackStack() },
                onBackToList = { navController.navigateBackFromExercise() },
                onNextExercise = { id,screen->
                    navController.navigateExercise(id, screen)
                }
            )
        }
        composable(
            route = RepExCamDestination.routeWithArgs,
            arguments = listOf(
                navArgument(RepExCamDestination.runIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                },
                navArgument(RepExCamDestination.orenArg) {
                    defaultValue = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    type = NavType.IntType
                })
        ){
            RepExCamScreen(
                onBack = { navController.popBackStack() },
                onBackToList = {
                    navController.navigateBackFromExercise()
                },
                onNextExercise = { id,screen->
                    navController.navigateExercise(id, screen)
                },
                orientation = it.arguments?.getInt(RepExCamDestination.orenArg) ?:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            )
        }
        composable(
            route = TimeExCamDestination.routeWithArgs,
            arguments = listOf(
                navArgument(TimeExCamDestination.runIdArg) {
                    defaultValue = 0
                    type = NavType.IntType
                },
                navArgument(TimeExCamDestination.orenArg) {
                    defaultValue = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    type = NavType.IntType
                },
            )
        ){
            TimeExCamScreen(
                onBack = { navController.popBackStack() },
                onBackToList = {
                    navController.navigateBackFromExercise()
                },
                onNextExercise = { id,screen->
                    navController.navigateExercise(id, screen)
                },
                orientation = it.arguments?.getInt(TimeExCamDestination.orenArg) ?:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            )
        }
    }


}

fun NavHostController.navigateBottom(route : String){
    this@navigateBottom.navigate(route){
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(this@navigateBottom.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

fun NavHostController.navigateExercise(runId: Int, screen: Int){
    var route = ""
    when (screen){
        1 -> {
            route = "${RepExDestination.route}?${RepExDestination.runIdArg}=$runId"
        }
        2 -> {
            route = "${TimeExDestination.route}?${TimeExDestination.runIdArg}=$runId"
        }
        3 -> {
            route = "${RepExCamDestination.route}?${RepExCamDestination.runIdArg}=$runId"
        }
        4 -> {
            route = "${RepExCamDestination.route}?${RepExCamDestination.runIdArg}=$runId&${RepExCamDestination.orenArg}=${ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}"
        }
        5 -> {
            route = "${TimeExCamDestination.route}?${TimeExCamDestination.runIdArg}=$runId"
        }
        6 -> {
            route = "${TimeExCamDestination.route}?${TimeExCamDestination.runIdArg}=$runId&${TimeExCamDestination.orenArg}=${ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}"
        }
    }
    val navExList = this.popBackStack(
        route = MyPlanListDestination.route,
        inclusive = false,
        saveState = false
    )
    if(!navExList) {
        this.popBackStack(
            route = ResumePlanListDestination.route,
            inclusive = false,
            saveState = false
        )
    }
    this.navigate(route){
//        popUpTo(ExerciseListDestination.routeWithArgs){
//            inclusive = false
//            saveState = false
//        }
//        launchSingleTop = true
    }

}

fun NavHostController.navigateBackFromExercise(){
    val navExList = this.popBackStack(
        route = MyPlanListDestination.route,
        inclusive = false,
        saveState = true
    )
    if(!navExList) {
        this.popBackStack(
            route = ResumePlanListDestination.route,
            inclusive = false,
            saveState = true
        )
    }
}