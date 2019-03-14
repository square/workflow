const { createFilePath } = require(`gatsby-source-filesystem`)
const path = require(`path`)


exports.onCreateNode = ({ node, getNode, actions }) => {
  const { createNodeField } = actions
  if (node.internal.type === `MarkdownRemark`) {
    const slug = createFilePath({ node, getNode, basePath: `pages` })
    createNodeField({
      node,
      name: `slug`,
      value: slug,
    })
  }
}


exports.createPages = ({ graphql, actions }) => {
    const { createPage } = actions
    return createMarkdownPages(graphql, createPage)
}

const createMarkdownPages = (graphql, createPage) => {

    return new Promise((resolve, reject) => {
        graphql(`
        {
            allMarkdownRemark {
                edges {
                    node {
                        fields {
                            slug
                        }
                        frontmatter {
                            title
                            index
                        }
                    }
                }
            }
        }
        `).then(result => {
            result.data.allMarkdownRemark.edges.forEach(({ node }) => {
                createPage({
                    path: node.fields.slug,
                    component: path.resolve(`./src/components/markdown-page.js`),
                    context: {
                        // Data passed to context is available
                        // in page queries as GraphQL variables.
                        title: node.frontmatter.title,
                        index: node.frontmatter.index,
                        slug: node.fields.slug,
                    },
                })
            })
            resolve()
        })
    })

}
