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

## Local Verification

Windows and Linux can validate the portable metadata and Android target:

```bash
./gradlew :shared:metadataCommonMainClasses :shared:compileAndroidMain :shared:testAndroidHostTest
```

Apple framework linking must run on a macOS host:

```bash
./gradlew :shared:assembleEterUeeSharedDebugXCFramework
```

## Migration Boundary

The Android app still owns the current production UI and platform integrations. Move code into
`shared` only when it is free of Android framework, AndroidX-only, Room runtime, Firebase Android,
or Android WebView APIs. Good early candidates are serializable data contracts, prompt/template
logic, provider request/response normalization, and roleplay domain rules.
