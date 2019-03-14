import React from 'react'

import H2 from '../components/h2'
import H3 from '../components/h3'
import Layout from '../components/layout'
import Intro from '../components/homepage/intro'
import Rule from '../components/rule'
import ThreeUp from '../components/three-up'
import CodeSample from '../components/code-sample'

export const frontmatter = {
  title: "Home",
}

class IndexPage extends React.Component {

  render() {

    return <Layout>
      <Intro />
      <Rule />
      <ThreeUp>
          <div>
            <H3>Take Control of Your Software</H3>
            <p>In a workflow, every state of your application is clearly modeled and easy to reason about so you can spend less time in the debugger searching for mysterious behavior.</p>
        </div>
        <div>
            <H3>
                (Re) Composable
            </H3>
            <p>
                Model complex features fractally out of small, focused workflows. And it's always easy to change your code later because tight coupling between workflows is effectively impossible.
            </p>
        </div>
        <div>
            <H3>
                Simple and Testable
            </H3>
            <p>
                Workflows makes building and testing easier by emphasizing a predictable, undidirectional data flow that prioritizes data over effects. Say goodbye to massive load-bearing View Controllers!
            </p>
        </div>   
      </ThreeUp>
      <Rule />

      <H2>
        Hello, World
      </H2>

      <CodeSample 
      swift={`
struct HelloWorldWorkflow: Workflow {

  struct State {
    var name: String
  }

  func makeInitialState() -> State {
    return State(name: "")
  }
    
  func compose(state: State, context: WorkflowContext<HelloWorldWorkflow>) -> HelloWorldViewModel {
    return HelloWorldViewModel(
      text: "Hello, \\(state.name)!",
      onNameChange: context.makeEventHandler { HelloWorldAction.nameChanged($0) }
    )
 }

}

enum HelloWorldAction: WorkflowAction {

  typealias WorkflowType = HelloWorldWorkflow
    
  case nameChanged(String)

  func apply(toState state: inout HelloWorldWorkflow.State) -> HelloWorldWorkflow.Output? {
    switch self {
    case .nameChanged(let name):
      state.name = name
    }
  }
  
}

struct HelloWorldViewModel {
  var text: String
  var onNameChange: (String) -> Void
}
      `} 
      
      kotlin={`
  /// Coming soon...
      `} />


    </Layout>


  }
}

export default IndexPage
