import React from 'react'
import { StyleSheet, css } from 'aphrodite'

import SyntaxHighlighter from 'react-syntax-highlighter/prism';
import { duotoneSpace } from 'react-syntax-highlighter/styles/prism';

import './code-highlight.css'


const styles = StyleSheet.create({
    container: {
        marginTop: "2em",
        marginBottom: "2em"
    }
});


class CodeBlock extends React.PureComponent {

    render() {
        return <div className={css(styles.container)}>
            <SyntaxHighlighter language={this.props.language} style={duotoneSpace}>
                {this.props.children}
            </SyntaxHighlighter>
        </div>
    }
}

export default CodeBlock