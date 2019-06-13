# Releasing workflow

## Production Releases

---

***Before you begin:*** *Please make sure you are set up with [`pod trunk`](https://guides.cocoapods.org/making/getting-setup-with-trunk.html) and your CocoaPods account is a contributor to both the Workflow and WorkflowUI pods. If you need to be added as a contributor, please [open a ticket requesting access](https://github.com/square/workflow/issues/new), and assign it to @apgar or @timdonnelly.*

---

1. Merge an update of [the change log](CHANGELOG.md) with the changes since the last release.

1. Make sure you're on the `master` branch (or fix branch, e.g. `v0.1-fixes`).

1. Confirm that the kotlin build is green before committing any changes
   ```
   (cd kotlin && ./gradlew build connectedCheck)
   ```

2. In `kotlin/gradle.properties`, remove the `-SNAPSHOT` prefix from the `VERSION_NAME` property.
   E.g. `VERSION_NAME=0.1.0`

3. Create a commit and tag the commit with the version number:
   ```
   git commit -am "Releasing v0.1.0."
   git tag v0.1.0
   ```

4. Upload the kotlin artifacts:
   ```
   (cd kotlin && ./gradlew clean uploadArchives)
   ```

5. Publish to CocoaPods:
    ```
    bundle exec pod trunk push Workflow.podspec
    bundle exec pod trunk push WorkflowUI.podspec
    ```

6. Bump the version
  - **Kotlin:** Update the `VERSION_NAME` property in `kotlin/gradle.properties` to the new snapshot 
    version, e.g. `VERSION_NAME=0.2.0-SNAPSHOT`.
  - **Swift:** Update `s.version` in `*.podspec` to the new version, e.g. `0.2.0`.

7. Commit the new snapshot version:
   ```
   git commit -am "Finish releasing v0.1.0."
   ```

8. Push your commits and tag:
   ```
   git push origin master
   # or git push origin fix-branch
   git push origin v0.1.0
   ```

9. Create the release on GitHub:
     1. Go to the [Releases](https://github.com/square/workflow/releases) page for the GitHub project.
     2. Click "Draft a new release".
     3. Enter the tag name you just pushed.
     4. Title the release with the same name as the tag.
     5. Copy & paste the changelog entry for this release into the description.
     6. If this is a pre-release version, check the pre-release box.
     7. Hit "Publish release".

10. If this was a fix release, merge changes to the master branch:
   ```
   git checkout master
   git reset --hard origin/master
   git merge --no-ff v0.1-fixes
   # Resolve conflicts. Accept master's versions of gradle.properties and podspecs.
   git push origin master
   ```

---

## Kotlin Notes

### Development

To build and install the current version to your local Maven repository (`~/.m2`), run:

```
./gradlew clean installArchives
```

### Deploying

#### Configuration

In order to deploy artifacts to a Maven repository, you'll need to set 4 properties in your
private Gradle properties file (`~/.gradle/gradle.properties`):

```
RELEASE_REPOSITORY_URL=<url of release repository>
SNAPSHOT_REPOSITORY_URL=<url of snapshot repository
SONATYPE_NEXUS_USERNAME=<username>
SONATYPE_NEXUS_PASSWORD=<password>
```

#### Snapshot Releases

To deploy a new snapshot release, you don't need to update any properties. Just run:

```
./gradlew clean uploadArchives
```
