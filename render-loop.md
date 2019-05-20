Issue [348](https://github.com/square/workflow/issues/348)

# Problem statement:

Events can be lost, due to the render pass happening (async) after the processing of an event. Results in a sink no longer being valid for a period before the UI has been provided a new sink.


# Current behavior:
```
(workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
--- main thread yielded ---
(UI tap -> screen.onTap) -> workflow.Action.apply(.onTap)
--- main thread yielded ---
(workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
```

## Detail of failure mode:

```
(workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
--- main thread yielded ---
(UI tap -> screen.onTap) -> workflow.Action.apply(.onTap)
--- main thread yielded ---

--- NEXT RENDER PASS QUEUED ---
(UI tap -> screen.onTap) -> "into the ether" (the previous sink is no longer valid, specifically the signal behind it is not longer subscribed)
--- main thread yielded ---
(workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
```

## Initial Design proposal:
```
(workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
--- main thread yielded ---
(UI tap -> screen.onTap) -> workflow.Action.apply(.onTap) -> (workflow render -> screen) -> (UI "onTap" bound to -> screen.onTap)
```


# Details of actual subscription/wiring process:

## Visualization

```
      /--------\
      | Screen |
      \--------/
          ^
          |
          | Render (sync)
          |
/-------------------\
|                   | async /---------------\ sync /------\
|                   |<------| Subscriptions |<-----| Sink |
|                   |       \---------------/      \------/
|     Workflow      |
|                   |   async   /--------\
|                   |<----------| Worker |
|                   |           \--------/
\-------------------/
         ^
         |
         | Output (sync)
         |
/-------------------\
|                   |
|                   |
|       Child       | async  /-----\
|     Workflow      |<-------| ... |
|                   |        \-----/
|                   |
|                   |
\-------------------/
```

Render:

call `render` on the workflow with "RenderContext":
- create sinks (each sink creates a signal)
- create children (each child creates a signal for output)
- create workers (each worker has a signal producer created, which specifically has a signal. Current subscribed on the workflow scheduler)

(aside, the `SubtreeManager`'s `onUpdate` closure is wired to the `WorkflowNode` `handle` method (that processes actions)

`render` completes:
- context is invalidated. Calls to it will assert.
- childWorkflow `.onUpdate` closure is wired up to the subtree managers `.handle` function (which calls `.onUpdate`)
- childWorker `.onUpdate` closure is wired up to the subtree managers `.handle` function
- The event sources (ie: the signal's for all of the sinks, as well as any `subscribe` calls) are merged into a single signal, and mapped into an Output of "update"(`AnyWorkflowAction`)
    - The other type of `Output` is childDidUpdate, which will induce a render pass when it gets to the top of the tree.
- The merged signal of event sources is sent to the `childEvent` signal (which is a signal of signals). It's `flatMap`'d to `.latest`, so only one event will be handled from all the signals.
    - When any of the signal's emit, the `handle` function will be called (which calls the `.onUpdate` to get back to the WorkflowNode)

The `rendering` is returned from the subtree manager's "render".


# Use Cases

## General Design Goals/Constraints

- Only one event should be handled per render pass. Handling a single event must result in a new `render`.
    - This guarantees that the Action being handled is handled from an "expected" state.
- Thus, there are two limitations:
    - Multiple events cannot happen at the "same" time.
    - Events cannot be queued. (This is potentially debatable)

The two limitations are in some sense the exact same limitation (since if there is a queue and only the first is pulled off, there is only one event. If two calls happen synchronously two would still be queued, but again only the first is pulled off).

## UI originated Events

The callback invoked from UI to generate an event must be (conceptually) valid the next time the UI is able to generate an event (from UIKit).
- This means that if a new callback is handed to the UI on each render pass (and is only valid for a single event), the new callback must be provided before UIKit can generate a new UI event.
    - (as it stands now) If we fatal errored on a second event being send to a sink, we would currently have a race condition that would cause crashes. Instead, we are silently dropping events.
- Can we dispatch the event down to the workflow asynchronously when it's received?
    - No? If the UI thread yields after sending a single event, and then our queue gets priority, it would have to get back to the UI thread to update the screen. The UI thread could send another event.
- The sink should conceptually become "valid" as soon as `render` completes.
- An event update must immediately change the state and cause render to be called.

## Workers

A worker emits some action (ideally in the future) from the SignalProducer that is returned from `run`. The "ideally in the future" part is the complexity we need to handle:

1) The signal producer does something async, and then emits an action
    - This is trivial, and handled.
2) The signal producer is in the shape of `SignalProducer(value: <whatever>)` that emits immediately.
    - We need to handle this async, so either it must be subscribed on a scheduler that is not immediate, or not started until after `render` is completed.

## Subscriptions

1) A subscription emits a single event some time in the future.
    - This is trivial, and handled.
