Pod::Spec.new do |s|
  s.name         = 'Tutorial3'
  s.version      = '1.0.0.LOCAL'
  s.summary      = 'See the README.'
  s.homepage     = 'https://github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { git: 'Not Published', tag: "podify/#{s.version}" }

  # 1.7 is needed for `swift_versions` support
  s.cocoapods_version = '>= 1.7.0.beta.1'

  s.swift_versions = ['5.0']
  s.ios.deployment_target = '11.0'

  s.source_files = 'Sources/**/*.swift'
  s.resource_bundle = { 'TutorialResources' => ['Resources/**/*'] }

  s.dependency 'TutorialViews'
  s.dependency 'Workflow'
  s.dependency 'WorkflowUI'
  s.dependency 'BackStackContainer'
end
