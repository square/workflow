// swift-tools-version:5.0
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Workflow",
    products: [
        .library(
            name: "Workflow",
            targets: ["Workflow"]),
        .library(
            name: "WorkflowUI",
            targets: ["WorkflowUI"]),
    ],
    dependencies: [
        .package(url: "https://github.com/ReactiveCocoa/ReactiveSwift.git", from: "6.0.0")
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
        .target(
            name: "WorkflowUI",
            dependencies: ["Workflow"],
            path: "swift/WorkflowUI/Sources"),
        .testTarget(
            name: "WorkflowUITests",
            dependencies: ["WorkflowUI"],
            path: "swift/WorkflowUI/Tests"),
    ],
    swiftLanguageVersions: [.v5]
)
