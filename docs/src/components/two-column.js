import React from 'react'
import PropTypes from 'prop-types'
import { StyleSheet, css } from 'aphrodite'

const styles = StyleSheet.create({
  container: {
    display: "grid",
    gridTemplateColumns: "2fr 1fr",
    gridColumnGap: "24px"
  },
  column: {
      minWidth: "0"
  }
});


const TwoColumn = ({ body, nav }) => (
    <div className={css(styles.container)}>
        <div className={css(styles.column)}>
            {body}
        </div>
        <div className={css(styles.column)}>
            {nav}
        </div>
    </div>
)

TwoColumn.propTypes = {
  body: PropTypes.node.isRequired,
  nav: PropTypes.node.isRequired
}

export default TwoColumn
