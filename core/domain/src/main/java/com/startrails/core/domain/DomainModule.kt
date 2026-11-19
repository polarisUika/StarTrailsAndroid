package com.startrails.core.domain

import com.startrails.core.imaging.KotlinStackingEngine
import com.startrails.core.imaging.StackingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideStackingEngine(): StackingEngine = KotlinStackingEngine()
}
