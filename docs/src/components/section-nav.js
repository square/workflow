import React from 'react'
import PropTypes from 'prop-types'
import { StyleSheet, css } from 'aphrodite'
import Link from './link'

import AllPages from './queries/all-pages'


const styles = StyleSheet.create({

  list: {
    margin: "0px",
    padding: "0px",
    listStyle: "none"
  },

  item: {
    paddingTop: "8px",
    paddingBottom: "8px"
  },

  activeItem: {
      fontWeight: "bold"
  },

  link: {
    paddingTop: "24pt",
    display: "block"
  },

  nestedList: {
      paddingLeft: "8pt",
      marginTop: "8pt",
      marginBottom: "8pt",
      borderLeft: "4px solid #eceef1"
  }

});

const makePathComponents =  path => {
    return path.split("/").filter((val) => val)
}

const parse = (path, pages) => {
    let result = _parse(path, pages, true)
    return result
}

const _parse = (path, pages, allowRoot) => {

    const pathComponents = makePathComponents(path)

    const filteredPages = pages.filter(page => {
        let pagePathComponents = makePathComponents(page.path)

        if (pagePathComponents.length < pathComponents.length) {
            return false
        }

        if (pagePathComponents.length === pathComponents.length && !allowRoot) {
            return false
        }

        if (pagePathComponents.length > pathComponents.length+1) {
            return false
        }

        for (var i = 0; i < pathComponents.length; i++) {
            if (pagePathComponents[i] !== pathComponents[i]) {
                return false
            }
        }

        return true
    })

    var linkData = []

    for (var page of filteredPages) {
        var navIndex = 999
        if (page.context.index !== null) {
            navIndex = page.context.index
        }

        var title = page.context.title
        if (title === undefined || title === null || title.length === 0) {
            title = page.path
        }

        var children = []
        if (makePathComponents(page.path).length !== pathComponents.length) {
            children = _parse(page.path, pages, false)
        }

        linkData.push({
            title: title,
            path: page.path,
            navIndex: navIndex,
            children: children
        })
    }



    linkData.sort((a,b) => {
        return a.navIndex - b.navIndex
    })

    return linkData
}

const List = ({linkData}) => {
    return <ul className={css(styles.list)}>
        {linkData.map(link => <ListItem key={link.path} link={link} />)}
    </ul>
}

const ListItem = ({link}) => {
    var nestedList = null

    if (link.children.length > 0) {
        nestedList = (
            <div className={css(styles.nestedList)}>
                <List linkData={link.children} />
            </div>
        )
    }


    return <li className={css(styles.item)} key={link.path}>
        <Link className={css(styles.link)} to={link.path}>{link.title}</Link>
        {nestedList}
    </li>
}

class Inner extends React.PureComponent {

    render() {
        let linkData = parse(this.props.path, this.props.pages, true)
        return (
            <List linkData={linkData} />
        )
    }
}


const SectionNav = ({ path }) => (
    <AllPages render={pages =>
        <Inner pages={pages} path={path} />
    } />
)

SectionNav.propTypes = {
  path: PropTypes.string.isRequired,
}

export default SectionNav
