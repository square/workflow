import Workflow
import WorkflowUI
import Emoji


public struct Sample {
    public var title: String
    public var subtitle: String
    public var workflow: AnyWorkflow<Screen, Never>
    public var viewRegistry: ViewRegistry
}


extension Sample {

    public static var all: [Sample] {
        return [
            Sample(
                title: "Emoji",
                subtitle: "Simple Workflow demonstrating an input loop",
                workflow: AnyWorkflow(EmojiWorkflow()),
                viewRegistry: ViewRegistry.emojiScreens)
        ]
    }

}
