import React from 'react'
import { StyleSheet, css } from 'aphrodite'

import squareLogo from '../images/square-logo.png'


const styles = StyleSheet.create({

  outerContainer: {
    backgroundColor: '#1b2126',
    marginBottom: '0',
    position: 'sticky',
    top: '0',
    color: "eceef1",
    marginTop: "96pt"
  },

    innerContainer: {
        margin: '0 auto',
        maxWidth: 1400,
        padding: '1.45rem 1.0875rem',
    },

    columnGrid: {
        display: "grid",
        gridTemplateColumns: "repeat(3, 1fr);",
        gridColumnGap: "24px",
        paddingTop: "24pt",
        paddingBottom: "48pt"
    },

    logo: {
        width: "32pt",
        height: "32pt"
    },

    contributorList: {
        listStyle: "none",
        margin: "0",
        padding: "0"
    }
});



const Footer = ({ siteTitle }) => (
  <div className={css(styles.outerContainer)} >
    <div className={css(styles.innerContainer)}>
        <div className={css(styles.columnGrid)}>
            <div>
                <img src={squareLogo} alt="logo" className={css(styles.logo)} />
            </div>
            <div>
                <ul className={css(styles.contributorList)}>
                    <li>© 2019 Square, Inc.</li>
                </ul>
            </div>
            <div>
                <ul className={css(styles.contributorList)}>
                    <li>David Apgar</li>
                    <li>Tim Donnelly</li>
                    <li>Zach Klippenstein</li>
                    <li>Ray Ryan</li>
                </ul>
            </div>
        </div>
    </div>
  </div>
)

export default Footer
