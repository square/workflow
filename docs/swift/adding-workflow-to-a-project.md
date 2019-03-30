# Adding Workflow to a project

This document will guide you through the process of adding Workflow to an iOS project.


## Libraries

You'll need the following four libraries:

```swift
import Workflow
import WorkflowUI
import ReactiveSwift
import Result
```

The easiest way to integrate these libraries is via Cocoapods. If you are using Cocoapods, you can simply add the dependencies to your `.podspec`.

```ruby
# MySoftware.podspec
Pod::Spec.new do |s|
    # ...

    s.dependency 'Workflow'
    s.dependency 'WorkflowUI'
    s.dependency 'ReactiveSwift'
    s.dependency 'Result'

    # ...
end
```