2) A subscription emits immediatley when it's subscribed.
    - Due to how Signal conceptually are, this shouldn't happen. However, it *could* with sink's (and it's been the case in the past where it does happen immediately, and it's dropped).
    - Back to "a sink should not be valid until `render` is completed.
3) A subscription emits two values synchronously.
    - This is not supported, see UI events above (again, worth revisiting)

## Child Workflows (Output events)

Child workflows are likely the most "easy" ones to handle (for output events). Because a child workflow can *only* generate an output during an `apply` call on an Action, they are just the originator of an event and it should be trivial to ensure they synchronously flow up the tree.

- Child has an action happen from some source.
- Emits and output
- Parent immediately received the output as an action and processes it, optionally emitting another output (and the loop continues)

This should be made clear in the subtree manager to guarantee it's the behavior. It definitely must not move schedulers, or we would have a partial tree update (and be missing calls to render).

# Queueing Events

As noted above, currently only a single event (action) is processed for each render loop. This can result in dropped events if (for instance) `send` is called twice on a sink synchronously. The first event is processed, and the second is dropped. This behavior can present with subscriptions and workers. Child workflows do not have this behavior, as only a single output can be emitted with each Action `apply`.

Adding queueing of events conceptually seems like a way to solve this, but it introduces (likely) "surprising" behavior when applying the actions.

## Example 1 (subscription):
1) `render` is called, and a signal is subscribed to, due to being in a "waiting" state (just a case in the state enum).
    -  The `apply` of the subscribed signal action assumes that the current state is `waiting`, due to it only being subscribed when that is the current state.
2) The signal emits two events, which are mapped into two actions.
    - The second action is queued.
3) The first action is applied. The `apply` updates the state to "not waiting". `render` occures, and the signal is *not* subscribed to.
4) The queued event from the previous render pass is applied. The state is no longer `waiting`, so the assumption of the state when applying the action is invalid.
    - The state is updated a second time, and potentially a behavior bug has happened.

The "solution" when writing this is to validate the expected state anytime an action is being applied. However, this increases the likelihood of bugs, as it may not be expected that a workflow would have to handle actions from a previous render loop.

## Example 2 (UI event):

TODO

# Proposal

*Discount multiple events during a single render pass. Queueing adds too much complexity when writing business logic.*

To ensure that events are not lost from the UI, as well as subscriptions and workers, the subtree manager will be updated to have a mix of synchronous and asynchronous behaviors depending on the event source.

- UI Events (from a `sink`). The sink type will no longer be backed by a subscription/signal, but instead will be a separate underlying type.
    - A `sink` will only become "valid" after the call to `render` has completed. This prevents an action being emitted during a `render` call, avoid reentrancy.
    - UI Events from a sink will be applied synchronously. They will immediately induce an update, and `render` will be called prior to returning from the `sink.send` call.
- Child output events will (continue) be applied synchronously.
- Subscriptions to signals we be done on a separate scheduler from the UI scheduler. This ensures that a `Signal` that emits immediately on `subscribe` will not be dropped.
- Workers that must be `run` will be subscribed on the same scheduler as subscriptions. This ensures that immediate events will not be dropped.

The handling of updates from events will be separated from how the events are injected into the  system. This provides the path for a `sink` event to be immediate, but workers and subscriptions to be asynchronous.

## Visualized proposal

```
      /--------\
      | Screen |
      \--------/
          ^
          |
          | Render (sync)
          |
/-------------------\
|                   |      /--------\    sync    /------\
|                   |      |        |<-----------| Sink |
|                   |      |        |            \------/
|                   |      |        |
|                   | sync |        | async /---------------\
|     Workflow      |<-----| Update |<------| Subscriptions |
|                   |      |        |       \---------------/
|                   |      |        |
|                   |      |        |   async   /--------\
|                   |      |        |<----------| Worker |
|                   |      \--------/           \--------/
\-------------------/           ^
                                |
                                |
                                | Output (sync)
                                |
                       /-------------------\
                       |                   |
                       |                   |
                       |       Child       | (as above) /-----\
                       |     Workflow      |<-----------| ... |
                       |                   |            \-----/
                       |                   |
                       |                   |
                       \-------------------/
```
