# Releasing workflow

## Production Releases

1. Make sure you're on the `master` branch.
2. In `kotlin/gradle.properties`, remove the `-SNAPSHOT` prefix from the `VERSION_NAME` property.
   E.g. `VERSION_NAME=0.1.0`
   Update `swift/CHANGELOG.md` and `kotlin/CHANGELOG.md` with the changes since the last release.
3. Create a commit and tag the commit with the version number:
   ```
   git commit -am "Releasing v0.1.0."
   git tag v0.1.0
   ```
4. Upload the artifacts:
   ```
   ./gradlew clean uploadArchives
   ```
5. Bump the version
  - **Kotlin:** Update the `VERSION_NAME` property in `kotlin/gradle.properties` to the new snapshot 
    version, e.g. `VERSION_NAME=0.2.0-SNAPSHOT`.
  - **Swift:** Update `s.version` in `Workflow.podspec` to the new version, e.g. `0.2.0`.
6. Commit the new snapshot version:
   ```
   git commit -am "Finish releasing v0.1.0."
   ```
7. Push your commits and tag:
   ```
   git push origin master && git push origin v0.1.0
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