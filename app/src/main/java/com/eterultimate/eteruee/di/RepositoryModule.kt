package com.eterultimate.eteruee.di

import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.files.SkillManager
import com.eterultimate.eteruee.data.repository.ConversationRepository
import com.eterultimate.eteruee.data.repository.FavoriteRepository
import com.eterultimate.eteruee.data.repository.FilesRepository
import com.eterultimate.eteruee.data.repository.GenMediaRepository
import com.eterultimate.eteruee.data.repository.MemoryRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        ConversationRepository(get(), get(), get(), get(), get(), get())
    }

    single {
        MemoryRepository(get())
    }

    single {
        GenMediaRepository(get())
    }

    single {
        FilesRepository(get())
    }

    single {
        FavoriteRepository(get())
    }

    single {
        FilesManager(get(), get(), get())
    }

    single {
        SkillManager(get(), get())
    }
}

