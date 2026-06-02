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
import com.eterultimate.eteruee.roleplay.domain.service.GroupSpeakerService
import com.eterultimate.eteruee.roleplay.domain.service.GroupSpeakerServiceImpl
import com.eterultimate.eteruee.roleplay.domain.service.PresetService
import com.eterultimate.eteruee.roleplay.domain.service.PresetServiceImpl
import com.eterultimate.eteruee.roleplay.domain.subagent.RoleplaySubagentExecutor
import com.eterultimate.eteruee.roleplay.domain.extension.ExtensionManager
import com.eterultimate.eteruee.roleplay.domain.extension.ExtensionManagerImpl
import com.eterultimate.eteruee.roleplay.ui.viewmodel.ChatViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.PresetEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.PresetListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoListViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoTestViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
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
        )
        .addMigrations(
            RolePlayDatabase.MIGRATION_1_2,
            RolePlayDatabase.MIGRATION_2_3,
            RolePlayDatabase.MIGRATION_3_4,
            RolePlayDatabase.MIGRATION_4_5,
            RolePlayDatabase.MIGRATION_5_6
        )
        .build()
    }
    
    // DAOs
    single { get<RolePlayDatabase>().characterDao() }
    single { get<RolePlayDatabase>().chatDao() }
    single { get<RolePlayDatabase>().worldInfoDao() }
    single { get<RolePlayDatabase>().groupDao() }
    single { get<RolePlayDatabase>().bookmarkDao() }
    single { get<RolePlayDatabase>().presetDao() }
    
    // File Storage
    single { RolePlayFileStorage(androidContext()) }
    
    // Services
    single<CharacterService> { CharacterServiceImpl(androidContext(), get(), get()) }
    single<ChatService> { ChatServiceImpl(androidContext(), get(), get(), get()) }
    single<WorldInfoService> { WorldInfoServiceImpl(androidContext(), get(), get()) }
    single<GroupService> { GroupServiceImpl(androidContext(), get(), get(), get()) }
    single<TokenService> { TokenServiceImpl() }
    single<BookmarkService> { BookmarkServiceImpl(get()) }
    single<BackupService> { BackupServiceImpl(get()) }
    single<PromptBuilderService> { PromptBuilderServiceImpl() }
    single<GroupSpeakerService> { GroupSpeakerServiceImpl() }
    single<ExtensionManager> { ExtensionManagerImpl() }
    single<PresetService> { PresetServiceImpl(androidContext(), get()) }
    single { RoleplaySubagentExecutor() }
    
    // ViewModels
    viewModel { CharacterListViewModel(get()) }
    viewModel { CharacterEditViewModel(get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get()) }
    viewModel { WorldInfoListViewModel(get()) }
    viewModel { WorldInfoEditViewModel(get()) }
    viewModel { GroupListViewModel(get()) }
    viewModel { GroupEditViewModel(get()) }
    viewModel { WorldInfoTestViewModel(get()) }
    viewModel { PresetListViewModel(get()) }
    viewModel { PresetEditViewModel(get()) }
}
