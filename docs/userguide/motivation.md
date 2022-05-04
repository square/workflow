# Why Workflow?

So you want me to take the application feature I have to develop and break it down into separate components? And then enumerate every possible state for each of those components? As well as writing classes or structs that represent each of these states in addition to the collection of objects that each component might pass to another? That sounds like a lot of work just to help the seller order a set of gift cards! Why make something simple so complicated? Why should I use Workflow?

Even those of us who use Workflow all the time end up asking this question. It’s a very reasonable question that we try to answer here. At the heart of the matter, there are two complementary justifications for Workflow, which we will expand on below:

1. Software clarity, correctness, and testability (especially at scale).
1. Encouraging programming paradigms that are best practices for the mobile domain.

## **Software clarity, correctness, and testability (at scale)**

I like to think that most of us have been there: It's our second straight day staring at the logs from over 200 customers. **We know what the problem is**: the user gets to screen Y and object foo’s state is bar, but foo *should not* be bar while in screen Y.

> *Why is foo bar?*

Unfortunately we don’t have any debug log for foo’s state at the time we start screen Y. We only have one when the user tries to click on button Z, and at that point the state is already bar even though it should only ever be baz or buz.

What happened? How did foo get to state bar on screen Y? Looking at the code, foo is shared state with 15 other screens, and it is mutable in all of them. The logic to update foo’s state in screen Y happens in code that is coupled to interaction with button Z, so we cannot simply add a unit test for this, we need a complex UI test to reproduce screen Y. **We don’t know *how* the problem happened and it almost seems like we *can’t* know how without significant effort!**

The story above is a little dramatic but the feeling it invokes should be familiar. It is a daunting task to reason through application code and build up a sufficient mental model of all possible side effects in any one feature area.

Now scale up the numbers a bit — foo is shared by 150 other screens — and the once daunting task seems almost impossible.

All mobile developers face some form of the above problem, and at Square within our Point of Sale applications we face the scaled up version every day.

**What do we want?**

- Clear boundaries *between* each feature’s software components that can be instrumented with logs and that have contracts that can be tested.
- Clear expectations for outcomes *within* a particular feature’s software component that can be verified for correctness with tests.
- Immutable State within any particular scope (e.g. Screen Y in the context above) so that the code handling mutations to provide a new State as a result of some event is in a “protected area” that can be instrumented and tested.
- A clear separation of the State updates from the presentation of the UI.

We want the conditions above because we want:

1. Not to have bugs like the one we started this discussion with. In other words, we want our tests to give us confidence in our application logic.
1. In the inevitable case that we do have a bug, we want to be able to isolate the scenario, reproduce the exact conditions, fix the bug and write a test so that it doesn’t happen again.

Workflow facilitates these goals for native mobile applications by providing a pattern (and a supporting application Runtime) similar to React, Elm, or any number of other web application JavaScript frameworks, not to mention new native mobile frameworks such as Jetpack Compose and SwiftUI.

Each logical component area is separated into a Workflow with a finite set of states and the logic to transition between them. Workflows can be composed together for a full feature with each Workflow’s signature specifying a clear contract. The Workflow Runtime’s event loop handles the production of new immutable states for each Workflow so that within the Workflow render logic it is immutable. Workflows can be executed and instrumented in a testable way with extra hooks for simple verification of outcomes in unit tests.

On an even simpler level Workflow improves clarity by giving a large team of developers a shared idiom of software components with which to discuss business logic across feature areas, and across mobile platforms (Android, iOS). Further, as the application is composed with multiple Workflows, the framework enables loose coupling between features to focus the impact of code changes.

## **Encouraging programming paradigms that are best practices for the mobile domain**

Mobile applications receive and display a lot of data! Our applications at Square certainly do. As a result of this, there is a growing trend towards **reactive programming** for mobile applications. In this paradigm, the application logic subscribes to a stream of data which is then pushed to the logic rather than having to be periodically pulled and operated on. This has the profound effect of ensuring that the data shown to the application user is never stale. This style of programming also makes clear that most mobile applications are a series of mapping operations on a stream of data that is eventually mapped into some UI.

Another mobile programming best practice (arising out of a long tradition) is to favor **declarative programming** over **imperative programming**. With this style choice, the code for a feature declares what should be occurring for a particular state, rather than consisting of a series of statements that are essentially *how* to make that occur. This is a best practice because when a program’s logic is defined in this way, it is very simple to test (so more likely to be tested!): “For state Y we expect Rendering Z;” “From state Y given input A we expect Rendering Z+.” Possibly more important, it is easier to read, comprehend quickly, and to reason about than a series of complex commands for the computer.

Workflows encourage a declarative style because each state of a particular component must be enumerated and then the Rendering (representation that gets passed to the UI framework) is *declared* for that particular state, alongside a declaration of what children and side effects *should be* running in that state. The well-tested and reliable Workflow runtime loop itself handles *how* to start and stop the children and side effects, reducing resource leaks. By requiring these formal definitions of each State, Rendering, and the Actions that will change the current state, Workflow naturally encourages declarative programming.

While reactive and declarative programming may be current best practices, there is one Software Engineering principle that has proven over and over again to be the most universal and the most important for systems of scale: **Separation of Concerns**. Any system of scale requires multiple separate components that can be worked on, tested, improved, and refactored independently by multiple teams of people. A system of multiple components requires communication and any good communication begs explicit structure and contracts.

> 🔎 For mobile applications at Square we have settled on the [Model-View-ViewModel](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) (MVVM) architecture as the structure for the topical separation of concerns of the layers of the application. MVVM’s unidirectional layered communication is the same as that of [Model-View-Presenter](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93presenter) (MVP), as opposed to the ‘circular’ communication of [Model-View-Controller](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) (MVC). MVVM’s use of a strict *binding* between the ViewModel and the View is the same as MVC, as opposed to the imperative interpretation of the Model in MVP. MVVM provides the reasoning and comprehension benefits of unidirectional data flow while *also* eliminating as much business logic as possible from the view layer and encouraging declarative ViewModels. At Square this works well because we have UI design frameworks that change infrequently (so keeping bindings up-to-date is not much overhead), but business logic that is constantly being updated (so emphasizing low coupling is important).
>
> Workflows embrace MVVM because the Rendering produced by a Workflow tree *is* the ViewModel, which can then be bound to any native mobile UI framework.
>
> For feature based separation of concerns we lean on Workflow’s facility for composition at scale via strong parent-child contracts and a hierarchical tree organization.

While building a Hello World Workflow may seem like overkill (although [it's](https://github.com/square/workflow-kotlin/blob/main/samples/hello-workflow/src/main/java/com/squareup/sample/helloworkflow/HelloWorkflow.kt) really not that bad!), the explicitness and contracts that Workflows require of the developer lay the structure for good communication. The composability of Workflows encourages **reuse** and encourages separation of concerns into the most appropriate reusable components.

There are even more platform-specific best practices that Workflow dovetails well with, such as structured concurrency with Kotlin coroutines, as each Worker or side effect can define a specific coroutine scope for the operations.

What about the next 10 years? Jetpack Compose UI and SwiftUI are establishing themselves as the native mobile UI toolkits of the future. They both embrace the same MVVM approach that Workflow does, and encourage thinking about the “composability” of *separate* components of your application. With this resonance, Workflows help you to prepare your mental model to adapt to these new UI toolkits, and shapes our codebase in a way that will ease our adoption of them.
