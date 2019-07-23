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

# This script uses Jazzy.
# https://github.com/realm/jazzy
# It requires ruby, bundler (2.x), CocoaPods, and Jazzy to build.
# gem install bundler jazzy

# Usage: build_swift_docs SHA REPO_URL_OR_PATH OUTPUT_DIR SPACE_SEPARATED_SCHEMES
function build_swift_docs() {
    local SHA=$1
    local REPO="$2"
    local OUTPUT_DIR="$(realpath "$3")"
    local SCHEMES="$4"
    local DIR=swiftdocs-clone
    local GA_HEAD=$(cat <<-END_HEREDOC
<!-- Global site tag (gtag.js) - Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=$WORKFLOW_GOOGLE_ANALYTICS_KEY"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', '$WORKFLOW_GOOGLE_ANALYTICS_KEY');
</script>
END_HEREDOC
)

    # Delete any existing temporary website clone.
    rm -rf $DIR

    # Clone the current repo into temp folder.
    git clone $REPO $DIR

    # Move working directory into temp folder.
    pushd $DIR
    git checkout $SHA

    # Generate the API docs.
    cd swift/Samples/SampleApp/
    bundle exec pod install
    for scheme in $SCHEMES; do
        jazzy \
            --xcodebuild-arguments "-scheme,$scheme,-workspace,SampleApp.xcworkspace" \
            --author Square \
            --author_url https://developer.squareup.com \
            --github_url https://github.com/square/workflow \
            --head "$GA_HEAD" \
            --output "$OUTPUT_DIR/$scheme"
    done

    # Delete our temp folder.
    popd
    rm -rf $DIR
}
