# iOS player bridge (frozen for Phase 2)

Status: **frozen 2026-11-16** (PLAN task 2.1). Change it only through a Fable review; both sides are
written against this text.

The iOS player is split in two:

- **Kotlin** (`core/data/iosMain`): `AvPlayerAdapter : MediaPlayerInterface`, a structural port of
  `MpvPlayerAdapter`. It owns the playlist, shuffle order, repeat, precache, crossfade timing,
  retries and every decision.
- **Swift** (`iosApp/Sources/SimpMusic/Player/`): `PlayerEngine`, which owns `AVPlayer` objects,
  the audio session and the lock screen. It does what it is told and reports what happened.

## 1. Why handles, not two slots

The PLAN sketch used two fixed slots. `MpvPlayerAdapter` actually holds up to four live players at
once: the current one, the one fading in, and up to two precached ones, and it *promotes* a
precached or fading-in player to current by keeping the same object. A handle maps onto `MpvPlayer`
one to one, so the port stays mechanical. A handle is an `Int` the engine hands out; it is never
reused inside one process.

Expected ceiling: 4 handles alive. The engine must not assume a limit.

## 2. Kotlin declarations

File: `core/domain/src/iosMain/kotlin/com/maxrave/domain/mediaservice/player/ios/NativePlayerEngine.kt`
(in `domain` so both `data` and `composeApp` see it). Primitive types, `String`, and
`Map<String, String>` only. No `suspend`, no generics of our own, no `Flow`, no default arguments.

```kotlin
package com.maxrave.domain.mediaservice.player.ios

interface NativePlayerEngine {
    // ----- lifecycle of one player -----
    /** New idle player. [video] = false means video tracks are never decoded or displayed. */
    fun create(video: Boolean): Int

    /**
     * Replace whatever [handle] holds with [url] (http(s):// or file://). Seeks to [startMs] before
     * the first frame. Starts playing as soon as it can when [autoPlay], otherwise buffers and
     * holds paused (this is what precache uses). [live] = an endless HLS playlist: no duration, no
     * onEnded.
     */
    fun load(handle: Int, url: String, headers: Map<String, String>, startMs: Long, autoPlay: Boolean, live: Boolean)

    fun play(handle: Int)
    fun pause(handle: Int)
    fun seek(handle: Int, positionMs: Long)

    /** 0f..1f, already the product of user volume, sleep fade and crossfade gain. */
    fun setVolume(handle: Int, volume: Float)

    /** Playback speed, pitch preserved. Applied now if playing, remembered for the next play(). */
    fun setRate(handle: Int, rate: Float)

    /** Stop, drop the item, forget the handle. Unknown handles are ignored. No event follows. */
    fun release(handle: Int)

    // ----- reads: callable from any thread, never block -----
    /** Extrapolated between observer ticks, so it is smooth enough for word-by-word lyrics. */
    fun positionMs(handle: Int): Long
    /** 0 until known, and always 0 for live. */
    fun durationMs(handle: Int): Long
    /** End of the loaded range that contains the playhead; 0 when unknown. */
    fun bufferedMs(handle: Int): Long

    // ----- system integration: one per app -----
    /**
     * The handle the user is listening to: its position/rate feed the lock screen, and (Phase 3)
     * its video is what the player video view shows. -1 = none. During a crossfade this is the
     * INCOMING handle, matching what the UI shows.
     */
    fun setActiveHandle(handle: Int)

    /** Lock screen / Control Centre text. The engine downloads [artworkUrl] itself; "" = none. */
    fun setNowPlaying(title: String, artist: String, album: String, artworkUrl: String, durationMs: Long, live: Boolean)

    /** Removes the lock-screen entry (queue cleared / stopped). */
    fun clearNowPlaying()

    /** Greys out next/previous on the lock screen. Play, pause, toggle and seek are always on. */
    fun setRemoteCommandsEnabled(next: Boolean, previous: Boolean)

    /** One listener; a second call replaces the first. Events arrive on the main thread. */
    fun setListener(listener: NativePlayerEngineListener)

    /** Release every handle and deactivate the audio session. */
    fun releaseAll()
}

interface NativePlayerEngineListener {
    /** The item can play. [durationMs] is 0 for live. Fires once per load(). */
    fun onReady(handle: Int, durationMs: Long)

    /** Sound is / is not actually coming out (AVPlayer.timeControlStatus == .playing). */
    fun onPlayingChanged(handle: Int, playing: Boolean)

    /** Wants to play but is waiting for data. Never true while paused. */
    fun onBufferingChanged(handle: Int, buffering: Boolean)

    /** Reached the end. Never fires for live, never after release(). */
    fun onEnded(handle: Int)

    /**
     * Load or playback failed; the handle is dead until the next load(). [httpStatus] is the HTTP
     * status when the failure was a server answer (403 = expired stream URL), otherwise 0.
     * [code] is the NSError code, [message] its description.
     */
    fun onError(handle: Int, httpStatus: Int, code: Int, message: String)

    /** Phase 3. Size of the video track once known. */
    fun onVideoSize(handle: Int, width: Int, height: Int)

    /** Phase 3. First video frame is on screen. */
    fun onFirstVideoFrame(handle: Int)

    /** Lock screen, Control Centre, headset, CarPlay-less car buttons. See REMOTE_* below. */
    fun onRemoteCommand(command: Int, positionMs: Long)

    /**
     * Another app or a call took the audio. On [began] the system has already paused us.
     * On end, [shouldResume] is the system's hint; Kotlin decides whether to call play().
     */
    fun onInterruption(began: Boolean, shouldResume: Boolean)

    /** Headphones unplugged / Bluetooth gone: the system has already paused us. */
    fun onOutputDeviceLost()
}

object NativePlayerConstants {
    const val REMOTE_PLAY = 1
    const val REMOTE_PAUSE = 2
    const val REMOTE_TOGGLE = 3
    const val REMOTE_NEXT = 4
    const val REMOTE_PREVIOUS = 5
    const val REMOTE_SEEK = 6        // positionMs carries the target
    const val NO_HANDLE = -1
}

/** Set once by Swift before MainViewController() is created; read by createMediaServiceHandler. */
object NativePlayerRegistry {
    var engine: NativePlayerEngine? = null
}
```

