import React from "react"
import Layout from "./layout"
import TwoColumn from './two-column'
import SectionNav from './section-nav'
import Markdown from './markdown'
import { graphql } from 'gatsby'


export default (props) => {
    let post = props.data.markdownRemark
    let nav = post.frontmatter.navigation

    let content = (
        <Markdown source={post.htmlAst} />
    )

    if (nav !== null && nav.visible === true) {

        return (
            <Layout>
                <TwoColumn 
                    body={content} 
                    nav={ <SectionNav path={nav.path} /> } 
                />
            </Layout>
        )

    } else {

        return (
            <Layout>
                {content}
            </Layout>
        )
        
    }


}
  
export const query = graphql`
    query($slug: String!) {
        markdownRemark(fields: { slug: { eq: $slug } }) {
            htmlAst
            frontmatter {
                navigation {
                    visible
                    path
                }
                title
            }
        }
    }
`