Pod::Spec.new do |s|
  s.name         = 'TabBarContainer'
  s.version      = '1.0.0.LOCAL'
  s.summary      = 'See the README.'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { git: 'Not Published', tag: "podify/#{s.version}" }

  # 1.7 is needed for `swift_versions` support
  s.cocoapods_version = '>= 1.7.0'

  s.swift_versions = ['5.0']
  s.ios.deployment_target = '10.0'

  s.source_files = 'Sources/**/*.swift'

  s.dependency 'Workflow'
  s.dependency 'WorkflowUI'

  s.app_spec 'DemoApp' do |app_spec|
    app_spec.source_files = 'DemoApp/**/*.swift'
  end

  s.test_spec 'SnapshotTests' do |test_spec|
    test_spec.requires_app_host = true
    test_spec.source_files = 'SnapshotTests/**/*.swift'

    test_spec.framework = 'XCTest'

    test_spec.dependency 'iOSSnapshotTestCase'

    test_spec.scheme = { 
      environment_variables: { 
        'FB_REFERENCE_IMAGE_DIR' => '$PODS_TARGET_SRCROOT/SnapshotTests/ReferenceImages',
        'IMAGE_DIFF_DIR' => '$PODS_TARGET_SRCROOT/SnapshotTests/FailureDiffs'
      } 
    }
  end
end
