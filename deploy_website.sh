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

set -ex

REPO="git@github.com:square/workflow.git"
DIR=temp-clone

# Delete any existing temporary website clone.
rm -rf $DIR

# Clone the current repo into temp folder.
git clone $REPO $DIR

# Move working directory into temp folder.
cd $DIR

# Build the site and push the new files up to GitHub.
mkdocs gh-deploy
git checkout gh-pages
git push

# Delete our temp folder.
cd ..
rm -rf $DIR
