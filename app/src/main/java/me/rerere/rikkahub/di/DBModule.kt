package me.rerere.rikkahub.di

import androidx.room.Room
import me.rerere.rikkahub.data.db.AppDatabase
import org.koin.dsl.module

val dbModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "rikka_hub")
            .fallbackToDestructiveMigration()
            .build()
    }
}