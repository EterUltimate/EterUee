// swift-tools-version: 6.0

import PackageDescription

let kotlinNativeLinkerSettings: [LinkerSetting] = [
    .linkedFramework("Foundation"),
    .linkedFramework("CoreFoundation"),
    .linkedLibrary("xpc", .when(platforms: [.macOS])),
]

let package = Package(
    name: "EterUeeApple",
    platforms: [
        .iOS(.v16),
        .macOS(.v14),
    ],
    products: [
        .library(
            name: "EterUeeAppleSupport",
            targets: ["EterUeeAppleSupport"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "EterUeeShared",
            path: "Frameworks/EterUeeShared.xcframework"
        ),
        .target(
            name: "EterUeeAppleSupport",
            dependencies: ["EterUeeShared"],
            linkerSettings: kotlinNativeLinkerSettings
        ),
        .testTarget(
            name: "EterUeeAppleSupportTests",
            dependencies: ["EterUeeAppleSupport"],
            linkerSettings: kotlinNativeLinkerSettings
        ),
    ]
)