`composeApp/iosMain` adds the one function Swift calls, which also forces both interfaces into the
framework's Objective-C header:

```kotlin
fun registerNativePlayerEngine(engine: NativePlayerEngine) { NativePlayerRegistry.engine = engine }
```

The Swift names the header gives these types (plain or module-prefixed) are read from
`ComposeApp.framework/Headers/ComposeApp.h` after the first CI build, not assumed.

## 3. Rules both sides rely on

**Threads**
- Kotlin may call any engine method from any thread. The engine runs every mutating call on the
  main queue, in call order (`DispatchQueue.main.async`, or inline when already on main).
- `create` returns synchronously: the id comes from an atomic counter and the `AVPlayer` is built
  on main afterwards. Calls on that handle queue up behind the construction, so order holds.
- The three reads answer from a lock-protected snapshot, never by hopping to main.
- Every listener call is made on the main thread. The adapter immediately re-dispatches onto its
  own scope, exactly as `MpvPlayerAdapter` does with pump-thread events.

**Events**
- After `release(h)` no event for `h` is delivered. After `load(h, …)` no event from the previous
  item of `h` is delivered. The engine enforces this with a per-handle generation number; the
  adapter still checks "is this my current handle" as the mpv adapter does.
- `onPlayingChanged(h, false)` is not sent for the pause that `release` or `load` causes.
- Order for a normal track: `onBufferingChanged(true)`? → `onReady` → `onPlayingChanged(true)` →
  … → `onPlayingChanged(false)` → `onEnded`.

**Position**
- Snapshot = (media time, host time, rate, playing), refreshed by a 250 ms periodic time observer
  and on every seek completion, play, pause and stall. `positionMs` returns
  `media + (now − host) × rate` while playing, clamped to the duration, otherwise `media`.
- Right after `seek(h, p)` and until the seek completes, `positionMs(h)` returns `p`.

**Volume and crossfade**
- The engine never changes a volume on its own. Crossfade is the adapter stepping `setVolume` on
  two handles; sleep fade and ducking are multiplied in by the adapter before the call.
- iOS crossfade is a plain equal-power volume fade. The desktop DJ mode (filter sweeps, BPM speed
  matching, key shifting, AutoMix structure analysis) is not ported: on iOS the DJ and Auto
  settings behave as a fixed-length fade.

**Audio session**
- Category `.playback`, mode `.default`, policy `.longFormAudio`, set once at launch (already in
  `AppDelegate`). The engine activates the session on the first `play` and keeps it active while
  any handle exists and is playing or paused by the user.
- Interruptions and route loss: the engine reports them and does nothing else. No auto-resume in
  Swift.

**Lock screen**
- The engine owns `MPNowPlayingInfoCenter`. Kotlin supplies text, artwork URL and duration; the
  engine supplies elapsed time and rate from the active handle whenever they change.
