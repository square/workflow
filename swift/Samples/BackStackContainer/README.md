# BackStackContainer

An example of how a back stack container could be implemented allowing for declarative navigation backed by a UINavigationController.

Given a list of `BackStackScreen.Item`s will update the navigation controller with all of the view controllers in the stack. The push and pop animations are based on if the changed list of back stack items contains a new or previous screen.
