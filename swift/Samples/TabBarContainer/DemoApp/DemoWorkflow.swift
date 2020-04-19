/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Workflow
import WorkflowUI
import TabBarContainer


// MARK: Input and Output

struct DemoWorkflow: Workflow {

    typealias Output = Never

}


// MARK: State and Initialization

extension DemoWorkflow {

    enum State {
        case baz
        case foo
    }

    func makeInitialState() -> State {
        return .baz
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {
    }

}

// MARK: Actions

extension DemoWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = DemoWorkflow
        
        case bazTabSelected
        case fooTabSelected
        
        func apply(toState state: inout DemoWorkflow.State) -> Never? {
            switch self {
            case .bazTabSelected:
                state = .baz
            case .fooTabSelected:
                state = .foo
            }
            
            return nil
        }
    }
}


// MARK: Rendering

extension DemoWorkflow {

    typealias Rendering = TabBarScreen<AnyScreen>

    func render(state: State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        
        let selectedIndex: Int
        let currentScreen: AnyScreen
    
        switch state {
        case .baz:
            let bazScreen = BazWorkflow()
                .rendered(with: context)
            
            currentScreen = AnyScreen(bazScreen)
            selectedIndex =  0
        case .foo:
            let fooScreen = FooWorkflow()
                .rendered(with: context)
            
            currentScreen = AnyScreen(fooScreen)
            selectedIndex = 1
        }
        
        let bazBadgeValue = BazBadgeWorkflow()
            .mapRendering({ (rendering) -> BarItem.Badge in
                guard let badgeValue = rendering else {
                    return .none
                }
                
                return .text(badgeValue)
            })
            .rendered(with: context)
        
        let bazBarImage: UIImage
        if #available(iOS 13.0, *) {
            bazBarImage = UIImage(systemName: "triangle") ?? UIImage()
        } else {
            bazBarImage = UIImage()
        }
        
        let bazBarItem = BarItem(
            title: "Baz",
            image: bazBarImage,
            badge: bazBadgeValue,
            onSelect: { sink.send(.bazTabSelected) }
        )
        
        let fooBarImage: UIImage
        if #available(iOS 13.0, *) {
            fooBarImage = UIImage(systemName: "square") ?? UIImage()
        } else {
            fooBarImage = UIImage()
        }
        
        let fooBarItem = BarItem(
            title: "Foo",
            image: fooBarImage,
            badge: .none,
            onSelect: { sink.send(.fooTabSelected) }
        )
        
        return TabBarScreen(
            currentScreen: currentScreen,
            barItems: [bazBarItem, fooBarItem],
            selectedIndex: selectedIndex
        )
    }

}
