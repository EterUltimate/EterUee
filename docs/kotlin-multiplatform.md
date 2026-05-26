# Kotlin Multiplatform Compatibility

`shared` is the first Kotlin Multiplatform boundary for EterUee. It keeps Android-specific UI,
Room, Firebase, WebView, and Compose Android code out of portable targets while exposing stable
Kotlin/Native host surfaces for iOS, iPadOS, macOS, Windows 11, and Linux. The `desktop`
module is the first full desktop GUI packaging boundary and uses Compose Multiplatform/JVM
for native Windows and Linux distributions.

## Supported Targets

- Android: `android`
- iOS and iPadOS device: `iosArm64`
- iOS and iPadOS simulators: `iosSimulatorArm64`, `iosX64`
- macOS: `macosArm64`
- Windows 11 x64: `mingwX64`
- Linux x64: `linuxX64`
- Desktop JVM GUI: `desktop`

`macosX64` is intentionally not enabled because Kotlin 2.3 marks that target as deprecated.
Windows support is intentionally limited to Windows 11 x64. Older Windows releases are not part
of the compatibility contract even though the Kotlin/Native `mingwX64` toolchain can run on older
64-bit Windows versions.
Linux support targets x64 glibc environments through Kotlin/Native `linuxX64`. Full GUI
desktop packaging is owned by `desktop`; AppImage and Flatpak remain outside the current release
contract.

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
(cd apple && xcodebuild -list && xcodebuild -scheme EterUeeApple-Package -destination 'generic/platform=iOS Simulator' -sdk iphonesimulator build)
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
- `EterUeeWindowsBridge`
- `EterUeeLinuxBridge`

Android `roleplay` delegates prompt assembly to the shared engine, so Apple hosts and Android use
the same world-info insertion and context truncation behavior.

## Windows 11 Native Host

Windows uses the Kotlin/Native `mingwX64` target and a thin smoke executable under
`shared/src/mingwMain`. The stable host entrypoint is:

- `EterUeeWindowsBridge`

The smoke executable verifies that the compiled Windows binary can call the shared runtime
capability bridge and roleplay prompt engine:

```powershell
.\gradlew.bat :shared:compileKotlinMingwX64 :shared:runDebugExecutableMingwX64 :shared:mingwX64Test --no-daemon --stacktrace --console=plain
```

Expected smoke output:

```text
EterUeeShared Windows 11 smoke OK
```

The generated executable is written under:

```text
shared/build/bin/mingwX64/debugExecutable/
```

The GitHub Actions Windows job runs on GitHub's standard `windows-2025` x64 runner and validates
the `mingwX64` build, smoke executable, and native tests. Exact Windows 11 OS validation should be
run on a Windows 11 machine before release because GitHub does not currently provide a standard
Windows 11 x64 hosted-runner label.

## Linux Native Host

Linux uses the Kotlin/Native `linuxX64` target and a thin smoke executable under
`shared/src/linuxMain`. The stable host entrypoint is:

- `EterUeeLinuxBridge`

The smoke executable verifies that the compiled Linux binary can call the shared runtime capability
bridge and roleplay prompt engine:

```bash
./gradlew :shared:compileKotlinLinuxX64 :shared:runDebugExecutableLinuxX64 :shared:linuxX64Test --no-daemon --stacktrace --console=plain
```

Expected smoke output:

```text
EterUeeShared Linux x64 smoke OK
```

The generated executable is written under:

```text
shared/build/bin/linuxX64/debugExecutable/
```

The GitHub Actions Linux job runs on GitHub's standard Ubuntu x64 runner and validates the
`linuxX64` build, smoke executable, native tests, and uploaded debug executable.

## Desktop GUI Packages

`desktop/` is a Compose Multiplatform desktop application. It provides a packaged GUI shell with
separate Agent and Roleplay workspaces, reuses `shared` runtime and roleplay prompt logic, and
keeps the Android production data layer out of the desktop binary until those Android-only
dependencies are moved behind portable interfaces.

Run and test:

```bash
./gradlew :desktop:test
./gradlew :desktop:run
```

Build and verify a release app image:

```bash
./gradlew :desktop:desktopReleaseAppImage
```

The app image and checksum manifest are written to:

```text
desktop/build/compose/binaries/main-release/app/
desktop/build/compose/binaries/main-release/desktop-app-image-manifest.txt
```

Build and verify native installers on the matching operating system:

```powershell
.\gradlew.bat :desktop:desktopReleasePackage --no-daemon --stacktrace --console=plain
```

```bash
./gradlew :desktop:desktopReleasePackage --no-daemon --stacktrace --console=plain
```

Installer outputs are written to:

```text
desktop/build/compose/binaries/main-release/exe/
desktop/build/compose/binaries/main-release/deb/
desktop/build/compose/binaries/main-release/desktop-release-manifest.txt
```

`jpackage` cannot cross-build native installer formats. Windows `.exe` must be built on Windows
with WiX available; Linux `.deb` must be built on Linux with Debian packaging tools such as
`fakeroot` and `dpkg-dev`.

If Windows `.exe` packaging fails in `jpackage` after the app image has been validated, inspect
`desktop/build/compose/logs/packageReleaseExe/`. That failure is in the host installer toolchain;
`:desktop:desktopReleaseAppImage` still validates the runnable GUI app image, while CI and release
jobs run `:desktop:desktopReleasePackage` on clean Windows and Linux runners.

## Local Verification

Windows and Linux can validate the portable metadata and Android target:

```bash
./gradlew :shared:metadataCommonMainClasses :shared:compileAndroidMain :shared:testAndroidHostTest
```

Windows 11 can validate the Windows native target:

```powershell
.\gradlew.bat :shared:metadataCommonMainClasses :shared:compileKotlinMingwX64 :shared:runDebugExecutableMingwX64 :shared:mingwX64Test :shared:testAndroidHostTest --no-daemon --stacktrace --console=plain
```

Linux x64 can validate the Linux native target:

```bash
./gradlew :shared:metadataCommonMainClasses :shared:compileKotlinLinuxX64 :shared:runDebugExecutableLinuxX64 :shared:linuxX64Test :shared:testAndroidHostTest --no-daemon --stacktrace --console=plain
```

Apple framework linking must run on a macOS host:

```bash
./gradlew :shared:assembleEterUeeSharedDebugXCFramework
rm -rf apple/Frameworks/EterUeeShared.xcframework
mkdir -p apple/Frameworks
cp -R shared/build/XCFrameworks/debug/EterUeeShared.xcframework apple/Frameworks/
swift test --package-path apple
```

Desktop GUI verification can run on Windows and Linux:

```bash
./gradlew :desktop:desktopReleaseAppImage
./gradlew :desktop:desktopReleasePackage
```

## Migration Boundary

The Android app still owns the current production data layer and platform integrations. Move code
into `shared` only when it is free of Android framework, AndroidX-only, Room runtime, Firebase
Android, or Android WebView APIs. Move UI to `desktop` only after the feature depends on portable
contracts. Good early candidates are serializable data contracts, prompt/template logic, provider
request/response normalization, and roleplay domain rules.
