Pod::Spec.new do |s|
  s.name         = 'Development'
  s.version      = '0.1.0'
  s.summary      = 'Infrastructure for Workflow-powered UI'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git', :tag => "v#{s.version}" }

  s.ios.deployment_target = '11.0'
  s.swift_version = '5.0'
  s.dependency 'Workflow'
  s.dependency 'WorkflowUI'
  s.source_files = 'swift/Samples/Dummy.swift'

  s.subspec 'Dummy' do |ss|
  end

  s.default_subspecs = 'Dummy'

  dir = Pathname.new(__FILE__).dirname
  snapshot_test_env = {
    'IMAGE_DIFF_DIR' => dir.join('swift/FailureDiffs'),
    'FB_REFERENCE_IMAGE_DIR' => dir.join('swift/Samples/SnapshotTests/ReferenceImages'),
  }

  s.scheme = { 
    environment_variables: snapshot_test_env
  }

  s.app_spec 'SampleApp' do |app_spec|
    app_spec.source_files = 'swift/Samples/SampleApp/Sources/**/*.swift'
    app_spec.resources = 'swift/Samples/SampleApp/Resources/**/*.swift'
  end

  s.test_spec 'WorkflowTesting' do |test_spec|
    test_spec.requires_app_host = true
    test_spec.dependency 'WorkflowTesting'
    test_spec.source_files = 'swift/WorkflowTesting/Tests/**/*.swift'
  end

  # TODO: Disabled because app specs cannot increase the deployment target of the root
  # To use, increase the deployment target of this spec to 13.0 or higher
  #
  # s.app_spec 'SampleSwiftUIApp' do |app_spec|
  #   app_spec.ios.deployment_target = '13.0'
  #   app_spec.dependency 'WorkflowSwiftUI'
  #   app_spec.pod_target_xcconfig = {
  #     'IFNFOPLIST_FILE' => '${PODS_ROOT}/../swift/Samples/SampleSwiftUIApp/SampleSwiftUIApp/Configuration/Info.plist'
  #   }
  #   app_spec.source_files = 'SampleSwiftUIApp/SampleSwiftUIApp/**/*.swift'
  # end

  s.app_spec 'SampleTicTacToe' do |app_spec|
    app_spec.source_files = 'swift/Samples/TicTacToe/Sources/**/*.swift'
    app_spec.resources = 'swift/Samples/TicTacToe/Resources/**/*'
    app_spec.dependency 'BackStackContainer'
    app_spec.dependency 'ModalContainer'
  end

  s.test_spec 'TicTacToeTests' do |test_spec|
    test_spec.dependency 'Development/SampleTicTacToe'
    test_spec.requires_app_host = true
    test_spec.app_host_name = 'Development/SampleTicTacToe'
    test_spec.source_files = 'swift/Samples/TicTacToe/Tests/**/*.swift'
  end

  s.app_spec 'SampleSplitScreen' do |app_spec|
    app_spec.dependency 'SplitScreenContainer'
    app_spec.source_files = 'swift/Samples/SplitScreenContainer/DemoApp/**/*.swift'

    app_spec.scheme = {
      environment_variables: snapshot_test_env
    }
  end

  s.test_spec 'SplitScreenTests' do |test_spec|
    test_spec.dependency 'SplitScreenContainer'
    test_spec.dependency 'Development/SampleSplitScreen'
    test_spec.app_host_name = 'Development/SampleSplitScreen'
    test_spec.requires_app_host = true
    test_spec.source_files = 'swift/Samples/SplitScreenContainer/SnapshotTests/**/*.swift'

    test_spec.framework = 'XCTest'

    test_spec.dependency 'iOSSnapshotTestCase'

    test_spec.scheme = { 
      environment_variables: snapshot_test_env
    }
  end

  s.app_spec 'SampleTabBarScreen' do |app_spec|
    app_spec.dependency 'TabBarContainer'
    app_spec.source_files = 'swift/Samples/TabBarContainer/DemoApp/**/*.swift'

    app_spec.scheme = {
      environment_variables: snapshot_test_env
    }
  end

  s.test_spec 'TabBarScreenTests' do |test_spec|
    test_spec.dependency 'TabBarContainer'
    test_spec.dependency 'Development/SampleTabBarScreen'
    test_spec.app_host_name = 'Development/SampleTabBarScreen'
    test_spec.requires_app_host = true
    test_spec.source_files = 'swift/Samples/TabBarContainer/SnapshotTests/**/*.swift'

    test_spec.framework = 'XCTest'

    test_spec.dependency 'iOSSnapshotTestCase'

    test_spec.scheme = { 
      environment_variables: snapshot_test_env
    }
  end

  s.test_spec 'WorkflowTests' do |test_spec|
    test_spec.requires_app_host = true
    test_spec.source_files = 'swift/Workflow/Tests/**/*.swift'
    test_spec.framework = 'XCTest'
  end

  s.test_spec 'WorkflowUITests' do |test_spec|
    test_spec.requires_app_host = true
    test_spec.source_files = 'swift/WorkflowUI/Tests/**/*.swift'
    test_spec.framework = 'XCTest'
  end
end
