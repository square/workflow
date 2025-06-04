#!/bin/zsh
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
# It requires Python 3 to run.
# Install the packages with the following command:
# pip install -r requirements.txt
# Preview the site as you're editing it with:
# mkdocs serve
#
# Usage deploy_website.sh --kotlin-ref SHA_OR_REF_TO_DEPLOY
# Set the DRY_RUN environment variable to any non-null value to skip the actual deploy.
# A custom username/password can be used to authenticate to the git repo by setting
# the GIT_USERNAME and GIT_PASSWORD environment variables.
#
# E.g. to test the script: DRY_RUN=true ./deploy_website.sh --kotlin-ref main

# Automatically exit the script on error.
set -e

KOTLIN_REPO=square/workflow-kotlin

if [ -z "$WORKFLOW_GOOGLE_ANALYTICS_KEY" ]; then
    echo "Must set WORKFLOW_GOOGLE_ANALYTICS_KEY to deploy." >&2
    exit 1
fi

function getAuthenticatedRepoUrl() {
	if (( $# == 0 )); then echo "Must pass repo name, eg 'square/workflow'" >&2; exit 1; fi
	local repoName="$1"

	# Accept username/password overrides from environment variables for Github Actions.
	if [ -n "$GIT_USERNAME" -a -n "$GIT_PASSWORD" ]; then
		echo "Authenticating as $GIT_USERNAME." >&2
		gitCredentials="$GIT_USERNAME:$GIT_PASSWORD"
		echo "https://${gitCredentials}@github.com/$repoName.git"
	else
		echo "Authenticating as current user." >&2
		echo "git@github.com:$repoName.git"
	fi
}

function buildKotlinDocs() {
	local deployRef="$1"
	local targetDir="$2"
	local workingDir=deploy-kotlin

	if [[ -z "$deployRef" ]]; then echo "buildKotlinDocs: Must pass deploy ref as first arg" >&2; exit 1; fi
	if [[ -z "$targetDir" ]]; then echo "buildKotlinDocs: Must pass target dir as second arg" >&2; exit 1; fi

	if [[ -d "$workingDir" ]]; then
		echo "Removing old working directory $workingDir..."
		rm -rf "$workingDir"
	fi

	echo "Shallow-cloning $KOTLIN_REPO from $deployRef into $workingDir..."
	git clone --depth 1 --branch $deployRef $(getAuthenticatedRepoUrl $KOTLIN_REPO) $workingDir

	echo "Building Kotlin docs..."
	pushd $workingDir
	./gradlew assemble --build-cache --quiet
	./gradlew siteDokka --build-cache --quiet
	popd

	echo "Moving generated documentation to $targetDir..."
	# Clean the target dir first.
	[[ -d "$targetDir" ]] && rm -rf "$targetDir"
	mkdir -p "$targetDir"
	mv "$workingDir/build/dokka/htmlMultiModule" "$targetDir"

	echo "Removing working directory..."
	rm -rf "$workingDir"

	echo "Kotlin docs finished."
}

# Process arguments. See man zshmodules.
zparseopts -A refs -kotlin-ref:
KOTLIN_REF=${refs[--kotlin-ref]}
if [[ -z $KOTLIN_REF ]]; then
	echo "Missing --kotlin-ref argument" >&2
	exit 1
fi
echo "Deploying from $KOTLIN_REPO at $KOTLIN_REF"

echo "Building Kotlin docs…"
buildKotlinDocs $KOTLIN_REF "$(pwd)/docs/kotlin/api"

# Push the new files up to GitHub.
mkdocsMsg="Deployed docs using mkdocs {version} and script from {sha} from ${KOTLIN_REPO}@$KOTLIN_REF"
if [ -n "$DRY_RUN" ]; then
	echo "DRY_RUN enabled, building mkdocs but skipping gh-deploy and push…"
	mkdocs build
	echo "Would use commit message: $mkdocsMsg"
else
	echo "Running mkdocs gh-deploy --force…"
	# Build the site and force-push to the gh-pages branch.
	mkdocs gh-deploy --force --message "$mkdocsMsg"
fi

# Delete our temp folder.
echo "Deploy finished."
