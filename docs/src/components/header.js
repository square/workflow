import React from 'react'
import Link from './link'
import { StyleSheet, css } from 'aphrodite'
import { Link as GatsbyLink } from 'gatsby'


const styles = StyleSheet.create({

  outerContainer: {
    backgroundColor: '#FFFFFF',
    marginBottom: '1.45rem',
    position: 'sticky',
    top: '0'
  },

  innerContainer: {
    margin: '0 auto',
    maxWidth: 1400,
    padding: '1.45rem 1.0875rem',
    display: "flex",
    flexDirection: "row",
    alignItems: "center"
  },

  title: {
    margin: 0,
    display: "block",
    fontWeight: "medium",
    fontSize: "1.2em",
    // textTransform: "uppercase",
    letterSpacing: "0.05em"
  },

  link: {
    textDecoration: 'none',
    color: "#3e4348"
  },

  sectionLink: {
    display: "block",
    marginLeft: "36px"
  }
});

const Header = ({ siteTitle }) => (
  <div className={css(styles.outerContainer)} >
    <div className={css(styles.innerContainer)}>
      <h1 className={css(styles.title)}>
        <GatsbyLink to="/" className={css(styles.link)}>
          {siteTitle}
        </GatsbyLink>
      </h1>
      <span className={css(styles.sectionLink)}>
        <Link to="/documentation" exact={false}>Documentation</Link>
      </span>
    </div>
  </div>
)

export default Header
