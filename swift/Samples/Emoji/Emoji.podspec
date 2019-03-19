Pod::Spec.new do |s|
  s.name         = 'Emoji'
  s.version      = '1.0.0'
  s.summary      = 'Workflow Sample'
  s.homepage     = 'https://www.github.com/square/workflow'
  s.license      = 'Apache License, Version 2.0'
  s.author       = 'Square'
  s.source       = { :git => 'https://github.com/square/workflow.git', :tag => s.version }

  s.swift_version = '4.2'
  s.ios.deployment_target = '9.3'

  s.source_files = 'Sources/*.swift'

  s.dependency 'Workflow'
  s.dependency 'WorkflowUI'
  s.dependency 'BackStack'

end
