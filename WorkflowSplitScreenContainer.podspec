Pod::Spec.new do |s|
  s.name         = 'WorkflowSplitScreenContainer'
  s.version      = '0.23.0'
  s.summary      = 'See the README.'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git', :tag => "v#{s.version}" }

  # 1.7 is needed for `swift_versions` support
  s.cocoapods_version = '>= 1.7.0.beta.1'

  s.swift_versions = ['5.0']
  s.ios.deployment_target = '9.3'

  s.source_files = 'swift/WorkflowSplitScreenContainer/Sources/**/*.swift'

  s.dependency 'Workflow', "#{ENV['WORKFLOW_VERSION'] || s.version}"
  s.dependency 'WorkflowUI', "#{ENV['WORKFLOW_VERSION'] || s.version}"

  s.app_spec 'DemoApp' do |app_spec|
    app_spec.source_files = 'swift/WorkflowSplitScreenContainer/DemoApp/**/*.swift'
  end

  s.test_spec 'SnapshotTests' do |test_spec|
    test_spec.requires_app_host = true
    test_spec.source_files = 'swift/WorkflowSplitScreenContainer/SnapshotTests/**/*.swift'

    test_spec.framework = 'XCTest'

    test_spec.dependency 'iOSSnapshotTestCase'

    test_spec.scheme = { environment_variables: { 'FB_REFERENCE_IMAGE_DIR' => '$PODS_TARGET_SRCROOT/swift/WorkflowSplitScreenContainer/SnapshotTests/ReferenceImages' } }
  end

end
