module.exports = {
  pathPrefix: '/pages/~TDONNELLY/workflowdocs/master/browse',
  siteMetadata: {
    title: 'Workflow',
  },
  plugins: [
    'gatsby-plugin-react-helmet',
    {
      resolve: `gatsby-plugin-manifest`,
      options: {
        name: 'workflow-web',
        short_name: 'starter',
        start_url: '/',
        background_color: '#FFFFFF',
        theme_color: '#663399',
        display: 'minimal-ui',
        icon: 'src/images/square-icon.png', // This path is relative to the root of the site.
      },
    },
    {
      resolve: 'gatsby-source-filesystem',
      options: {
        name: 'markdown-pages',
        path: 'src/markdown/',
      },
    },
    {
      resolve: "gatsby-transformer-remark",
      options: {
        plugins: [
          "gatsby-remark-component",
          {
            resolve: `gatsby-remark-images`,
            options: {
                maxWidth: 1200,
            },
        }],
      },
    },
    'gatsby-plugin-offline',
    'gatsby-plugin-aphrodite',
    'gatsby-plugin-sharp'
  ],
}
