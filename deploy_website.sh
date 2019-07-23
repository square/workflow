#!/bin/bash
#
# Copyright 2019 Square Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material
# Preview the site as you're editing it with:
# mkdocs serve
# It also uses CocoaPods and Jazzy to build the Swift docs.
# See .buildscript/build_swift_docs.sh for setup info.
#
# Usage deploy_website.sh SHA_OR_REF_TO_DEPLOY

set -ex

# Get the actual SHA, even if a ref was passed.
SHA=$(git rev-parse $1)
REPO="git@github.com:square/workflow.git"
DIR=mkdocs-clone
SWIFT_API_DIR=swift/api
SWIFT_SCHEMES="Workflow WorkflowUI WorkflowTesting"

source .buildscript/build_swift_docs.sh

# Delete any existing temporary website clone.
rm -rf $DIR

# Clone the current repo into temp folder.
git clone $REPO $DIR

# Move working directory into temp folder.
pushd $DIR
git checkout $SHA

# Generate the Kotlin API docs.
(cd kotlin && ./gradlew dokka)

# Build the site
mkdocs gh-deploy
# Remove Dokka markdown.
git clean -fdx
git checkout gh-pages

# Generate the Swift API docs.
# Clone local repo because this script doesn't need to push.
build_swift_docs $SHA ".git" $SWIFT_API_DIR "$SWIFT_SCHEMES" && \
    git add $SWIFT_API_DIR && \
    git commit -m "Deployed Swift docs from $SHA"

# Push the new files up to GitHub.
git push origin gh-pages:gh-pages

# Delete our temp folder.
popd
rm -rf $DIR
