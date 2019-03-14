---
title: Workflow Documentation
index: 0
navigation:
    visible: true
    path: documentation
---

# Workflow Documentation


### This section provides an overview of the Workflow library and the tools that you will use to build your features and products.

Workflow is a library for building applications in a simple, consistant shape.


```
┌────────────────────────────┐                    ┌────────────────────────────┐
│                            │                    │                            │
│                            │                    │                            │
│                            │                    │                            │
│                            │  ◀ ─ ─ ─ ─ ─ ─ ─   │                            │
│                            │                    │                            │
│         Workflows          │  ◀ ─ ─ ─ ─ ─ ─ ─   │                            │
│                            │                    │                            │
│                            │  ◀ ─ ─ ─ ─ ─ ─ ─   │             UI             │
│                            │                    │                            │
│                            │      Events        │                            │
│                            │                    │                            │
└────────────────────────────┘                    │                            │
              │                                   │                            │
              ▼                                   │                            │
       ┌─────────────┐                            │                            │
       │ View Models │                            │                            │
       └─────────────┘                            └────────────────────────────┘
              │                                                  ▲              
              ▼                                                  │              
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                                  Container                                   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Workflows

The Workflows at the left of the diagram contain all state and business logic for the application. This is where network requests happen, navigation decisions are made, models are saved to or loaded from disk – if it's not UI, it's in this box.

### View Models

The primary job of the Workflows is to emit an observable stream of view models representing the current state of the application's UI. You will sometimes hear these view models referred to as 'screens', which is just another way to refer to a view model that contains the data for an entire screen in the app.

### Container

The container is responsible for plumbing together the two separate halves of the application. It subscribes to the stream of view models that the workflows provide, then implements the logic to update the live UI whenever a new view model is emitted.

### UI

This is typically conventional platform-specific UI code. One important note is that UI code should never attempt to navigate using system components (navigation controller pushes, modal presentation, etc). In this architecture the workflows are in charge – any navigation that happens outside of the workflow will be disregarded and stomped on during the next update cycle.

### Events

In order for the application to actually do anything, the workflow needs to receive events from the UI. When the user interacts with the application by, for example, tapping a button, the workflow receives that event – which may trigger a simple state transition, or more complex behavior such as a network request.
