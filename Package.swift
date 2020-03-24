// swift-tools-version:5.0
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Workflow",
    platforms: [
        .iOS("9.3"),
        .macOS("10.12")
    ],
    products: [
        .library(
            name: "Workflow",
            targets: ["Workflow"]),
        .library(
            name: "WorkflowUI",
            targets: ["WorkflowUI"]),
        .library(
            name: "WorkflowReactiveSwift",
            targets: ["WorkflowReactiveSwift"]),
    ],
    dependencies: [
        .package(url: "https://github.com/ReactiveCocoa/ReactiveSwift.git", from: "6.0.0")
    ],
    targets: [
        .target(
            name: "Workflow",
            path: "swift/Workflow/Sources"),
        .testTarget(
            name: "WorkflowTests",
            dependencies: ["Workflow", "WorkflowReactiveSwift", "ReactiveSwift"],
            path: "swift/Workflow/Tests"),
        .target(
            name: "WorkflowUI",
            dependencies: ["Workflow", "WorkflowReactiveSwift", "ReactiveSwift"],
            path: "swift/WorkflowUI/Sources"),
        .testTarget(
            name: "WorkflowUITests",
            dependencies: ["WorkflowUI", "WorkflowReactiveSwift"],
            path: "swift/WorkflowUI/Tests"),
        .target(
            name: "WorkflowReactiveSwift",
            dependencies: ["Workflow", "ReactiveSwift"],
            path: "swift/WorkflowReactiveSwift/Sources"),
        .testTarget(
            name: "WorkflowReactiveSwiftTests",
            dependencies: ["Workflow", "WorkflowReactiveSwift", "ReactiveSwift"],
            path: "swift/WorkflowReactiveSwift/Tests"),
    ],
    swiftLanguageVersions: [.v5]
)
