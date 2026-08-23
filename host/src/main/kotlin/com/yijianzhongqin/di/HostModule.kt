package com.yijianzhongqin.di

import android.content.Context
import androidx.room.Room
import com.yijianzhongqin.App
import com.yijianzhongqin.shell.PluginManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HostModule {

    @Provides
    @Singleton
    fun providePluginManager(app: App): PluginManager = app.pluginManager
}
