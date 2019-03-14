import React from 'react'
import PropTypes from 'prop-types'
import { StaticQuery, graphql } from 'gatsby'


const AllPages = ({ render }) => (

  <StaticQuery
    query={graphql`
        {
            allSitePage {
                edges {
                    node {
                        componentPath
                        path
                        jsonName
                        context {
                            title
                            index
                        }
                    }
                }
            }
        }
    `}
    render={data => {
        const pages = data.allSitePage.edges.map(({node}) => node)
        return render(pages)
    }}
  />
)

AllPages.propTypes = {
  render: PropTypes.func.isRequired,
}

export default AllPages
