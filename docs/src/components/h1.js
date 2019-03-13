import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({

  pageTitle: {
    fontWeight: "bold",
    fontSize: "2em",
    marginTop: "0",
    marginBottom: "2em"
  }
});

const PageTitle = ({ children }) => (
  <h1 className={css(styles.pageTitle)}>
      {children}
  </h1>
)

export default PageTitle
