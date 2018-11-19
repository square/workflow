# Releasing workflow

## Development

To build and install the current version to your local Maven repository (`~/.m2`), run:

```
./gradlew clean installArchives
```

## Deploying

### Configuration

In order to deploy artifacts to a Maven repository, you'll need to set 4 properties in your
private Gradle properties file (`~/.gradle/gradle.properties`):

```
RELEASE_REPOSITORY_URL=<url of release repository>
SNAPSHOT_REPOSITORY_URL=<url of snapshot repository
SONATYPE_NEXUS_USERNAME=<username>
SONATYPE_NEXUS_PASSWORD=<password>
```

### Snapshot Releases

To deploy a new snapshot release, you don't need to update any properties. Just run:

```
./gradlew clean uploadArchives
```

### Production Releases

1. Make sure you're on the `master` branch.
2. In `gradle.properties`, remove the `-SNAPSHOT` prefix from the `VERSION_NAME` property.
   E.g. `VERSION_NAME=0.1.0`
3. Create a commit and tag the commit with the version number:
   ```
   git commit -am "Releasing version 0.1.0."
   git tag 0.1.0
   ```
4. Push your commit and tag:
   ```
   git push origin --tags
   ```
5. Upload the artifacts:
   ```
   ./gradlew clean uploadArchives
   ```
