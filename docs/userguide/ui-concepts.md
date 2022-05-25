# Workflow UI

This page provides a high level overview of Workflow UI, the companion that allows [Workflow Core](../core-workflow) to drive Android and iOS apps.

!!! warning Kotlin WIP
    The `Screen` interface that is so central to this discussion has reached Kotlin very recently, via `v1.8.0-beta01`.
    Thus, if you are working against the most recent non-beta release, you will find the code blocks here don't match what you're seeing.

    Square is using the `Screen` machinery introduced with the beta at the heart of our Android app suite, and we expect the beta period to be a short one.
    The Swift `Screen` protocol _et al._ have been in steady use for years.

## Separation of Concerns

Workflow maintains a rigid separation between its core runtime and its UI support.
The [Workflow Core](../core-workflow) modules are strictly Swift and Kotlin, with no dependencies on any UI framework.
Dependencies on Android and iOS are restricted to the Workflow UI modules, as you would expect.
This innate separation naturally puts developers on a path to avoid entangling view concerns with their app logic.

And note that we say "app logic" rather than "business logic."
In any interesting app, the code that manages navigation and other UI-releated behavior is likely to dwarf that for what we typically think of as model concerns, in both size and complexity.

We're all pretty good at capturing business concerns in tidy object-oriented models of items for sale, shopping carts, payment cards and the like, nicely decoupled from the UI world.
But the rest of the app, and in particular the bits about how our users navigate it?
Traditionally it's hard to keep that app-specific logic centralized, so that you can see what's going on; and even harder to keep it decoupled from your view system, so that it's easy to test.
The strict divide between Workflow UI and Workflow Core leads you to maintain that separation by accident.

## What's a Screen?

Most Workflow implementations produce `struct` / `data class` [renderings](../core-workflow#rendering) that can serve as view models.
Such a rendering provides enough data to paint a UI, and functions to be called in response to UI events.

These view model renderings implement the `Screen` [protocol](https://github.com/square/workflow-swift/blob/main/WorkflowUI/Sources/Screen/Screen.swift) / [interface](https://github.com/square/workflow-kotlin/blob/main/workflow-ui/core-common/src/main/java/com/squareup/workflow1/ui/Screen.kt) to advertise that this is their intended use.
The core service provided by Workflow UI is to transform `Screen` types into platform-specific view objects, and to keep those views updated as new `Screen` renderings are emitted.

`Screen` is the lynch pin that ties the Workflow Core and Workflow UI worlds together, the basic UI building block for Workflow-driven apps.
A `Screen` is an object that can be presented as a basic 2D UI box, like an `android.view.View` or a `UIViewController`.
And Workflow UI provides the glue that allows you to declare (at compile time!) that instances of `FooScreen : Screen` are used to drive `FooViewController`, `layout/foo_screen.xml`, or `@Composable fun Content(FooScreen, ViewEnvironment)`.

=== "iOS"
    ```Swift
    struct WelcomeScreen: Screen {
        var name: String
        var onNameChanged: (String) -> Void
        var onLoginTapped: () -> Void

        func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            return WelcomeViewController.description(for: self, environment: environment)
        }
    }

    private final class WelcomeViewController: ScreenViewController<WelcomeScreen> {
        // ...
    ```

=== "Android Classic"
    ```Kotlin
    data class HelloScreen(
      val message: String,
      val onClick: () -> Unit
    ) : AndroidScreen<HelloScreen> {
      override val viewFactory: ScreenViewFactory<HelloScreen> =
        fromViewBinding(HelloViewBinding::inflate) { r, _ ->
          helloMessage.text = r.message
          helloMessage.setOnClickListener { r.onClick() }
        }
    }
    ```

=== "Android Compose"
    ```Kotlin
    data class HelloScreen(
      val message: String,
      val onClick: () -> Unit
    ) : AndroidScreen<HelloScreen> by ComposeScreen {
      Button(onClick = onClick)
      Text(message)
    }
    ```

After all the chest-thumping above about [Separation of Concerns](#separation-of-concerns), these code samples probably don't look very separate. That's because, while the libraries themselves are completely decoupled, they don't force that strict rigor on your app code.

If you aren't building, say, a core Workflow module that you want to ship separately from its Android and command line interfaces, you'd probably gain nothing from enforced separation but boilerplate and runtime errors.
And in practice, your Workflow unit tests won't call `viewFactory` and will build and run just fine against the JVM.
Likewise, at this point we've been building apps this way for hundreds of engineering years, and so far no one has called `viewControllerDescription()` and stashed a `UIViewController` in their workflow state.
(This is not a challenge.)

If you're one of the few who truly do need impermeable boundaries between your core and UI modules, they aren't hard to get.
Your `Screen` implementations can be defined completely separately from their view code and bound later.

=== "iOS"
    ```Swift
    // WelcomeScreen.swift
    struct WelcomeScreen {
        var name: String
        var onNameChanged: (String) -> Void
        var onLoginTapped: () -> Void
    }

    // WelcomeViewController.swift
    extension WelcomeScreen: Screen {
        func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
            return WelcomeViewController.description(for: self, environment: environment)
        }
    }

    private final class WelcomeViewController: ScreenViewController<WelcomeScreen> {
        // ...
    ```

=== "Android"
    ```Kotlin
    // HelloScreen.kt
    data class HelloScreen(
      val message: String,
      val onClick: () -> Unit
    ) : Screen

    // HelloScreenViewFactory.kt
    object HelloScreenViewFactory: ScreenViewFactory<HelloScreen>
    by ScreenViewFactory.fromViewBinding(HelloViewBinding::inflate) { r, _ ->
          helloMessage.text = r.message
          helloMessage.setOnClickListener { r.onClick() }
        }
    }

    // HelloActivity.kt
    class HelloWorkflowActivity : AppCompatActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model: HelloViewModel by viewModels()
        setContentView(
          WorkflowLayout(this).apply { take(lifecycle, model.renderings) }
        )
      }
    }

    class HelloViewModel(savedState: SavedStateHandle) : ViewModel() {
      private val viewRegistry = ViewRegistry(HelloScreenViewFactory)

      val renderings: StateFlow<Screen> by lazy {
        renderWorkflowIn(
          workflow = HelloWorkflow.mapRenderings { it.withRegistry(viewRegistry) },
          scope = viewModelScope,
          savedStateHandle = savedState
        )
      }
    }
    ```

!!! faq "Why \"Screen\"?"
    We chose the name "Screen" because "View" would invite confusion with the like-named Android and iOS classes, and because "Box" didn't occur to us.
    (No one seems to have been bothered by the fact that `Screen` and iOS's `UIScreen` are unrelated.)

    And really, we went with "Screen" because it's the nebulous term that we and our users have always used to discuss our apps:
    "Go to the Settings screen."
    "How do I get to the Tipping screen?"
    "The Cart screen is shown in a modal over the Home screen on tablets."
    It's a safe bet you understood each of those sentences.

## Composition and Navigation: Screens all the way down

All of the above is expressive enough if a `Screen` really is modeling the entire display, but that's not very realistic.
It falls apart as soon as you need think about two or more screens at a time, in cases like:

* Back-stack style push / pop transitions
* Overview / detail split pane navigation, like an email app that shows an inbox on the left, and the selected message on the right
* Showing one screen over another one for a temporary modal session

Workflow handles all of this via the [Container Screen](../../Glossary#container-screen) pattern: Screens that are built out of other Screens.

TBD (read, "To Be Distilled from this [support ticket](https://github.com/square/workflow/issues/613)")