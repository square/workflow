'use strict';

const e = React.createElement;

class WorkflowInspectorApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {liked: false};
  }

  render() {
    // if (this.state.liked) {
    //   return 'You liked this.';
    // }
    //
    // return <button onClick={() => this.setState({liked: true})}>Like</button>;
    return <div>
      <header className="page-header">
        <div className="page-title">Workflow Web Inspector</div>
      </header>
      <nav className="page-nav">
        <ul>
          <li><a onClick={() => true}>All Workflow Events</a></li>
          <li>
            <button onClick={() => this.downloadTraceFile()}>Download Trace File</button>
          </li>
        </ul>
      </nav>
    </div>;
  }

  downloadTraceFile() {
    document.location.href = "/workflow_trace.json"
  }
}

const domContainer = document.querySelector('#react_container');
ReactDOM.render(e(WorkflowInspectorApp), domContainer);
