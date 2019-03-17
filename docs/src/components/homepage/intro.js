import React from 'react'
import { StyleSheet, css } from 'aphrodite'

import Subtitle from '../subtitle'


const styles = StyleSheet.create({

  bigTitle: {
    fontWeight: "bold",
    fontSize: "6em"
  }
});

const PageTitle = ({ children }) => (
  <div>
    <h1 className={css(styles.bigTitle)}>
      Workflow
    </h1>
    <Subtitle>A reactive application architecture for Kotlin and Swift</Subtitle>
  </div>

)

export default PageTitle
