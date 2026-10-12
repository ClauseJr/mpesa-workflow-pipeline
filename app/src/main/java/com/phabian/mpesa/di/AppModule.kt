
package com.phabian.mpesa.di

import android.content.Context
import androidx.room.Room
import com.phabian.mpesa.data.AppDatabase
import com.phabian.mpesa.data.RawSmsDao
import com.phabian.mpesa.data.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mpesa_analytics.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideTransactionDao(
        db: AppDatabase
    ): TransactionDao =
        db.transactionDao()

    @Provides
    fun provideRawSmsDao(
        db: AppDatabase
    ): RawSmsDao =
        db.rawSmsDao()
}