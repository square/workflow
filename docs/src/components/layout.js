import React from 'react'
import PropTypes from 'prop-types'
import Helmet from 'react-helmet'
import { StaticQuery, graphql } from 'gatsby'
import { StyleSheet, css } from 'aphrodite'

import Footer from './footer'
import Header from './header'
import MainContent from './main-content'

const styles = StyleSheet.create({

  html: {
    margin: "0px",
    padding: "0px"
  },

  body: {
    fontFamily: ["Square Market", "Helvetica", "Arial", "sans-serif"],
    color: "#3e4348",
    lineHeight: "1.6em",
    margin: "0px",
    padding: "0px"
  },

  mainColumn: {
    display: "flex",
    flexDirection: "column",
    minHeight: "100vh",
    alignItems: "stretch"
  },

  growContainer: {
    flexGrow: "1"
  }
});


const Layout = ({ children }) => (
  <StaticQuery
    query={graphql`
      query SiteTitleQuery {
        site {
          siteMetadata {
            title
          }
        }
      }
    `}
    render={data => (
      <>
        <Helmet
          title={data.site.siteMetadata.title}
          meta={[
            { name: 'description', content: 'Sample' },
            { name: 'keywords', content: 'sample, something' },
          ]}
        >
          <html lang="en" className={css(styles.html)} />
          <body className={css(styles.body)} />
          <link rel="stylesheet" href="https://d1g145x70srn7h.cloudfront.net/fonts/sqmarket/sq-market.css" />
 
          <style type="text/css">{`
            a {
              color: #49a4d5;
              text-decoration: none;
            }
            a:visited {
              color: #5174d1;
            }
            a:active, a:hover {
              text-decoration: underline;
            }
          `}</style>
        </Helmet>

        <div className={css(styles.mainColumn)}>
          <Header siteTitle={data.site.siteMetadata.title} />
          <div className={css(styles.growContainer)}>
            <MainContent>
              {children}
            </MainContent>
          </div>
          <Footer />
        </div>

      </>
    )}
  />
)

Layout.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Layout
