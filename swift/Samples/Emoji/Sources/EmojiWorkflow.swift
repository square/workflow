import Workflow
import WorkflowUI


extension ViewRegistry {

    public static var emojiScreens: ViewRegistry {
        var result = ViewRegistry()
        result.register(screenViewControllerType: EmojiViewController.self)
        return result
    }

}


public struct EmojiWorkflow: Workflow {

    public init() {}

    public typealias State = GameState

    public typealias Output = Never

    public typealias Rendering = Screen

    public func makeInitialState() -> State {
        return GameState()
    }

    public func workflowDidChange(from previousWorkflow: EmojiWorkflow, state: inout State) {

    }

    public func compose(state: State, context: WorkflowContext<EmojiWorkflow>) -> Screen {

        let sink = context.makeSink(of: Action.self)

        return EmojiScreen(
            gameState: state,
            sink: sink)
    }
}

