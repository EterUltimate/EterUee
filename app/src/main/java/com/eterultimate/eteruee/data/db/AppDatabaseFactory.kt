package com.eterultimate.eteruee.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eterultimate.eteruee.data.db.fts.MessageFtsSchema
import com.eterultimate.eteruee.data.db.migrations.Migration_11_12
import com.eterultimate.eteruee.data.db.migrations.Migration_13_14
import com.eterultimate.eteruee.data.db.migrations.Migration_14_15
import com.eterultimate.eteruee.data.db.migrations.Migration_15_16
import com.eterultimate.eteruee.data.db.migrations.Migration_6_7
import com.eterultimate.eteruee.data.db.migrations.MIGRATION_17_18

/** Shared schema, migrations and driver for the app and staged backup validation. */
internal object AppDatabaseFactory {
    fun create(context: Context, name: String = SQLiteConfiguration.DATABASE_NAME): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(Migration_6_7, Migration_11_12, Migration_13_14, Migration_14_15, Migration_15_16, MIGRATION_17_18)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    MessageFtsSchema.ensure(db)
                }

                override fun onOpen(connection: SQLiteConnection) {
                    MessageFtsSchema.ensure(connection)
                }
            })
            .build()
}
