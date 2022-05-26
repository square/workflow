# Coding Workflow UI

This page translates the high level discussion of [Workflow UI](../ui-concepts) into Android and iOS code.

## Separation of Concerns

Workflow maintains a rigid separation between its core runtime and its UI support.
The [Workflow Core](../concepts) modules are strictly Swift and Kotlin, with no dependencies on any UI framework.
Dependencies on Android and iOS are restricted to the Workflow UI modules, as you would expect.
This innate separation naturally puts developers on a path to avoid entangling view concerns with their app logic.

And note that we say "app logic" rather than "business logic."
In any interesting app, the code that manages navigation and other UI-releated behavior is likely to dwarf that for what we typically think of as model concerns, in both size and complexity.

We're all pretty good at capturing business concerns in tidy object-oriented models of items for sale, shopping carts, payment cards and the like, nicely decoupled from the UI world.
But the rest of the app, and in particular the bits about how our users navigate it?
Traditionally it's hard to keep that app-specific logic centralized, so that you can see what's going on; and even harder to keep it decoupled from your view system, so that it's easy to test.
The strict divide between Workflow UI and Workflow Core leads you to maintain that separation by accident.

## Bootstrapping

The following snippets demonstrate using Workflow to drive the root views of iOS and Android apps.
But really, you can host a Workflow driven UI anywhere you can show a view, whatever "view" means on your platform.

=== "iOS"
    ```Swift
    @UIApplicationMain
    class AppDelegate: UIResponder, UIApplicationDelegate {
        var window: UIWindow?

        func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
            window = UIWindow(frame: UIScreen.main.bounds)

            window?.rootViewController = ContainerViewController(workflow: RootWorkflow())

            window?.makeKeyAndVisible()

            return true
        }
    }
    ```

=== "Android Classic"
    Android classic makes things a little complicated (naturally), as your Workflow runtime has to survive configuration changes.
    Our habit is use a Jetpack `ViewModel` to solve that problem, on what is typically the only line of code in a Workflow app that deals with the Jetpack Lifecycle at all.

    ```kotlin title="HelloWorkflowActivity.kt"
    class HelloWorkflowActivity : AppCompatActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This ViewModel will survive configuration changes. It's instantiated
        // by the first call to androidx.activity.viewModels(), and that
        // original instance is returned by succeeding calls.
        val model: HelloViewModel by viewModels()
        setContentView(
          WorkflowLayout(this).apply { take(lifecycle, model.renderings) }
        )
      }
    }

    class HelloViewModel(savedState: SavedStateHandle) : ViewModel() {
      val renderings: StateFlow<HelloRendering> by lazy {
        renderWorkflowIn(
          workflow = HelloWorkflow,
          scope = viewModelScope,
          savedStateHandle = savedState
        )
      }
    }
    ```

