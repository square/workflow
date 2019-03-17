import React from 'react'
import PropTypes from 'prop-types'

import rehypeReact from "rehype-react"

import CodeBlock from '../components/code-block'

import Rule from '../components/rule'

import InlineCode from './inline-code'

import H1 from '../components/h1'
import H2 from '../components/h2'
import H3 from '../components/h3'




class Code extends React.Component {
    render() {
        if (this.props.containedInPre === true) {
            /// Search for language

            var language = null

            if (this.props.className !== undefined) {
                let classes = this.props.className.split(" ")
                for (let classStr of classes) {
                    if (classStr.startsWith("language-")) {
                        language = classStr.replace("language-", "")
                    }
                }
            }



            console.log(language)

            return <CodeBlock language={language}>
                {this.props.children}
            </CodeBlock>
        } else {
            return <InlineCode>{this.props.children}</InlineCode>
        }
    }
}

const Pre = (props) => {
    let childrenWithProps = React.Children.map(props.children, child => {
        return React.cloneElement(child, { containedInPre: true })
    });

    return <pre>{childrenWithProps}</pre>
}

const components = {
    h1: H1,
    h2: H2,
    h3: H3,
    hr: Rule,
    pre: Pre,
    code: Code
}

const renderAst = new rehypeReact({
    createElement: React.createElement,
    components: components,
  }).Compiler


const Markdown = ({ source }) => (
    <div>
        {renderAst(source)}
        {/* <ReactMarkdown source={source} renderers={renderers} /> */}
    </div>
  )
  
  Markdown.propTypes = {
    source: PropTypes.object.isRequired,
  }
  
  export default Markdown