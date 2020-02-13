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

# This script uses SourceDocs.
# https://github.com/eneko/SourceDocs
# brew install sourcedocs
# It requires Xcode (minimum 10.2) to run.
#
# Usage: ./build_swift_docs.sh OUTPUT_DIR

SOURCEDOCS_OUTPUT_DIR="$1"
WORKFLOW_SCHEMES="Workflow WorkflowUI WorkflowTesting"

if [[ -z "$SOURCEDOCS_OUTPUT_DIR" ]]; then
	echo "No output dir specified. Usage: \`build_swift_docs.sh [OUTPUT_DIR]\`"
	exit 1
fi

set -ex

# Prepare the Xcode project.
bundle exec pod gen Development.podspec
cd gen/Development

# Generate the API docs.
for scheme in $WORKFLOW_SCHEMES; do
    sourcedocs generate \
        --output-folder "$SOURCEDOCS_OUTPUT_DIR/$scheme" \
        -- \
        -scheme $scheme \
        -workspace Development.xcworkspace
done
