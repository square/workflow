# Releasing workflow

## Deploying the documentation website

Official Workflow documentation lives at <https://square.github.io/workflow>. The website content
consists of three parts:

1. Markdown documentation: Lives in the `docs/` folder, and consists of a set of hand-written
   Markdown files that document high-level concepts. The static site generator
   [mkdocs](https://www.mkdocs.org/) (with [Material](https://squidfunk.github.io/mkdocs-material/)
   theming) is used to convert the Markdown to static, styled HTML.
1. Kotlin API reference: Kdoc embedded in Kotlin source files is converted to GitHub-flavored
   Markdown by Dokka and then included in the statically-generated website.
1. Swift API reference: Markup comments from Swift files are converted Markdown by
   [DocC](https://www.swift.org/documentation/docc/) and then published independently at [square.github.io/workflow-swift/documentation](https://square.github.io/workflow-swift/documentation).

**Note: The documentation site is automatically built and deployed whenever a version tag is pushed.
You only need these steps if you want to work on the site locally.**

### Setting up the site generators

If you've already done this, you can skip to _Deploying the website to production_ below.

#### Kotlin: Dokka

Dokka runs as a Gradle plugin, so you need to be able to build the Kotlin source with Gradle, but
that's it. To generate the docs manually, run:

```bash
cd kotlin
./gradlew dokka
```

#### Swift: DocC

The Swift documentation is published by CI in the Swift repo and linked from the cross-platform Workflow docs. For info on how to generate the Swift docs locally, check out [the workflow-swift repo](https://github.com/square/workflow-swift).

#### mkdocs

Mkdocs is written in Python, so you'll need Python 3 and pip in order to run it. Assuming those are
set up, run:

```bash
pip install -r requirements.txt
```

Generate the site manually with:

```bash
mkdocs build
```

While you're working on the documentation files, you can run the site locally with:

```bash
mkdocs serve
```

### Deploying the website to production

**Note: The documentation site is automatically built and deployed by a Github Workflow whenever a
version tag is pushed. You only need these steps if you want to publish the site manually.**

Before deploying the website for real, you need to export our Google Analytics key in an environment
variable so that it will get added to the HTML. Get the key from one of the project maintainers,
then add the following to your `.bashrc` and re-source it:

```bash
export WORKFLOW_GOOGLE_ANALYTICS_KEY=UA-__________-1
```

Now you're ready to publish the site! Just choose a tag or SHA to deploy from, and run:

```bash
./deploy_website.sh TAG_OR_SHA
# For example:
#./deploy_website.sh v0.18.0
```

This will clone the repo to a temporary directory, checkout the right SHA, build Kotlin and Swift
API docs, generate HTML, and push the newly-generated content to the `gh-pages` branch on GitHub.

### Validating Markdown

Since all of our high-level documentation is written in Markdown, we run a linter in CI to ensure
we use consistent formatting. Lint errors will fail your PR builds, so to run locally, install
[markdownlint](https://github.com/markdownlint/markdownlint):

```bash
gem install mdl
```

Run the linter using the `lint_docs.sh`:

```bash
./lint_docs.sh
```

Rules can be configured by editing `.markdownlint.rb`.

---

## Kotlin Notes

### Development

To build and install the current version to your local Maven repository (`~/.m2`), run:

```bash
./gradlew clean installArchives
```

### Deploying

#### Configuration

In order to deploy artifacts to a Maven repository, you'll need to set 4 properties in your private
Gradle properties file (`~/.gradle/gradle.properties`):

```
RELEASE_REPOSITORY_URL=<url of release repository>
SNAPSHOT_REPOSITORY_URL=<url of snapshot repository
SONATYPE_NEXUS_USERNAME=<username>
SONATYPE_NEXUS_PASSWORD=<password>
```

#### Snapshot Releases

Double-check that `gradle.properties` correctly contains the `-SNAPSHOT` suffix, then upload
snapshot artifacts to Sonatype just like you would for a production release:

```bash
./gradlew clean build && ./gradlew uploadArchives --no-parallel --no-daemon
```

You can verify the artifacts are available by visiting
https://oss.sonatype.org/content/repositories/snapshots/com/squareup/workflow/.
