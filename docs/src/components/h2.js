import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({
    h2: {
        fontWeight: "normal",
        fontSize: "1.6em",
        lineHeight: "1.5em",
        marginTop: "2em",
        marginBottom: "0.8em",
        color: "#888"
    }
});

const H2 = ({ children }) => (
  <h2 className={css(styles.h2)}>
      {children}
  </h2>
)

export default H2
