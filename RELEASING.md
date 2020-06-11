# Releasing workflow

## Production Releases

---

***Before you begin:*** *Please make sure you are set up with 
[`pod trunk`](https://guides.cocoapods.org/making/getting-setup-with-trunk.html) and your CocoaPods
account is a contributor to both the Workflow and WorkflowUI pods. If you need to be added as a
contributor, please [open a ticket requesting access](https://github.com/square/workflow/issues/new),
and assign it to @bencochran or @aquageek.*

---
1. Merge an update of [the change log](CHANGELOG.md) with the changes since the last release.

1. Make sure you're on the `trunk` branch (or fix branch, e.g. `v0.1-fixes`).

1. Create a commit and tag the commit with the version number:
   ```bash
   git commit -am "Releasing v0.1.0."
   git tag v0.1.0
   ```

1. Publish to CocoaPods:
    ```bash
    bundle exec pod trunk push Workflow.podspec
    bundle exec pod trunk push WorkflowTesting.podspec
    bundle exec pod trunk push WorkflowUI.podspec
    ```

1. Bump the version
   - **Swift:** Update `s.version` in `*.podspec` to the new version, e.g. `0.2.0`.

1. Commit the new snapshot version:
   ```
   git commit -am "Finish releasing v0.1.0."
   ```

1. Push your commits and tag:
   ```
   git push origin trunk
   # or git push origin fix-branch
   git push origin v0.1.0
   ```

1. Create the release on GitHub:
   1. Go to the [Releases](https://github.com/square/workflow/releases) page for the GitHub
      project.
   1. Click "Draft a new release".
   1. Enter the tag name you just pushed.
   1. Title the release with the same name as the tag.
   1. Copy & paste the changelog entry for this release into the description.
   1. If this is a pre-release version, check the pre-release box.
   1. Hit "Publish release".

1. If this was a fix release, merge changes to the trunk branch:
   ```bash
   git checkout trunk
   git pull
   git merge --no-ff v0.1-fixes
   # Resolve conflicts. Accept trunk's versions of gradle.properties and podspecs.
   git push origin trunk
   ```

1. Publish the website. See below.

## Deploying the documentation website

Official Workflow documentation lives at <https://squareup.github.io/workflow>. The website content
consists of three parts:

1. Markdown documentation: Lives in the `docs/` folder, and consists of a set of hand-written
   Markdown files that document high-level concepts. The static site generator
   [mkdocs](https://www.mkdocs.org/) (with [Material](https://squidfunk.github.io/mkdocs-material/)
   theming) is used to convert the Markdown to static, styled HTML.
1. Kotlin API reference: Kdoc embedded in Kotlin source files is converted to GitHub-flavored
   Markdown by Dokka and then included in the statically-generated website.
1. Swift API reference: Markup comments from Swift files are converted Markdown by
   [Sourcedocs](https://github.com/eneko/SourceDocs) and then included in the statically-generated
   website.

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

#### Swift: Sourcedocs

Sourcedocs generates a Markdown site from Swift files. You need Ruby, rubygems,
bundler (2.x), Xcode 10.2+, CocoaPods, and of course Sourcedocs itself, to run it. Assuming you've
already got Xcode, Ruby, and rubygems set up, install the rest of the dependencies:

```bash
gem install bundler cocoapods
brew install sourcedocs
```

If that succeeded, you need to generate an Xcode project before running Sourcedocs:

```bash
cd swift/Samples/SampleApp/
bundle exec pod install
# If this is your first time running CocoaPods, that will fail and you'll need to run this instead:
#bundle exec pod install --repo-update
```

You can manually generate the docs to verify everything is working correctly by running:

```bash
#cd swift/Samples/SampleApp/
sourcedocs generate -- -scheme Workflow -workspace SampleApp.xcworkspace
sourcedocs generate -- -scheme WorkflowUI -workspace SampleApp.xcworkspace
sourcedocs generate -- -scheme WorkflowTesting -workspace SampleApp.xcworkspace
```

Note that currently sourcedocs only supports Xcode 10, if you run it with Xcode 11 you might see
an error about Catalyst and only empty READMEs will get generated.

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
