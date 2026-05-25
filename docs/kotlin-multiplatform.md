# Kotlin Multiplatform Apple Compatibility

`shared` is the first Kotlin Multiplatform boundary for EterUee. It keeps Android-specific UI,
Room, Firebase, WebView, and Compose Android code out of Apple targets while exposing a stable
Kotlin/Native framework for iOS, iPadOS, and macOS hosts.

## Supported Targets

- Android: `android`
- iOS and iPadOS device: `iosArm64`
- iOS and iPadOS simulators: `iosSimulatorArm64`, `iosX64`
- macOS: `macosArm64`

`macosX64` is intentionally not enabled because Kotlin 2.3 marks that target as deprecated.

## Framework

The Apple framework base name is `EterUeeShared`.

On macOS:

```bash
./gradlew :shared:assembleEterUeeSharedDebugXCFramework
./gradlew :shared:assembleEterUeeSharedReleaseXCFramework
```

Outputs are written under:

```text
shared/build/XCFrameworks/
```

Swift and SwiftUI hosts should import `EterUeeShared` from the generated XCFramework.
The framework is dynamic so Apple hosts can link Kotlin/Native platform dependencies through the
framework boundary instead of re-linking the full Kotlin/Native static archive in each host target.

## Swift Package Host

`apple/` contains a Swift Package smoke host for work that can be completed before physical-device
testing. It expects the generated XCFramework at:

```text
apple/Frameworks/EterUeeShared.xcframework
```

The CI job creates that folder from `shared/build/XCFrameworks/debug/EterUeeShared.xcframework`,
then runs:

```bash
swift test --package-path apple
xcodebuild -scheme EterUeeAppleSupport -packagePath apple -destination 'generic/platform=iOS Simulator' -sdk iphonesimulator build
```

This verifies that Swift can import the Kotlin/Native framework, call the stable bridge, run a
macOS host test, and compile an iOS/iPadOS simulator target without requiring a real device.
iPadOS support is covered by the iOS device and simulator Kotlin/Native targets.
The macOS target follows the current stable Kotlin/Native Apple Silicon target.

The checked-in Swift target intentionally stays thin. Product code should call stable bridge APIs
or add SwiftUI screens on top of `EterUeeAppleSupport`, while platform storage, notification,
Keychain, and camera/file-provider behavior remain Apple-host responsibilities.

## Shared Roleplay Domain

The first shared domain surface is roleplay prompt assembly:

- `RoleplayPromptBuildRequest`
- `SharedChatMessage`
- `SharedWorldInfoEntry`
- `RoleplayPromptEngine`
- `EterUeeAppleBridge`

Android `roleplay` delegates prompt assembly to the shared engine, so Apple hosts and Android use
the same world-info insertion and context truncation behavior.

## Local Verification

Windows and Linux can validate the portable metadata and Android target:

```bash
./gradlew :shared:metadataCommonMainClasses :shared:compileAndroidMain :shared:testAndroidHostTest
```

Apple framework linking must run on a macOS host:

```bash
./gradlew :shared:assembleEterUeeSharedDebugXCFramework
rm -rf apple/Frameworks/EterUeeShared.xcframework
mkdir -p apple/Frameworks
cp -R shared/build/XCFrameworks/debug/EterUeeShared.xcframework apple/Frameworks/
swift test --package-path apple
```

## Migration Boundary

The Android app still owns the current production UI and platform integrations. Move code into
`shared` only when it is free of Android framework, AndroidX-only, Room runtime, Firebase Android,
or Android WebView APIs. Good early candidates are serializable data contracts, prompt/template
logic, provider request/response normalization, and roleplay domain rules.
