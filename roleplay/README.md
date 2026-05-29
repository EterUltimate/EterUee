# RolePlay Module

`roleplay` is the local roleplay feature module embedded in the EterUee Android app. It owns roleplay-specific data, domain services, Compose screens, ViewModels, Tavern-compatible import/export logic, and local persistence.

## Current Scope

Implemented or wired into the current app:

- Character list and editor.
- Chat list and chat screen.
- World info list, editor, and entries.
- Group list and editor.
- Preset list/editor path.
- Bookmark page and bookmark navigation.
- Message branching and regeneration.
- AI response generation through the shared AI layer.
- Tavern character, chat, world info, and PNG codec support.
- Local Room database and file-backed asset storage.
- Koin module integration.
- App navigation integration through `RouteActivity`.

## Module Structure

```text
roleplay/
  src/main/java/com/eterultimate/eteruee/roleplay/
    data/
      local/          Room database, DAOs, entities, file storage
      model/          Character, Chat, Group, Preset, WorldInfo, Bookmark
      serialization/  JSON helpers
      tavern/         Tavern-compatible codecs
    domain/
      extension/      Roleplay extension primitives
      service/        Business services and prompt/token helpers
      subagent/       Roleplay subagent executor glue
    di/               Koin module
    ui/
      components/     Shared roleplay UI components
      pages/          Compose pages
      viewmodel/      StateFlow-backed ViewModels
```

## App Routes

The app currently wires RolePlay screens in `app/src/main/java/com/eterultimate/eteruee/RouteActivity.kt`.

Relevant screen keys:

- `Screen.RolePlay`
- `Screen.CharacterList`
- character edit route
- roleplay chat route
- world info routes
- group routes
- preset route
- bookmark route

Keep route ownership in `app`; expose roleplay pages from this module.

## Persistence

The module uses Room for structured state and app-local files for assets/imports.

Primary local entities:

- `rp_characters`
- `rp_chats`
- `rp_world_infos`
- `rp_groups`
- `rp_bookmarks`
- `rp_presets`

File storage is rooted under the app files directory, not external shared storage by default.

## AI Boundary

RolePlay should call the shared AI layer instead of implementing provider-specific logic.

Use:

- `com.eterultimate.eteruee.ai.ui.UIMessage`
- `com.eterultimate.eteruee.ai.sdk.AISDK`
- app-level provider/model settings

Keep roleplay-specific prompt construction inside `domain/service`, especially prompt building, world info injection, character context, and group speaker selection.

## Build And Test

Run from repository root:

```bash
./gradlew :roleplay:assembleDebug
./gradlew :roleplay:testDebugUnitTest
./gradlew :app:assembleDebug
```

For broad validation:

```bash
./gradlew test
git diff --check -- roleplay
```

## Manual Smoke Test

After installing a debug build:

1. Open EterUee.
2. Enter RolePlay from the app navigation.
3. Create or import a character.
4. Open a chat for that character.
5. Send a message and confirm streaming response behavior.
6. Edit or regenerate a message and confirm branch state.
7. Add a bookmark and navigate back to the message.
8. Create a world info entry and confirm it can be selected/edited.
9. Open preset and group editors to confirm forms load and save.

## Related Docs

- [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)
- [../docs/PROJECT_STATUS.md](../docs/PROJECT_STATUS.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [QUICK_START.md](QUICK_START.md)
- [FEATURE_CHECKLIST.md](FEATURE_CHECKLIST.md)
- [INTEGRATION.md](INTEGRATION.md)
