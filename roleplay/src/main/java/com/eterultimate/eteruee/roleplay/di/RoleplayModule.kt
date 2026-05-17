package com.eterultimate.eteruee.roleplay.di

import androidx.room.Room
import com.eterultimate.eteruee.roleplay.data.local.RolePlayDatabase
import com.eterultimate.eteruee.roleplay.data.local.RolePlayFileStorage
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
import com.eterultimate.eteruee.roleplay.domain.service.CharacterServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.ChatService
import com.eterultimate.eteruee.roleplay.domain.service.ChatServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.GroupService
import com.eterultimate.eteruee.roleplay.domain.service.GroupServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.TokenService
import com.eterultimate.eteruee.roleplay.domain.service.TokenServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.BookmarkService
import com.eterultimate.eteruee.roleplay.domain.service.BookmarkServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.BackupService
import com.eterultimate.eteruee.roleplay.domain.service.BackupServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.PromptBuilderService
import com.eterultimate.eteruee.roleplay.domain.service.PromptBuilderServiceImpl
import com.eterultimate.eteruee.roleplay.ui.viewmodel.ChatViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * RolePlay 模块的 Koin 依赖注入配置
 */
val roleplayModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            RolePlayDatabase::class.java,
            RolePlayDatabase.DATABASE_NAME
        ).build()
    }
    
    // DAOs
    single { get<RolePlayDatabase>().characterDao() }
    single { get<RolePlayDatabase>().chatDao() }
    single { get<RolePlayDatabase>().worldInfoDao() }
    single { get<RolePlayDatabase>().groupDao() }
    single { get<RolePlayDatabase>().bookmarkDao() }
    
    // File Storage
    single { RolePlayFileStorage(androidContext()) }
    
    // Services
    single<CharacterService> { CharacterServiceImpl(androidContext(), get(), get()) }
    single<ChatService> { ChatServiceImpl(androidContext(), get(), get()) }
    single<WorldInfoService> { WorldInfoServiceImpl(androidContext(), get(), get()) }
    single<GroupService> { GroupServiceImpl(androidContext(), get(), get(), get()) }
    single<TokenService> { TokenServiceImpl() }
    single<BookmarkService> { BookmarkServiceImpl(get()) }
    single<BackupService> { BackupServiceImpl(get()) }
    single<PromptBuilderService> { PromptBuilderServiceImpl() }
    
    // ViewModels
    viewModel { CharacterListViewModel(get()) }
    viewModel { CharacterEditViewModel(get()) }
    viewModel { ChatViewModel(get(), get(), get()) }
    viewModel { WorldInfoListViewModel(get()) }
    viewModel { WorldInfoEditViewModel(get()) }
    viewModel { GroupListViewModel(get()) }
    viewModel { GroupEditViewModel(get()) }
}
