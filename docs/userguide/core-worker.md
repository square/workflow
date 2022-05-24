# The Role of a Worker

`Worker` is a protocol (in Swift) and interface (in Kotlin) that defines an asynchronous task that
can be performed by a `Workflow`. `Worker`s only emit outputs, they do not have a `Rendering` type.
They are similar to child workflows with `Void`/`Unit` rendering types.

A workflow can ask the infrastructure to await the result of a worker by passing that worker to the
`RenderContext.runningWorker` method within a call to the `render` method. A workflow can handle
outputs from a `Worker`.

## Workers provide a declarative window into the imperative world

As nice as it is to write declarative code, real apps need to interact with imperative APIs. Workers
allow wrapping imperative APIs so that Workflows can interact with them in a declarative fashion.
Instead of making imperative "start this, do that, now stop" calls, a Workflow can say "I declare
that this task should now be running" and let the infrastructure worry about ensuring the task is
actually started when necessary, continues running if it was already in flight, and torn down when
it's not needed anymore.

## Workers can perform side effects

Unlike workflows' `render` method, which can be called many times and must be idempotent, workers
are started and then ran until completion (or cancellation) – independently of how many times the
workflow running them is actually rendered. This means that side effects that should be performed
only once when a workflow enters a particular state, for example, should be placed into a `Worker`
that the workflow runs while in that state.

## Workers are cold reactive streams

Workers are effectively simple wrappers around asynchronous streams with explicit equivalence. In
Swift, workers are backed by ReactiveSwift [`SignalProducer`s](http://reactivecocoa.io/reactiveswift/docs/latest/SignalProducer.html#/s:13ReactiveSwift14SignalProducerV).
In Kotlin, they're backed by Kotlin [`Flow`s](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/).
They are also easily derived from [Reactive Streams Publishers](https://www.reactive-streams.org),
including RxJava `Observable`, `Flowable`, or `Single` instances.

## Worker subscriptions are managed automatically

While Workers are _backed_ by reactive streams with library-specific subscription APIs, you never
actually subscribe directly to a worker yourself. Instead, a Workflow asks the infrastructure to
run a worker, and the infrastructure will take care of initializing and tearing down the
subscription as appropriate – much like how child workflows' lifetimes are automatically managed by
the runtime. This makes it impossible to accidentally leak a subscription to a worker.

## Workers manage their _own_ internal state

Unlike Workflows, which are effectively collections of functions defining state transitions, Workers
represent long-running tasks. For example, Workers commonly execute network requests. The worker's
stream will open a socket and, either blocking on a background thread or asynchronously, read from
that socket and eventually emit data to the workflow that is running it.

## Workers define their own equivalence

Since Workers represent ongoing tasks, the infrastructure needs to be able to tell when two workers
represent the same task (so it doesn't perform the task twice), or when a worker has changed between
render passes such that it needs to be torn down and re-started for the new work.

For these reasons, any time a workflow requests that a worker be run in sequential render passes, it
is asked to compare itself with its last instance and determine if they are equivalent. In Swift,
this is determined by the `Worker` `isEquivalent:to:` method. `Worker`s that conform to `Equatable`
will automatically get an `isEquivalent:to:` method based on the `Equatable` implementation. In
Kotlin, the `Worker` interface defines the `doesSameWorkAs` method which is passed the previous worker.

!!! faq "Kotlin: Why don't Workers use `equals`?"
    Worker equivalence is a key part of the Worker API. The default implementation of `equals`,
    which just compares object identity, is almost always incorrect for workers. Defining a separate
    method forces implementers to think about how equivalence is defined.

## Workers are lifecycle-aware

Workers are aware of when they're started (just like Workflows), but they are also aware of when
they are torn down. This makes them handy for managing resources as well.
