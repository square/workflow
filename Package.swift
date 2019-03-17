// swift-tools-version:4.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Workflow",
    products: [
        .library(
            name: "Workflow",
            targets: ["Workflow"]),
    ],
    dependencies: [
        .package(url: "https://github.com/ReactiveCocoa/ReactiveSwift.git", from: "5.0.0")
    ],
    targets: [
        .target(
            name: "Workflow",
            dependencies: ["ReactiveSwift"],
            path: "swift/Workflow/Sources"),
        .testTarget(
            name: "WorkflowTests",
            dependencies: ["Workflow"],
            path: "swift/Workflow/Tests"),
    ],
    swiftLanguageVersions: [.v4_2]
)
