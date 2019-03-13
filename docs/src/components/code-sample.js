import React from 'react'
import { StyleSheet, css } from 'aphrodite'
import CodeBlock from './code-block'


const styles = StyleSheet.create({
    container: {
    },

    pickerBar: {
        display: "flex",
        flexDirection: "row"
    },

    link: {
        fontWeight: "medium",
        cursor: "pointer",
        marginRight: "12px"
    },

    inactiveLink: {
        color: "gray",

    },

    activeLink: {

    }
});

class CodeSample extends React.Component {

    constructor(props) {
        super(props)

        this.selectKotlin = this.selectKotlin.bind(this)
        this.selectSwift = this.selectSwift.bind(this)

        this.state = {
            language: "swift"
        }
    }

    selectSwift() {
        this.setState({
            language: "swift"
        })
    }

    selectKotlin() {
        this.setState({
            language: "kotlin"
        })
    }

    render() {

        var component = null;
        var swiftClass = null;
        var kotlinClass = null;

        if (this.state.language === "swift") {
            component = <CodeBlock language="swift">{this.props.swift}</CodeBlock>
            swiftClass = css([styles.activeLink, styles.link])
            kotlinClass = css([styles.inactiveLink, styles.link])
        } else {
            component = <CodeBlock language="kotlin">{this.props.kotlin}</CodeBlock>
            swiftClass = css([styles.inactiveLink, styles.link])
            kotlinClass = css([styles.activeLink, styles.link])
        }

        return <div className={css(styles.container)}>
            <div className={css(styles.pickerBar)}>
                <div className={swiftClass} onClick={this.selectSwift}>Swift</div>
                <div className={kotlinClass} onClick={this.selectKotlin}>Kotlin</div>
            </div>
            
            {component}

        </div>;
    }
  }

export default CodeSample
