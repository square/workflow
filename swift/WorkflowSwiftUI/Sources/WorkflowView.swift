#if canImport(SwiftUI) && canImport(Combine) && swift(>=5.1)

    import Combine
    import ReactiveSwift
    import SwiftUI
    import Workflow

    /// Hosts a Workflow-powered view hierarchy.
    ///
    /// Example:
    ///
    /// ```
    /// var body: some View {
    ///     WorkflowView(workflow: MyWorkflow(), onOutput: { self.handleOutput($0) }) { rendering in
    ///         VStack {
    ///
    ///             Text("The value is \(rendering.value)")
    ///
    ///             Button(action: rendering.onIncrement) {
    ///                 Text("+")
    ///             }
    ///
    ///             Button(action: rendering.onDecrement) {
    ///                 Text("-")
    ///             }
    ///
    ///         }
    ///     }
    /// }
    /// ```
    @available(iOS 13.0, macOS 10.15, *)
    public struct WorkflowView<T: Workflow, Content: View>: View {
        /// The workflow implementation to use
        public var workflow: T

        /// A handler for any output events emitted by the workflow
        public var onOutput: (T.Output) -> Void

        /// A closure that maps the workflow's rendering type into a view of type `Content`.
        public var content: (T.Rendering) -> Content

        public init(workflow: T, onOutput: @escaping (T.Output) -> Void, content: @escaping (T.Rendering) -> Content) {
            self.onOutput = onOutput
            self.content = content
            self.workflow = workflow
        }

        public var body: some View {
            IntermediateView(
                workflow: workflow,
                onOutput: onOutput,
                content: content
            )
        }
    }

    @available(iOS 13.0, macOS 10.15, *)
    extension WorkflowView where T.Output == Never {
        /// Convenience initializer for workflows with no output.
        public init(workflow: T, content: @escaping (T.Rendering) -> Content) {
            self.init(workflow: workflow, onOutput: { _ in }, content: content)
        }
    }

    @available(iOS 13.0, macOS 10.15, *)
    extension WorkflowView where T.Rendering == Content {
        /// Convenience initializer for workflows whose rendering type conforms to `View`.
        public init(workflow: T, onOutput: @escaping (T.Output) -> Void) {
            self.init(workflow: workflow, onOutput: onOutput, content: { $0 })
        }
    }

    @available(iOS 13.0, macOS 10.15, *)
    extension WorkflowView where T.Output == Never, T.Rendering == Content {
        /// Convenience initializer for workflows with no output whose rendering type conforms to `View`.
        public init(workflow: T) {
            self.init(workflow: workflow, onOutput: { _ in }, content: { $0 })
        }
    }

    // We use a `UIViewController/UIViewControllerRepresentable` here to drop back to UIKit because it gives us a predictable
    // update mechanism via `updateUIViewController(_:context:)`. If we were to manage a `WorkflowHost` instance directly
    // within a SwiftUI view we would need to update the host with the updated workflow from our implementation of `body`.
    // Performing work within the body accessor is strongly discouraged, so we jump back into UIKit for a second here.
    @available(iOS 13.0, macOS 10.15, *)
    fileprivate struct IntermediateView<T: Workflow, Content: View> {
        var workflow: T
        var onOutput: (T.Output) -> Void
        var content: (T.Rendering) -> Content
    }

    #if canImport(UIKit)

        import UIKit

        @available(iOS 13.0, *)
        extension IntermediateView: UIViewControllerRepresentable {
            func makeUIViewController(context: UIViewControllerRepresentableContext<IntermediateView<T, Content>>) -> WorkflowHostingViewController<T, Content> {
                WorkflowHostingViewController(workflow: workflow, content: content)
            }

            func updateUIViewController(_ uiViewController: WorkflowHostingViewController<T, Content>, context: UIViewControllerRepresentableContext<IntermediateView<T, Content>>) {
                uiViewController.content = content
                uiViewController.onOutput = onOutput
                uiViewController.update(to: workflow)
            }
        }

        @available(iOS 13.0, *)
        fileprivate final class WorkflowHostingViewController<T: Workflow, Content: View>: UIViewController {
            private let workflowHost: WorkflowHost<T>
            private let hostingController: UIHostingController<RootView<Content>>
            private let rootViewProvider: RootViewProvider<Content>

            var content: (T.Rendering) -> Content
            var onOutput: (T.Output) -> Void

            private let (lifetime, token) = Lifetime.make()

            init(workflow: T, content: @escaping (T.Rendering) -> Content) {
                self.content = content
                self.onOutput = { _ in }

                self.workflowHost = WorkflowHost(workflow: workflow)
                self.rootViewProvider = RootViewProvider(view: content(workflowHost.rendering.value))
                self.hostingController = UIHostingController(rootView: RootView(provider: rootViewProvider))

                super.init(nibName: nil, bundle: nil)

                addChild(hostingController)
                view.addSubview(hostingController.view)
                hostingController.didMove(toParent: self)

                workflowHost
                    .rendering
                    .signal
                    .take(during: lifetime)
                    .observeValues { [weak self] rendering in
                        self?.didEmit(rendering: rendering)
                    }

                workflowHost
                    .output
                    .take(during: lifetime)
                    .observeValues { [weak self] output in
                        self?.didEmit(output: output)
                    }
            }

            required init?(coder: NSCoder) {
                fatalError("init(coder:) has not been implemented")
            }

            override func viewDidLayoutSubviews() {
                super.viewDidLayoutSubviews()
                hostingController.view.frame = view.bounds
            }

            private func didEmit(rendering: T.Rendering) {
                rootViewProvider.view = content(rendering)
            }

            private func didEmit(output: T.Output) {
                onOutput(output)
            }

            func update(to workflow: T) {
                workflowHost.update(workflow: workflow)
            }
        }

    #elseif canImport(AppKit)

        import AppKit

        @available(OSX 10.15, *)
        extension IntermediateView: NSViewControllerRepresentable {
            func makeNSViewController(context: NSViewControllerRepresentableContext<IntermediateView<T, Content>>) -> WorkflowHostingViewController<T, Content> {
                WorkflowHostingViewController(workflow: workflow, content: content)
            }

            func updateNSViewController(_ nsViewController: WorkflowHostingViewController<T, Content>, context: NSViewControllerRepresentableContext<IntermediateView<T, Content>>) {
                nsViewController.content = content
                nsViewController.onOutput = onOutput
                nsViewController.update(to: workflow)
            }
        }

        @available(macOS 10.15, *)
        fileprivate final class WorkflowHostingViewController<T: Workflow, Content: View>: NSViewController {
            private let workflowHost: WorkflowHost<T>
            private let hostingController: NSHostingController<RootView<Content>>
            private let rootViewProvider: RootViewProvider<Content>

            var content: (T.Rendering) -> Content
            var onOutput: (T.Output) -> Void

            private let (lifetime, token) = Lifetime.make()

            init(workflow: T, content: @escaping (T.Rendering) -> Content) {
                self.content = content
                self.onOutput = { _ in }

                self.workflowHost = WorkflowHost(workflow: workflow)
                self.rootViewProvider = RootViewProvider(view: content(workflowHost.rendering.value))
                self.hostingController = NSHostingController(rootView: RootView(provider: rootViewProvider))

                super.init(nibName: nil, bundle: nil)

                addChild(hostingController)

                workflowHost
                    .rendering
                    .signal
                    .take(during: lifetime)
                    .observeValues { [weak self] rendering in
                        self?.didEmit(rendering: rendering)
                    }

                workflowHost
                    .output
                    .take(during: lifetime)
                    .observeValues { [weak self] output in
                        self?.didEmit(output: output)
                    }
            }

            required init?(coder: NSCoder) {
                fatalError("init(coder:) has not been implemented")
            }

            override func loadView() {
                view = NSView()
            }

            override func viewDidLoad() {
                super.viewDidLoad()
                view.addSubview(hostingController.view)
            }

            override func viewDidLayout() {
                super.viewDidLayout()
                hostingController.view.frame = view.bounds
            }

            private func didEmit(rendering: T.Rendering) {
                rootViewProvider.view = content(rendering)
            }

            private func didEmit(output: T.Output) {
                onOutput(output)
            }

            func update(to workflow: T) {
                workflowHost.update(workflow: workflow)
            }
        }

    #endif

    // Assigning `rootView` on a `UIHostingController` causes unwanted animated transitions.
    // To avoid this, we never change the root view, but we pass down an `ObservableObject`
    // so that we can still update the hierarchy as the workflow emits new renderings.
    @available(iOS 13.0, macOS 10.15, *)
    fileprivate final class RootViewProvider<T: View>: ObservableObject {
        @Published var view: T

        init(view: T) {
            self.view = view
        }
    }

    @available(iOS 13.0, macOS 10.15, *)
    fileprivate struct RootView<T: View>: View {
        @ObservedObject var provider: RootViewProvider<T>

        var body: some View {
            provider.view
        }
    }

#endif