=== "Android Jetpack Compose"
    ```Kotlin title="HelloComposeActivity.kt"
    class HelloComposeActivity : AppCompatActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
          val rendering by HelloWorkflow.renderAsState(props = Unit, onOutput = {})
          WorkflowRendering(rendering, ViewEnvironment.EMPTY)
        }
      }
    }

Android developers should note that classic and Compose bootstrapping are completely interchangeable.
Each style is able to display Screens of any type, regardless of whether they are set up to inflate `View` instances or to run `@Composeable` functions.

## Building views from Screens<a id="view-binding"></a>

Hello, Screen world.

=== "iOS"
    ```swift title="WelcomeScreen.swift"
    struct WelcomeScreen: Screen {
        var name: String
        var onNameChanged: (String) -> Void
        var onLoginTapped: () -> Void

        func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            return WelcomeViewController.description(for: self, environment: environment)
        }
    }

    private final class WelcomeViewController: ScreenViewController<WelcomeScreen> {
        override func viewDidLoad() { … }
        override func viewDidLayoutSubviews() { … }

        override func screenDidChange(from previousScreen: WelcomeScreen, previousEnvironment: ViewEnvironment) {
            super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

            nameField.text = screen.name
        }
    }
    ```

    iOS `Screen` classes are expected to provide matching `ViewControllerDescription` instances.
    A `ViewControllerDescription` can build a `UIViewController` on demand, or update an existing one if it's recognized by `ViewControllerDescription.canUpdate(UIViewController)`.

    These duties are all fullfilled by the provided `open class ScreenViewController`.
    It's like any other `ViewController`, with the addition of:

    * an open `screenDidChange()` method that the Workflow UI runtime calls with a series of `Screen` instances of the specified type
    * a `description()` class method, perfect for calling from `Screen.viewcontrollerDescription()`

=== "Android Classic"
    ```kotlin title="HelloScreen.kt"
    data class HelloScreen(
      val message: String,
      val onClick: () -> Unit
    ) : AndroidScreen<HelloScreen> {
      override val viewFactory: ScreenViewFactory<HelloScreen> =
        fromViewBinding(HelloViewBinding::inflate) { helloScreen, viewEnvironment ->
          helloMessage.text = helloScreen.message
          helloMessage.setOnClickListener { helloScreen.onClick() }
        }
    }
    ```

    The Android `Screen` interface is purely a marker type.
    It defines no Android-specific methods to ensure you have the option of keeping your app logic pure.
    If you don't need that rigor, life is simpler (and safer, no runtime errors) if your `Screen` renderings implement `AndroidScreen` instead.

    An `AndroidScreen` is required to provide a matching `ScreenViewFactory`.
    `ScreenViewFactory` returns `View` instances wrapped in `ScreenViewHolder` objects.
    `ScreenViewHolder.show` is called by the Workflow UI runtime to update the view with `Screen` instances that are deemed acceptible by `ScreenViewHolder.canShow`.

    In this example the `fromViewBinding` function creates a `ScreenViewFactory` that builds `View` instances using a Jetpack view binding, `HelloViewBinding`, presumably derived from `hello_view_binding.xml`.
    The lamda argument to the `fromViewBinding` provides the implementation for `ScreenViewHolder.show`, and is guaranteed that the given `helloScreen` parameter is of the appropriate type.

    Other factory functions are provided to work with layout resources directly, or to build views entirely from code.

=== "Android Jetpack Compose"
    ```kotlin title="HelloScreen.kt"
    data class HelloScreen(
      val message: String,
      val onClick: () -> Unit
    ) : ComposeScreen<HelloScreen> {
        @Composable override fun Content(viewEnvironment: ViewEnvironment) {
          Button(onClick) {
            Text(message)
          }
        }
    }
    ```

    Here, `HelloScreen` is implementing `ComposeScreen`.
    `ComposeScreen` extends the same `AndroidScreen` class used for classic Android, defining `@Composable fun Content()` to get its work done.
    `Content` is always called from a `@Composable Box()` context.

    !!! tip "It's smart"
        Even though `AndroidScreen` provides a thing called `ScreenViewFactory` to do its work, the factories built by `ComposeScreen` are able to recognize whether they're being called from a classic `View` or from a `@Composeable` function, and do the right thing.
        Workflow UI only creates `ComposeView` instances as needed: when a `@Composeable` needs to be shown in a `View`.
        If the factory is to be used in a `@Composable` context, `Content()` is called directly.

## Where is that "separation of concerns" you promised?

After all the chest-thumping above about [Separation of Concerns](#separation-of-concerns), the code samples above probably look pretty entangled.
That's because, while the Workflow libraries themselves are completely decoupled, they don't force that strict rigor on your app code.

If you aren't building, say, a core Workflow module that you want to ship separately from its Android and command line interfaces, you'd probably gain nothing from enforced separation but boilerplate and runtime errors.
And in practice, your Workflow unit tests won't call `viewFactory` and will build and run just fine against the JVM.
Likewise, at this point we've been building apps this way for hundreds of engineering years, and so far no one has called `viewControllerDescription()` and stashed a `UIViewController` in their workflow state.
(This is not a challenge.)

If you are one of the few who truly do need impermeable boundaries between your core and UI modules, they aren't hard to get.
Your `Screen` implementations can be defined completely separately from their view code and bound later.

=== "iOS"
    ```Swift title="WelcomeScreen.swift"
    struct WelcomeScreen {
        var name: String
        var onNameChanged: (String) -> Void
        var onLoginTapped: () -> Void
    }
    ```
    ```Swift title="WelcomeViewController.swift"
    extension WelcomeScreen: Screen {
        func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            return WelcomeViewController.description(for: self, environment: environment)
        }
    }

    private final class WelcomeViewController: ScreenViewController<WelcomeScreen> {
        // ...
    ```

=== "Android"
    ```Kotlin title="HelloScreen.kt"
    data class HelloScreen(
        val message: String,
        val onClick: () -> Unit
    ) : Screen
    ```
    ```kotlin title="HelloWorkflowGreenTheme.kt"
    private object HelloScreenGreenThemeViewFactory: ScreenViewFactory<HelloScreen>
    by ScreenViewFactory.fromViewBinding(GreenHelloViewBinding::inflate) { r, _ ->
          helloMessage.text = r.message
          helloMessage.setOnClickListener { r.onClick() }
        }
    }
    private val viewRegistry = ViewRegistry(HelloScreenGreenThemeViewFactory)

    val HelloWorkflowGreenTheme =
        HelloWorkflow.mapRenderings { it.withRegistry(viewRegistry) }
    ```

## Container screens make container views

A [container screen](../../Glossary#container-screen) is one that is built out of other Screens.
And naturally enough, the thing that a container screen drives is a [container view](../../Glossary#container-view): one that is able to host child views that are driven by Screen instances of arbitrary type.

Workflow UI provides two root container views out of the box, the `ContainerViewController` and `WorkflowLayout` classes discussed above, under [Bootstrapping](#bootstrapping).
They do most of their work by delegating to another pair of support view classes: `ScreenViewController` for iOS and `WorkflowViewStub` for Android.
Android also provides `@Composable fun WorkflowRendering()` for use with Jetpack Compose.
For something like a `SplitScreen` rendering, you'll write your own view code that does the same.

=== "iOS"
    ```swift title="SplitScreen.swift"
    public struct SplitScreen<LeadingScreenType: Screen, TrailingScreenType: Screen>: Screen {
        public let leadingScreen: LeadingScreenType

        public let trailingScreen: TrailingScreenType

        public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            return SplitScreenViewController.description(for: self, environment: environment)
        }
    ```
    ```swift title="SplitScreenViewController.swift"
    internal final class SplitScreenViewController<LeadingScreenType: Screen, TrailingScreenType: Screen>: ScreenViewController<SplitScreenViewController.ContainerScreen> {
        internal typealias ContainerScreen = SplitScreen<LeadingScreenType, TrailingScreenType>

        private var leadingContentViewController: DescribedViewController
        private lazy var leadingContainerView: ContainerView = .init()

        private lazy var separatorView: UIView = .init()

        private var trailingContentViewController: DescribedViewController
        private lazy var trailingContainerView: ContainerView = .init()

        required init(screen: ContainerScreen, environment: ViewEnvironment) {
            self.leadingContentViewController = DescribedViewController(
                screen: screen.leadingScreen,
                environment: environment
            )
            self.trailingContentViewController = DescribedViewController(
                screen: screen.trailingScreen,
                environment: environment
            )
            super.init(screen: screen, environment: environment)
        }

        override internal func screenDidChange(from previousScreen: ContainerScreen, previousEnvironment: ViewEnvironment) {
            super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

            update(with: screen)
        }

        private func update(with screen: ContainerScreen) {
            leadingContentViewController.update(
                screen: screen.leadingScreen,
                environment: environment
            )
            trailingContentViewController.update(
                screen: screen.trailingScreen,
                environment: environment
            )

            // Intentional force of layout pass after updating the child view controllers
            view.layoutIfNeeded()
        }

        override internal func viewDidLoad() {
            /** Lay out the two children horizontally, nothing workflow specific here. */

            update(with: screen)
        }

        override internal func viewDidLayoutSubviews() {
            /** Calculate the layout, nothing workflow specific here. */
        }
    ```

    The interesting thing here is the use of `DescribedViewController` to display the nested `leadingContent` and `trailingContent` Screens.
    `DescribedViewController` uses `Screen.viewControllerDescription` to build a new `UIViewController` if it needs to, or update an existing one if it can.
    Everything else is just run of the mill iOS view code.

=== "Android Classic"

    ```kotlin title="SplitScreen.kt"
    data class SplitScreen<L: Screen, T: Screen>(
      val leadingScreen: L,
      val trailingScreen: T
    ): AndroidScreen<SplitScreen<L, T>> {
      override val viewFactory: ScreenViewFactory<SplitScreen<L, T>> =
        fromViewBinding(SplitScreenBinding::inflate) { screen, _ ->
          leadingStub.show(leadingScreen)
          trailingStub.show(trailingScreen)
        }
    }
    ```
    ```xml title="split_screen.xml"
    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        >

      <com.squareup.workflow1.ui.WorkflowViewStub
          android:id="@+id/leading_stub"
          android:layout_width="0dp"
          android:layout_height="match_parent"
          android:layout_weight="30"
          />

      <View
          android:layout_width="1dp"
          android:layout_height="match_parent"
          android:background="@android:drawable/divider_horizontal_bright"
          />

      <com.squareup.workflow1.ui.WorkflowViewStub
          android:id="@+id/trailing_stub"
          android:layout_width="0dp"
          android:layout_height="match_parent"
          android:layout_weight="70"
          />

    </LinearLayout>
    ```

=== "Android Jetpack Compose"
    ```kotlin title="SplitScreen.kt"
    data class SplitScreen<L: Screen, T: Screen>(
      val leadingScreen: L,
      val trailingScreen: T
    ): ComposeScreen<SplitScreen<L, T>> {
      @Composable override fun Content(viewEnvironment: ViewEnvironment) {
        TODO()
      }
    }
    ```
