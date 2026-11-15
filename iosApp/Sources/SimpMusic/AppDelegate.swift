import AVFoundation
import UIKit

/// UIKit entry point. The UI is the Compose Multiplatform root from the
/// `ComposeApp` framework, hosted by `SceneDelegate`.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        configureAudioSession()
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }

    /// Music playback: keep playing with the silent switch on and in the background
    /// (UIBackgroundModes = audio in Info.plist).
    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default, policy: .longFormAudio)
        } catch {
            NSLog("SimpMusic: AVAudioSession setCategory failed: \(error)")
        }
    }
}
