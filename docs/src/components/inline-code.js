import React from 'react'

import { StyleSheet, css } from 'aphrodite'

const styles = StyleSheet.create({
    inlineCode: {
        fontFamily: ["SFMono-Regular","Consolas","Liberation Mono","Menlo","Courier","monospace"],
        backgroundColor: "#eceef1",
        paddingLeft: "0.4em",
        paddingRight: "0.4em",
        paddingTop: "0.2em",
        paddingBottom: "0.2em",
        fontSize: "0.8em",
        borderRadius: "3pt"
    }
});

export default (props) => {
    return <code className={css(styles.inlineCode)}>{props.children}</code>
}