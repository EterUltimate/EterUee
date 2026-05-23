package com.eterultimate.eteruee.roleplay.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.ui.pages.character.CharacterListPage
import com.eterultimate.eteruee.roleplay.ui.pages.worldinfo.WorldInfoListPage
import com.eterultimate.eteruee.roleplay.ui.pages.group.GroupListPage
import com.eterultimate.eteruee.roleplay.ui.pages.preset.PresetListPage
import org.koin.androidx.compose.koinViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterListViewModel

/**
 * RolePlay 主页面 - 带底部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePlayMainPage(
    onCharacterClick: (Character) -> Unit,
    onCreateCharacter: () -> Unit,
    onWorldInfoClick: (WorldInfo) -> Unit,
    onCreateWorldInfo: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    onCreatePreset: () -> Unit,
    onGroupChatClick: (Group) -> Unit,
    onCreateGroup: () -> Unit,
    viewModel: CharacterListViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "角色") },
                    label = { Text("角色") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "世界书") },
                    label = { Text("世界书") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "预设") },
                    label = { Text("预设") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "群组") },
                    label = { Text("群组") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> CharacterListPage(
                    onCharacterClick = onCharacterClick,
                    onCreateCharacter = onCreateCharacter,
                    viewModel = viewModel
                )
                1 -> WorldInfoListPage(
                    onWorldInfoClick = onWorldInfoClick,
                    onCreateWorldInfo = onCreateWorldInfo
                )
                2 -> PresetListPage(
                    onPresetClick = onPresetClick,
                    onCreatePreset = onCreatePreset
                )
                3 -> GroupListPage(
                    onGroupClick = onGroupChatClick,
                    onCreateGroup = onCreateGroup
                )
            }
        }
    }
}
