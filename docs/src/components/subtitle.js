import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({
    subtitle: {
    fontWeight: "regular",
    fontSize: "1.6em"
  }
});

const Subtitle = ({ children }) => (
  <p className={css(styles.subtitle)}>
      {children}
  </p>
)

export default Subtitle
