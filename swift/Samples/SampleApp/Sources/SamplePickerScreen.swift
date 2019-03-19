import Workflow
import WorkflowUI

struct SamplePickerScreen: Screen {
    var samples: [Sample]
    var onSelectSample: Sink<Sample>
}
