// swift-tools-version: 6.0

import PackageDescription

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
            dependencies: ["EterUeeShared"]
        ),
        .testTarget(
            name: "EterUeeAppleSupportTests",
            dependencies: ["EterUeeAppleSupport"]
        ),
    ]
)
