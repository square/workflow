import React from 'react'
import { Location } from '@reach/router';
import { Link as GatsbyLink } from 'gatsby'
import { StyleSheet, css } from 'aphrodite'
import { withPrefix } from 'gatsby'


const styles = StyleSheet.create({

    active: {
        fontWeight: "bold"
    }
  
  });


const Inner = ({location, to, children, exact}) => {


    let locationPathComponents = location.pathname.split("/").filter((val) => val)
    let toComponents = withPrefix(to).split("/").filter((val) => val)
    var linkStyles = []

    if (exact === false) {

        /// Support prefix
        if (toComponents.length <= locationPathComponents.length) {
            var allComponentsEqual = true
            for (var i=0; i<toComponents.length; i++) {
                if (locationPathComponents[i] !== toComponents[i]) {
                    allComponentsEqual = false
                }
            }
            
            if (allComponentsEqual) {
                linkStyles.push(styles.active)
            }
        }

    } else {

        /// Require equal path
        if (locationPathComponents.length === toComponents.length) {
            var equal = true
            for (var idx=0; idx<locationPathComponents.length; idx++) {
                if (locationPathComponents[idx] !== toComponents[idx]) {
                    equal = false
                }
            }
            
            if (equal) {
                linkStyles.push(styles.active)
            }
        }

    }



    return <GatsbyLink to={to} className={css(linkStyles)}>{children}</GatsbyLink>
}

const Link = (props) => {
    return <Location>
        {locationProps => {
            return <Inner location={locationProps.location} to={props.to} exact={props.exact}>
                {props.children}
            </Inner>
        }}
    </Location>
}

export default Link
