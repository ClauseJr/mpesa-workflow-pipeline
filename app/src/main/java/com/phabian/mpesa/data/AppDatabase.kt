
package com.phabian.mpesa.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {

    @TypeConverter
    fun fromType(value: TransactionType): String = value.name

    @TypeConverter
    fun toType(value: String): TransactionType =
        TransactionType.valueOf(value)

    @TypeConverter
    fun fromStatus(value: ParseStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ParseStatus =
        ParseStatus.valueOf(value)
}

@Database(
    entities = [
        TransactionEntity::class,
        RawSmsEntity::class
    ],
    version = 2,
    exportSchema = true
)

@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun rawSmsDao(): RawSmsDao

    companion object {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "UPDATE transactions SET amount = amount * 100"
            )
            database.execSQL(
                "UPDATE transactions SET balanceAfter = balanceAfter * 100 WHERE balanceAfter IS NOT NULL"
            )
            database.execSQL(
                "UPDATE transactions SET transactionCost = transactionCost * 100 WHERE transactionCost IS NOT NULL"
            )
        }
    }
}
}

