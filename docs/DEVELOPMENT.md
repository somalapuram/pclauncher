# Development

Stage A (SRS §2): pclauncher is a standalone APK. Nothing here needs the AOSP tree.

## Toolchain on this machine

| Piece | Where | Version |
|---|---|---|
| Android Studio | `~/android-studio` (launch: `~/android-studio/bin/studio.sh`, or the desktop entry) | Quail 3 · 2026.1.3 Patch 1 |
| Android SDK | `~/Android/Sdk` (`ANDROID_HOME`, exported from `~/.bashrc`) | platform **37.0** (Android 17) + 36 |
| Build tools | | 37.0.0, 36.0.0 |
| JDK | system `/usr/lib/jvm/java-17-openjdk-amd64` for the CLI; Studio ships its own JBR 25 | 17 / 25 |
| Gradle | wrapper only — `./gradlew` | 9.7.1 |
| AGP | | 9.3.1 (Kotlin support is **built in**; there is no `org.jetbrains.kotlin.android` plugin) |

`compileSdk` is 37 because that is Android 17 — the same platform level `pc_x86_64` ships, so
Stage A compiles against the Stage B target from day one.

## Build and test

```sh
./gradlew assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew test                 # unit tests, all modules
./gradlew lint                 # lint, all modules
```

Every version lives in `gradle/libs.versions.toml`, including the SDK levels. Nothing else may
hardcode one.

## Emulators

Two AVDs, both x86_64 with KVM:

| AVD | Image | Why |
|---|---|---|
| `pclauncher_pc_api37` | `android-37.0;google_apis` | **Primary.** 2560×1600 at 240 dpi — the same panel and density as `pc_x86_64` (SRS §4). |
| `pclauncher_desktop_api34` | `android-34;android-desktop` | **Secondary.** Boots a real desktop-windowing environment, the closest emulator analogue to WM Shell on the target. |

```sh
$ANDROID_HOME/emulator/emulator -avd pclauncher_pc_api37 -no-snapshot -gpu swiftshader_indirect
```

`swiftshader_indirect` is deliberate: the target renders in software until Mesa lands, so testing on
a software renderer is testing the real thing (SRS §4.3).

## Running it as the home screen

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.somalapuram.pclauncher/.HomeActivity   # or tap it in the app drawer
```

Then use the in-app **Set as default home** button, or:

```sh
adb shell cmd role add-role-holder android.app.role.HOME com.somalapuram.pclauncher
```

To hand the home role back:

```sh
adb shell cmd role remove-role-holder android.app.role.HOME com.somalapuram.pclauncher
```

## Enabling freeform on a dev device (tier T1)

Not needed for phase 1 — the shell runs at T0. When phase 6 lands:

```sh
adb shell settings put global enable_freeform_support 1
adb shell settings put global force_resizable_activities 1
adb reboot
```

On `pc_x86_64` none of this applies: the ROM enables desktop windowing itself (SRS §4.1).
