#!/usr/bin/swift 

import Foundation

let contents = try! String(contentsOfFile: "CHANGELOG.md", encoding: .utf8)

var lines = contents.components(separatedBy: "\n")

let workflowVersion = ProcessInfo.processInfo.environment["WORKFLOW_VERSION"]!
let kotlinChangelog = ProcessInfo.processInfo.environment["KOTLIN_CHANGELOG"]!
let swiftChangelog =  ProcessInfo.processInfo.environment["SWIFT_CHANGELOG"]!

let dateFormatter = DateFormatter()
dateFormatter.dateFormat = "YYYY-MM-dd"

let newChangelog = """

## Version \(workflowVersion)

_\(dateFormatter.string(from: Date()))_

### Kotlin

\(kotlinChangelog)

### Swift

\(swiftChangelog)
"""

lines.insert(newChangelog, at: 2)

try! lines.joined(separator: "\n").write(toFile: "CHANGELOG.md", atomically: true, encoding: .utf8)
