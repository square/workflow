Pod::Spec.new do |s|
  s.name         = 'WorkflowUI'
  s.version      = '0.22.0'
  s.summary      = 'Infrastructure for Workflow-powered UI'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git', :tag => "v#{s.version}" }

  # 1.7 is needed for `swift_versions` support
  s.cocoapods_version = '>= 1.7.0.beta.1'

  s.swift_versions = ['5.0']
  s.ios.deployment_target = '9.3'
  s.osx.deployment_target = '10.12'

  s.source_files = 'swift/WorkflowUI/Sources/**/*.swift'

  s.dependency 'Workflow', "#{s.version}"
 
  s.test_spec 'Tests' do |test_spec|
    test_spec.source_files = 'swift/WorkflowUI/Tests/**/*.swift'
    test_spec.framework = 'XCTest'
  end

end
