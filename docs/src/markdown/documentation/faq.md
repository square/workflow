---
title: FAQ
index: 100
navigation:
    visible: true
    path: documentation
---

# Frequently Asked Questions


### How do I get involved and/or contribute?

- [Workflow is open source!](https://github.com/square/workflow)
- Stay tuned! We're considering hosting a public Slack channel for open source contributors.

### This seems clever. Can I stick with a traditional development approach?

Of course! Workflow was designed to make complex application architecture predictable and safe for large development teams. We're confident that it brings benefits even to smaller projects, but we make no promises that this is "the only right way to build software."

### Why do we need another architecture?

Architectural patterns with weak access controls and heavy use of shared mutable state make it incredibly difficult to fully understand the behavior of the code that we are writing. This quickly devolves into an arms race as the codebase grows: if every feature or component in the codebase might change anything at any time, bug fixes turn into a really sad game of whack-a-mole.

We have seen this pattern occur repeatedly in traditional mobile applications using patterns like MVC.

Workflow defines strong boundaries and contracts between separate parts of the application to ensure that our code remains predictable and maintainable as the size and complexity of the codebase grows.
