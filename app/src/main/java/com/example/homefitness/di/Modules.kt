package com.example.homefitness.di

import androidx.room.Room
import com.example.homefitness.data.ExerciseRepositoryImpl
import com.example.homefitness.data.ExerciseRunRepositoryImpl
import com.example.homefitness.data.FitnessDatabase
import com.example.homefitness.data.PlanRepositoryImpl
import com.example.homefitness.ui.screen.browse.BrowsePlanListViewModel
import com.example.homefitness.ui.screen.exercise.custom.EditExerciseViewModel
import com.example.homefitness.ui.screen.exercise.ExerciseListViewModel
import com.example.homefitness.ui.screen.exercise.ExerciseViewModel
import com.example.homefitness.util.AlertPlayer
import com.example.homefitness.classifier.PoseClassifier
import com.example.homefitness.classifier.SoundClassifierProvider
import com.example.homefitness.ui.screen.browse.BrowseExerciseListViewModel
import com.example.homefitness.ui.screen.exercise.HistoryExerciseListViewModel
import com.example.homefitness.ui.screen.exercise.ResumeExerciseListViewModel
import com.example.homefitness.ui.screen.exercise.custom.NewExerciseListViewModel
import com.example.homefitness.ui.screen.exercise.run.RepExCamViewModel
import com.example.homefitness.ui.screen.exercise.run.RepExViewModel
import com.example.homefitness.ui.screen.exercise.run.TimeExCamViewModel
import com.example.homefitness.ui.screen.exercise.run.TimeExViewModel
import com.example.homefitness.ui.screen.plan.*
import com.example.homefitness.ui.screen.statistics.StatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object Modules {
    val databaseModule = module {
        single {
            Room.databaseBuilder(
                androidContext(),
                FitnessDatabase::class.java,
                "fitness"
            ).createFromAsset("database/my_fitness.db").build()
        }
    }

    val daosModule = module {
        includes(databaseModule)

        single { get<FitnessDatabase>().exerciseDao() }
        single { get<FitnessDatabase>().planDao() }
        single { get<FitnessDatabase>().exerciseRunDao() }
    }

    val repositoryModule = module {
        single {
            ExerciseRepositoryImpl(get())
        }
        single {
            PlanRepositoryImpl(get())
        }
        single {
            ExerciseRunRepositoryImpl(get())
        }
    }

    val alertPlayer = module {
        single { AlertPlayer(get()) }
    }

    val classifier = module {
        single { PoseClassifier(get()) }
        single { SoundClassifierProvider(get()) }
    }

    val viewModels = module {
        viewModel { MyPlanListViewModel(get(),get()) }
        viewModel { ExerciseListViewModel(get(),get(),get(),get()) }

        viewModel { NewPlanViewModel(get(),get()) }
        viewModel { NewExerciseListViewModel(get(),get(),get(),get()) }
        viewModel { EditExerciseViewModel(get(),get()) }

        viewModel { BrowsePlanListViewModel(get(),get()) }
        viewModel { BrowseExerciseListViewModel(get(),get(),get()) }

        viewModel { ResumePlanListViewModel(get(),get(),get()) }
        viewModel { ResumeExerciseListViewModel(get(),get(),get(),get()) }

        viewModel { HistoryPlanListViewModel(get(),get(),get()) }
        viewModel { HistoryExerciseListViewModel(get(),get(),get(),get()) }

        viewModel { ExerciseViewModel(get(),get(),get(),get(),get(),get(),get()) }
        viewModel { RepExViewModel(get(),get(),get(),get())}
        viewModel { TimeExViewModel(get(),get(),get(),get(),get()) }
        viewModel { RepExCamViewModel(get(),get(),get(),get(),get(),get(),get()) }
        viewModel { TimeExCamViewModel(get(),get(),get(),get(),get(),get()) }

        viewModel { StatViewModel(get(),get(),get())}
    }
}