# MissionPlayer

An **original**, offline, ad-free local music player for Android, with a layout and
feature set in the spirit of popular offline players (tabs for songs / playlists /
albums / artists / folders / genres, mini player, full "Now playing" screen,
equalizer, sleep timer, lyrics, color themes).

> No code, resources, strings, icons or branding were copied from any other app.
> Everything here is new. Package name: `com.missionbreak56.musicplayer`.

## Features

| Area | What it does |
|---|---|
| Library | Scans the device with MediaStore. Tabs: **Songs, Playlists, Albums, Artists, Folders, Genres**, swipeable |
| Songs | Shuffle-all button, sort by title / artist / album / date added / duration, search screen |
| Smart playlists | Favorites, Recently added, Most played, Recently played |
| Playlists | Create, rename, delete, add / remove songs from the song menu |
| Song menu | Play next, add to queue, add to playlist, favorite, set as ringtone, share |
| Folders | Pick a folder photo from your gallery or from the covers of the songs inside the folder |
| Mini player | Art, title, progress, play/pause, next, queue sheet — shown on every list screen |
| Now playing | Large art with color tint taken from the cover, seek bar, shuffle, repeat (off / all / one), favorite, queue |
| Lyrics | Attach a `.lrc` (synced, tap a line to seek) or `.txt` file per song |
| Equalizer | Device equalizer with presets and band sliders, bass boost, surround, **volume boost** above the normal maximum (up to +15 dB) |
| Sleep timer | 15 / 30 / 45 / 60 / 90 / 120 minutes, works in the background |
| Themes | 8 accent colors + system / light / dark mode |
| Languages | English and Portuguese (Brazil) |
| Resume | Remembers the last queue, song and position, and the chosen shuffle / repeat mode |
| Shuffle | Plays every song once before any repeats (current song first), also for songs added to the queue |
| Playback | Media3 `MediaSessionService`: background play, notification and lock-screen controls |

Not included: ads, analytics, network access, accounts, video player.

## Project structure

```
app/src/main/java/com/missionbreak56/musicplayer/
├── App.kt
├── data/       Song, MusicRepository (MediaStore), Library (albums/artists/folders/genres/playlists),
│               Prefs (favorites, playlists, stats, theme, equalizer), Lyrics (.lrc parser + store)
├── playback/   PlaybackService, PlayerConnection, PlaybackQueue, AudioFx (equalizer), SleepTimer
└── ui/         MainActivity (tabs), DetailActivity, SearchActivity, PlayerActivity,
                EqualizerActivity, ThemesActivity, AboutActivity, adapters and helpers
```

## Build

CI (`.github/workflows/build-apk.yml`) builds an ARM64 release APK on every push to `main`
and uploads it as the `app-arm64-v8a-release` artifact (Actions tab → run → Artifacts).

Locally: `gradle assembleRelease` (JDK 17, Android SDK 34).
