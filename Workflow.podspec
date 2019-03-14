Pod::Spec.new do |s|
  s.name         = 'Workflow'
  s.version      = '0.1.0'
  s.summary      = 'Reactive application architecture'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git' }

  s.swift_versions = ['4.2']
  s.ios.deployment_target = '9.3'

  s.source_files = 'swift/Workflow/Sources/*.swift'

  s.dependency 'ReactiveSwift'
  s.dependency 'Result'
 
  s.test_spec 'Tests' do |test_spec|
    test_spec.source_files = 'swift/Workflow/Tests/**/*.swift'
    test_spec.framework = 'XCTest'
  end

end
