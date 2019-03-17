import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({
    container: {
        display: "grid",
        gridTemplateColumns: "repeat(3, 1fr);",
        gridColumnGap: "24px"
    }
});

const ThreeUp = ({ children }) => (
  <div className={css(styles.container)}>
    {children}
  </div>
)

export default ThreeUp
