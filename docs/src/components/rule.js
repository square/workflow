import React from 'react'
import { StyleSheet, css } from 'aphrodite'


const styles = StyleSheet.create({
    rule: {
        border: "none",
        backgroundColor: "#7a7e82",
        height: "1px",
        marginTop: "36pt",
        marginBottom: "36pt"
    }
});

const Rule = () => (
  <hr className={css(styles.rule)} />
)

export default Rule
