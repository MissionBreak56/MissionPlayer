# MissionPlayer

An **original**, offline, ad-free local music player for Android — a fresh
implementation inspired (at the feature level only) by the audio-player feature
set found in the `Lark Player` package you provided for analysis.

> No code, resources, strings, icons or branding were copied from that package.
> Everything here is new. Package name: `com.missionbreak56.musicplayer`.

---

## 1. What was analyzed (behaviour only)

From `AndroidManifest.decoded.xml` (`com.dywx.larkplayer`), the relevant feature
surface was identified and re-implemented from scratch:

| Original capability | Evidence | Implemented here |
|---|---|---|
| Local audio library | `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` | `MusicRepository` (MediaStore scan) |
| Background playback | `foregroundServiceType="mediaPlayback"`, `MEDIA_BUTTON` | `PlaybackService` (Media3 `MediaSessionService`) |
| System media controls | `MediaSessionService` intent-filter | Media3 `MediaSession` + notification |
| Search / browse | `android.app.searchable` | Search field in `MainActivity` |
| Favorites / playlists | (app data) | `Prefs` (SharedPreferences) |
| About screen | (app) | `AboutActivity` (author = MissionBreak56) |

Explicitly **not** carried over: ads, analytics, network, Google Drive,
accounts/login, feedback, video player, widgets, deeplinks, equalizer.

---

## 2. Project structure

```
MissionPlayer/
├── settings.gradle.kts
├── build.gradle.kts                      # root: plugin versions
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── .gitignore
├── .github/workflows/build-apk.yml       # CI that produces the ARM64 APK
├── README.md
└── app/
    ├── build.gradle.kts                  # app module + ABI splits (arm64-v8a)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/missionbreak56/musicplayer/
        │   ├── App.kt                    # first-run premium=true
        │   ├── data/
        │   │   ├── Prefs.kt              # premium, favorites, playlists
        │   │   ├── Song.kt
        │   │   └── MusicRepository.kt    # MediaStore (offline)
        │   ├── playback/
        │   │   ├── PlaybackService.kt    # Media3 session service
        │   │   └── PlayerConnection.kt    # MediaController helper
        │   └── ui/
        │       ├── MainActivity.kt        # library, search, mini-player
        │       ├── SongAdapter.kt
        │       ├── PlayerActivity.kt      # now playing + favorite
        │       └── AboutActivity.kt       # "Developed by MissionBreak56"
        └── res/
            ├── layout/ (activity_main, item_song, activity_player, activity_about)
            ├── menu/menu_main.xml
            ├── drawable/ (ic_play, ic_pause, ic_music_note, ic_favorite, ic_favorite_border, ic_launcher)
            └── values/ (strings, colors, themes)
```

---

## 3. Change summary vs. requested tasks

| # | Task | Result |
|---|---|---|
| 2 | Remove internet | **No `INTERNET` / `ACCESS_NETWORK_STATE` in manifest.** No Retrofit/OkHttp/Ktor anywhere. |
| 3 | Remove ads | **No ad SDKs, no `AdView`/`InterstitialAd`, no ad unit IDs.** |
| 4 | Premium permanently on | `Prefs.isPremium()` returns `true`; `App.onCreate()` persists `premium=true` on first run. No license check. |
| 5 | Creator name | `strings.xml` → `developer_name = MissionBreak56`, About screen, manifest meta-data `${author}`. `applicationId` untouched by branding. |
| 6 | Remove Google Drive | No Drive/Backup/Restore deps or classes. `android:allowBackup="false"`. |
| 7 | Remove account | No login/signup/profile screens, no auth deps (no Firebase). |
| 8 | Remove feedback | No feedback screens/buttons/deps. |
| 9 | ARM64 APK | `splits { abi { include("arm64-v8a") } }` in `app/build.gradle.kts`. |

---

## 4. Build

### Option A — Android Studio (easiest)
1. `File ▸ Open` → select the `MissionPlayer` folder.
2. Let it sync (it downloads Gradle 8.7 + AGP 8.5.2 + Android SDK 34).
3. `Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)` (debug), or:
   `Build ▸ Generate Signed Bundle/APK` for release.

### Option B — Command line
Requires JDK 17 + Android SDK (`ANDROID_HOME`).
```bash
./gradlew clean assembleRelease
```
On Windows:
```powershell
.\gradlew.bat clean assembleRelease
```

If `gradlew`/`gradle-wrapper.jar` is not present yet, generate it once with a
machine that has Gradle:
```bash
gradle wrapper --gradle-version 8.7
```

### Option C — GitHub Actions (no local toolchain)
Push the project to a repo; the workflow `.github/workflows/build-apk.yml`
builds the signed ARM64 APK and uploads it as an artifact.

### Output
```
app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

Verify the APK contains only arm64 native libs (this app is pure Kotlin/Java, so
there may be no `lib/` at all — that also satisfies "arm64 only"):
```bash
unzip -l app/build/outputs/apk/release/app-arm64-v8a-release.apk | grep lib/
```

---

## 5. Notes on this machine

This environment has **no JDK, Gradle, Android SDK or Android Studio**
installed, so the APK could not be compiled here. The project is complete and
buildable via any of the three options above.
