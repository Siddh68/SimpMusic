import ComposeApp
import UIKit

/// Hosts the Compose root in a full-screen window. The root view controller's view
/// fills the window including the status bar and home indicator areas, so Compose
/// draws edge-to-edge and applies safe-area insets itself (WindowInsets).
///
/// `@objc(SceneDelegate)` gives the class a stable Objective-C name so the
/// Info.plist `UISceneDelegateClassName` can be the plain string "SceneDelegate".
@objc(SceneDelegate)
final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = MainViewControllerKt.MainViewController()
        window.makeKeyAndVisible()
        self.window = window
    }
}
