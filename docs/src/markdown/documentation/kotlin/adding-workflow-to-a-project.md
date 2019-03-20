---
title: Adding Workflow to a project
index: 1
navigation:
    visible: true
    path: documentation
---

# Adding Workflow to a project

This document will guide you through the process of adding Workflow to an Gradle project.


## Libraries

For basic workflow usage, you'll need the following libraries:

```groovy
api "com.squareup.workflow:workflow-core:${workflowVersion}"

// Optional:
implementation "com.squareup.workflow:workflow-rx2:${workflowVersion}"
```

### Android

For showing Android views from your workflow, you'll need some additional libraries:

```groovy
implementation "com.squareup.workflow:viewregistry-android:${workflowVersion}"
```
