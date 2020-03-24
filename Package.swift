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
        .library(
            name: "WorkflowRxSwift",
            targets: ["WorkflowRxSwift"]),
    ],
    dependencies: [
        .package(url: "https://github.com/ReactiveCocoa/ReactiveSwift.git", from: "6.0.0"),
        .package(url: "https://github.com/ReactiveX/RxSwift.git", from: "4.4.0"),
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
        .target(
            name: "WorkflowRxSwift",
            dependencies: ["Workflow", "RxSwift"],
            path: "swift/WorkflowRxSwift/Sources"),
        .testTarget(
            name: "WorkflowRxSwiftTests",
            dependencies: ["Workflow", "WorkflowReactiveSwift", "RxSwift"],
            path: "swift/WorkflowRxSwift/Tests"),
    ],
    swiftLanguageVersions: [.v5]
)
