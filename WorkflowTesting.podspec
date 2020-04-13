Pod::Spec.new do |s|
  s.name         = 'WorkflowTesting'
  s.version      = '0.26.0'
  s.summary      = 'Reactive application architecture'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git', :tag => "v#{s.version}" }

  # 1.7 is needed for `swift_versions` support
  s.cocoapods_version = '>= 1.7.0.beta.1'

  s.swift_versions = ['5.0']
  s.ios.deployment_target = '9.3'
  s.osx.deployment_target = '10.12'

  s.source_files = 'swift/WorkflowTesting/Sources/*.swift'

  s.dependency 'ReactiveSwift', '~> 6.0.0'
  s.dependency 'Workflow', "#{s.version}"
  s.framework = 'XCTest'

  s.test_spec 'Tests' do |test_spec|
    test_spec.source_files = 'swift/WorkflowTesting/Tests/**/*.swift'
    test_spec.framework = 'XCTest'
    test_spec.library = 'swiftDispatch'
    test_spec.library = 'swiftFoundation'
  end

end
