package com.urkinje.dualbudlistner.data.home.di

import com.urkinje.dualbudlistner.data.home.HomeItemsRepositoryImpl
import com.urkinje.dualbudlistner.domain.home.HomeItemsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HomeItemsModule {
    @Singleton
    @Binds
    abstract fun bindsHomeItemsRepository(
        homeItemsRepository: HomeItemsRepositoryImpl,
    ): HomeItemsRepository
}
