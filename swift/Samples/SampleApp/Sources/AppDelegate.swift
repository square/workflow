import UIKit
import WorkflowUI

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?


    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        
        window = UIWindow(frame: UIScreen.main.bounds)

        var viewRegistry = ViewRegistry()
        viewRegistry.registerDemoScreen()
        viewRegistry.registerWelcomeScreen()
        viewRegistry.registerCrossFadeContainer()
        window?.rootViewController = ContainerViewController(
            workflow: RootWorkflow(),
            viewRegistry: viewRegistry)

        window?.makeKeyAndVisible()
        
        return true
    }

}
