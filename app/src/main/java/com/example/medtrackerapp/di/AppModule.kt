package com.example.medtrackerapp.di

import android.content.Context
import androidx.room.Room
import com.example.medtrackerapp.data.local.database.CategoryDao
import com.example.medtrackerapp.data.local.database.ExamDao
import com.example.medtrackerapp.data.local.database.MedTrackerDatabase
import com.example.medtrackerapp.data.local.database.ParameterDao
import com.example.medtrackerapp.data.repository.MedicalExamRepositoryImpl
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo principal de la aplicación para Hilt
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Proporciona el contexto de la aplicación
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    /**
     * Proporciona la base de datos
     */
    @Provides
    @Singleton
    fun provideMedTrackerDatabase(@ApplicationContext context: Context): MedTrackerDatabase {
        return Room.databaseBuilder(
            context,
            MedTrackerDatabase::class.java,
            "medtracker_db"
        ).build()
    }

    /**
     * Proporciona el DAO de exámenes
     */
    @Provides
    @Singleton
    fun provideExamDao(database: MedTrackerDatabase): ExamDao {
        return database.examDao()
    }

    /**
     * Proporciona el DAO de categorías
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: MedTrackerDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Proporciona el DAO de parámetros
     */
    @Provides
    @Singleton
    fun provideParameterDao(database: MedTrackerDatabase): ParameterDao {
        return database.parameterDao()
    }

    /**
     * Proporciona el repositorio de exámenes médicos
     */
    @Provides
    @Singleton
    fun provideMedicalExamRepository(
        impl: MedicalExamRepositoryImpl
    ): MedicalExamRepository {
        return impl
    }
} 