- Remote commands are forwarded, never acted on in Swift. The handler returns `.success`
  immediately. `changePlaybackPosition` → `REMOTE_SEEK`. Skip-forward/backward intervals are
  disabled so the lock screen shows next/previous.

**Sources**
- One URL per load. A music video is played from a muxed source (HLS manifest or a progressive
  MP4), never from separate audio and video URLs: AVPlayer cannot merge two remote streams the way
  mpv's `edl://` does. Choosing that URL is task 2.5.
- Audio must be AAC/M4A, MP4 or HLS. The engine does not check; a WebM/Opus URL comes back as
  `onError`.
- `headers` go to `AVURLAsset` through `"AVURLAssetHTTPHeaderFieldsKey"`. Empty map = none.
- Local files are passed as `file://` URLs.

## 4. `MediaPlayerInterface` members on iOS

| Member | iOS behaviour |
|---|---|
| transport, queue, shuffle, repeat, timeline, listeners | Ported from `MpvPlayerAdapter` unchanged |
| `volume`, `sleepFadeFactor`, `crossfadeSuppressed`, `albumTrackIds` | Same as desktop |
| `playbackParameters` | Speed → `setRate`. Pitch is ignored (stays 1.0) |
| `skipSilenceEnabled` | Stored, no effect |
| `setEqualizer`, `setAudioEffects`, `setEightD` | Interface defaults (no-op); `eightDAvailable` stays false |
| `audioOutputs`, `selectAudioOutput`, `refreshAudioOutputs` | Current system route + an "AirPlay & Bluetooth" entry; any pick opens the system route picker (`AvAudioRoutes`) |
| `loadWaveform` | Interface default (`null`) |
| `audioSessionId` | 0 |
| `removeMediaItems` | Implemented as on desktop (the adapter owns the playlist, so the promise holds) |

## 5. Video views (Phase 3, task 3.1 — replaces the planned NativeViewFactory)

One engine addition; everything else is Kotlin (`composeApp/iosMain/.../expect/ui/`):

```kotlin
/** The AVPlayer behind [handle], or null (unknown/released handle, or not on main). Main thread only. */
fun nativePlayer(handle: Int): Any?
```

Swift: `PlayerEngine.player(handle:)` (`@MainActor`) and `KotlinPlayerBridge.nativePlayer(handle:)`.
Kotlin never changes the player's item, rate or volume; it only sets it on `AVPlayerLayer`s.

- **Which handle shows video**: `NativeVideoOutput.handle` (domain iosMain), published by
  `AvPlayerAdapter` after every serial task: the displayed handle (the incoming one during a
  crossfade) when it was created with `video = true`, else `NO_HANDLE` (views show the artwork).
- **Video source**: a track plays as video when it is a video item and the watch-video setting is
  on (as on desktop). On iOS `StreamRepositoryImpl` picks the muxed stream for video playback: the
  HLS manifest (pseudo-itag 96), else itag 18; that row is stored under the `VIDEO` key so it never
  overwrites the audio row. No muxed stream = the track plays as audio. Downloads are unchanged.
- **Main view**: one `AVPlayerLayer` per composable (`.resizeAspect`). It detaches the player on
  `didEnterBackground` (synchronously) and re-attaches on `willEnterForeground`, so audio keeps
  playing in the background; Picture in Picture keeps it attached. Size and first frame are read
  off the item/layer (`presentationSize`, `readyForDisplay`), so the engine still never sends
  `onVideoSize` / `onFirstVideoFrame`.
- **PiP**: the fullscreen player's view (`shouldPip = true`) owns an `AVPictureInPictureController`
  with `canStartPictureInPictureAutomaticallyFromInline`.
- **Canvas**: its own muted `AVQueuePlayer` + `AVPlayerLooper`, not an engine handle: never on the
  lock screen, no AirPlay, no display-sleep prevention, paused in the background.
- **Fullscreen**: `OrientationLock` (Kotlin) is read by the AppDelegate's
  `supportedInterfaceOrientationsFor` through `MainViewControllerKt.orientationLockMask()`;
  `requestGeometryUpdate` rotates.

## 6. Swift side

`PlayerEngine` is written with exactly the method names, parameter labels and types above
(`Int` → `Int32`, `Long` → `Int64`, `Float` → `Float`, `Boolean` → `Bool`,
`Map<String, String>` → `[String: String]`), so that once the framework carries the Kotlin
interfaces the conformance is a one-line extension plus a `registerNativePlayerEngine` call in
`AppDelegate`. Until then it talks to a Swift protocol `PlayerEngineListener` with the listener
methods above.
