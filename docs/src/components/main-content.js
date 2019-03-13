import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({

  outerContainer: {
    marginBottom: '1.45rem',
  },

  innerContainer: {
    margin: '0 auto',
    maxWidth: 1400,
    padding: '1.45rem 1.0875rem'
  }
});

const MainContainer = (props) => (
  <div className={css(styles.outerContainer)} >
    <div className={css(styles.innerContainer)}>
        {props.children}
    </div>
  </div>
)

export default MainContainer
