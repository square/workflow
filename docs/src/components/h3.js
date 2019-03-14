import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({
    h3: {
        fontWeight: "normal",
        fontSize: "1.4em",
        lineHeight: "1.3em",
        marginTop: "2em",
        marginBottom: "0.6em",
        color: "#888"
    }
});

const H3 = ({ children }) => (
  <h3 className={css(styles.h3)}>
      {children}
  </h3>
)

export default H3
