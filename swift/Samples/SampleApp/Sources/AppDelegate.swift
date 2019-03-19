import UIKit
import Workflow
import WorkflowUI
import BackStack


fileprivate func makeRootViewController() -> UIViewController {
    var viewRegistry = ViewRegistry()
    viewRegistry.register(screenViewControllerType: SamplePickerViewController.self)
    viewRegistry.registerBackStackScreen()

    Sample.all.forEach { viewRegistry.merge(with: $0.viewRegistry)}

    let workflow = RootWorkflow()

    return ContainerViewController(
        workflow: workflow,
        viewRegistry: viewRegistry)
}


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = makeRootViewController()
        window?.makeKeyAndVisible()
        
        return true
    }

}
