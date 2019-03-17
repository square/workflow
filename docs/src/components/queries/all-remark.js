import React from 'react'
import PropTypes from 'prop-types'
import { StaticQuery, graphql } from 'gatsby'


const AllRemark = ({ render }) => (

  <StaticQuery
    query={graphql`
        {
            allMarkdownRemark {
                edges {
                    node {
                        rawMarkdownBody
                        fields {
                            slug
                        }
                        headings {
                            value
                        }
                        frontmatter {
                            title
                            index
                        }
                    }
                }
            }
        }
    `}
    render={data => {
        const documents = data.allMarkdownRemark.edges.map(({node}) => node)
        return render(documents)
    }}
  />
)

AllRemark.propTypes = {
  render: PropTypes.func.isRequired,
}

export default AllRemark
