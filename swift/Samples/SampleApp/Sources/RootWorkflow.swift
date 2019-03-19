import Workflow
import WorkflowUI
import BackStack

struct RootWorkflow: Workflow {

    var samples = Sample.all

    enum State {
        case picker
        case showing(sample: Sample)
    }

    typealias Output = Never

    typealias Rendering = BackStackScreen

    func makeInitialState() -> State {
        return .picker
    }

    func workflowDidChange(from previousWorkflow: RootWorkflow, state: inout State) {

    }

    func compose(state: State, context: WorkflowContext<RootWorkflow>) -> BackStackScreen {

        let sink = context.makeSink(of: Action.self)

        let pickerScreen = SamplePickerScreen(
            samples: samples,
            onSelectSample: sink.contraMap { Action.select(sample: $0) })

        let pickerItem = BackStackItem(
            key: "picker",
            title: "Workflow Samples",
            screen: pickerScreen)

        var backStackScreen = BackStackScreen(items: [pickerItem])

        if case .showing(let sample) = state {

            let sampleScreen = sample
                .workflow
                .rendered(with: context)

            var sampleItem = BackStackItem(
                key: "sample",
                title: sample.title,
                screen: sampleScreen)

            sampleItem.backAction = .back(handler: {
                sink.send(.backToPicker)
            })

            backStackScreen.items.append(sampleItem)
        }

        return backStackScreen
    }

}

extension RootWorkflow {

    enum Action: WorkflowAction {
        case select(sample: Sample)
        case backToPicker

        typealias WorkflowType = RootWorkflow

        func apply(toState state: inout RootWorkflow.State) -> Never? {
            switch self {
            case .backToPicker: state = .picker
            case .select(let sample): state = .showing(sample: sample)
            }
            return nil
        }
    }

}

