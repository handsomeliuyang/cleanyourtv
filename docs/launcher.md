# The launcher

[Back to the README](../README.md#the-launcher)

A small native home screen for Android TV: **about 572 KB**, plain Java, built for TVs with 1 GB of RAM.
No blur, no video, no network permission, no background services. This page has the details the
README leaves out: settings, languages, remote keys and building it yourself. Every key a theme can
set is in [`themes/README.md`](../themes/README.md#keys), and the opt-in switches with their
screenshots are in [`docs/switches.md`](switches.md).

## Which TVs

The **TV** needs Android 8.0 (API 26) or newer: `minSdk` is a compile-time floor in
[`launcher/app/build.gradle`](../launcher/app/build.gradle), and the install script asks the TV for
`ro.build.version.sdk` before it builds, so an older set is told both numbers instead of failing at
`pm install` (see [Troubleshooting](TROUBLESHOOTING.md) for what is left on such a set).

## Build it yourself

It needs JDK 17 and Android SDK platform 34. On macOS: `brew install openjdk@17 android-commandlinetools`
and `sdkmanager "platforms;android-34" "build-tools;34.0.0"`. On Linux the install script finds
`~/Android/Sdk` and the `java` on your PATH.

## Configure

[`config.xml`](../launcher/app/src/main/res/values/config.xml) sets:

- the dock order, hidden apps and the Live TV package;
- columns (tiles shrink from 5 up) and rows under the dock;
- an optional VPN app for the network pill;
- `reduced_motion`, for TVs whose GPU cannot keep up.

Themes set the same keys plus a background colour and a wallpaper.

### Fixed household Home (local ES43 configuration)

This checkout enables `fixed_home` in `launcher/app/src/main/res/values/config.xml` for a household
with four permanent slots, in order: 看电影 (Jellyfin), 看照片 (Synology Photos for Android TV), 看电视
(HDMI 1), and 看央视 (央视频 TV). Set `fixed_home` to `false` and rebuild to use the ordinary launcher again.
These are build-time settings: themes and saved on-TV choices cannot change the slot list or unlock it.

- `fixed_home_slots` contains four `label|target` entries. An app target is its package name; comma-separated
  alternatives support regional editions of the same app, with the first installed TV entry winning.
  Only TV launch activities are opened. Missing or disabled apps keep their slots and show a maintenance
  message when selected; they never lead to an installer or store. Additional installed apps never appear.
- Installed clients use their own declared activity/application icon, centred in a uniform card. Missing
  clients, missing icons and HDMI use the built-in functional artwork. Only the focused card shows its
  short Chinese name; reserved label space prevents layout jumps.
- The desktop has no app library, module cards, install/add-app entry, edit/reorder menu, app-info shortcut,
  or long-press editing. The network indicator is read-only. Up opens the appearance-settings gear;
  Down returns to the row. Left/Right stop at its ends. Home returns to the first slot, and returning
  from an app restores its slot.
- The gear retains the original wallpaper picker, dock opacity, clock and animation controls. Column,
  row and reorder controls are hidden because the four slots are fixed. Themes can still be pushed
  after installation using the original theme provider; no rebuild is needed to change appearance.
  Theme changes cannot expose an app library or change the four destinations.
- `input:HDMI 1` selects a unique HDMI port whose original label is `HDMI 1` (spacing and case ignored).
  It excludes HDMI-CEC child devices. `fixed_home_input_id` can instead name the exact HDMI 1 input from
  this TV's `dumpsys tv_input`. A missing ID or ambiguous label shows the maintenance message; neither
  the first enumerated input nor `HW1` is assumed to be HDMI 1. The existing `input_action` and
  `input_sources` configuration still handles vendor-specific switching after selecting the port.

App identifiers were checked against the [official Jellyfin TV listing](https://play.google.com/store/apps/details?id=org.jellyfin.androidtv),
the [Synology Android TV download archive](https://archive.synology.com/download/Mobile/AndroidTV-SynologyPhotos/1.1.4-10073)
(`com.synology.projectkailash` and `.cn`), and the standard APK linked by the
[央视频 TV publisher](https://www.chinaott.com/cpzx/xstphone.html) (`com.newtv.cboxtv`).
The launcher opens these clients; it does not bundle, install or sign in to them. NAS and Jellyfin
credentials belong in the respective clients, not in launcher configuration.

Settings are protected by a six-digit [RFC 6238](https://www.rfc-editor.org/rfc/rfc6238) TOTP code
(SHA-1, 30-second steps, one adjacent step of clock tolerance). No settings view is inflated until
verification succeeds. Leaving settings for Home, another app or the lock screen closes that session.
The quick-settings shortcut goes through the same gate. A used code cannot unlock again; five wrong
codes cause a 60-second cooldown that survives process restarts. Missing or corrupt keys fail closed.

The owner has confirmed the current Authenticator binding and wants to keep it for future builds.
Routine builds and in-place updates must retain the package name, signing key and app data. They must
not run provisioning again: the secret lives in private device data, not in the APK. Authentication
tests belong on an isolated emulator with a separate test key. A first installation on another device
requires a separate migration of the owner's existing binding; do not silently generate a replacement.

The following provisioning command is only for an explicitly requested new/reset binding, using an
already-authorized ADB connection. It is **not** part of the build or ordinary update procedure:

```sh
tools/venv/bin/pip install 'qrcode[pil]'  # desktop QR generation only
tools/venv/bin/python tools/settings-auth.py --output-dir ~/.cleanyourtv/auth/phone-preview
# For the TV instead, use a new folder and add --serial <adb-target> --label 'Family TV'.
```

Scan the private PNG with an Authenticator app and enter its current code on the device to confirm.
Delete the temporary QR folder after confirming; the QR is a copy of the binding secret. The tool
refuses output inside the checkout and never prints the secret or URI. The device stores its 20-byte
key in its private, non-backed-up directory, separate from themes and layout preferences. The existing
provider exposes a dedicated **ADB-shell-only, write-only** provisioning path, not a theme key or a
read/export endpoint. To recover from a lost phone or bad binding, rerun provisioning with a new output
folder; the new key replaces the old binding. There is no default or recovery PIN. Theme pushes cannot
remove the gate. Authenticator cloud sync can copy the key to other devices; TOTP is possession of a
secret, not hardware binding or two-factor authentication by itself.

Gradle derives `BuildConfig.FIXED_HOME` from the same `config.xml` boolean so the release optimiser can
remove unreachable app-library paths in this household build, while retaining theme/appearance code.
Set the resource back to `false` and rebuild for the ordinary launcher, without the settings gate.

This locks the desktop only. Native TV settings, physical remote shortcuts, USB installation and ADB
remain system functions. There is no device-owner mode, new permission or network access.

Build locally using **Build it yourself** above; do not run `build-and-install.sh` until the TV is available.
Before installing on the ES43, check its SDK and installation restrictions, back up its original state,
then verify all four entries, real remote long presses, HDMI 1 video/audio, return to Home, standby/wake
and reboot. A local build or emulator check does not establish compatibility with the ES43 firmware.

## Launcher settings and the default home

Click **Settings** for the TV's own settings. Hold OK on it for its menu:

- **Launcher settings**. With `launcher_settings_button` a click opens those instead, and the menu
  leaves the row out.
- **Native settings**, where the TV ships a settings app of its own beside Android's - Xiaomi China,
  TCL/FFALCON and Fire OS are in `settings_fallback_components` - because that app holds pages
  Android's has no row for, such as Xiaomi's Account & Security with the ADB switch.
- **Module cards**, with `module_cards`.
- **Edit dock**, with `dock_row`.
- **Reorder apps**.
- **Restart…** while `HomeKeyService` runs, which opens the system power menu (a launcher may not
  restart the TV itself).

The launcher's own page has these rows, top to bottom:

- **Reorder apps**, only with `launcher_settings_button`: OK picks an app up, the arrows move it, Back
  places it, and Back again finishes.
- **Apps per row** 4, 5, 6 or 7; **Rows below dock** 0 to 3, how many rows of tiles stay visible under the
  dock band; **Dock band opacity** 100%, 70%, 50% or 30%. The last two only do anything while the dock is
  its own row (`dock_row`, on by default).
- **Clock**: **System** follows the TV's 12- or 24-hour setting; **12-hour** or **24-hour** sets the launcher's
  clocks (Home and the clock screensaver) to that, whatever the TV uses (`clock_format` in a theme).
- **Wallpaper**: one picture from the theme's collection (or its `wallpaper.jpg`), **None**, or **Theme
  default**. A pick holds that picture and stops the rotation until you choose Theme default, press
  Follow theme, or push the theme again.
- **Animations** on or off, and **Advanced effects** (tile lift, icon zoom and smooth ambient-colour
  transitions; they may use more memory) once animations are on.
- **Open on start**, only with `open_on_start_choice`: Off, or an app that opens instead of Home the first
  time Home comes up after the TV starts. Once per boot, so Home and Back still reach the launcher.
- **Follow theme (reset on-TV choices)**, which clears every choice above. Pushing or resetting a theme
  clears them too.
- **Choose default home** opens Android's home picker, and **TV system settings** the TV's own settings
  page.

**Change the default home:** Launcher settings and the optional Default-home tile
(`show_home_settings_shortcut`) open Android's home picker. Choose the original launcher to stop using
CleanYourTV as Home, or CleanYourTV to return. The original launcher must be enabled; if the install
script disabled it, run the [restore script](../README.md#restoring-your-tv) first. Some TVs provide no
home picker.

## Languages

English, German, Danish, Spanish, Latin American Spanish, Catalan, French, Russian, Serbian (Latin), Polish, Czech, Italian, Portuguese,
Turkish, Arabic, Hebrew, Simplified Chinese, Traditional Chinese (Taiwan), Thai, Finnish, Vietnamese,
Bangla, Indonesian, Norwegian Bokmål, Swedish, Dutch, Uzbek (Latin), Ukrainian, Korean and Asturian. The launcher follows the TV's language. Android 13+ can set it for the launcher
alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.

Hebrew mirrors the layout:

- menu buttons on the left, date and clock on the right with the greeting above;
- dock and app rows starting on the right, with matching remote navigation;
- the input selector's icons and labels on the right;
- dates with the month prefix ב (for example, 19 באוקטובר).

The Clock screensaver right-aligns its date and time. Russian covers every screen, including race
weekends, festival greetings and the Shamsi date. Other languages keep their original layouts.

## Module cards

Any TV app can declare its own tile (label, caption, glow). The launcher asks you once per app, keeps it
hidden by default, and remembers the answer for that app's signing key and card name. Hold OK on Settings
and choose Module cards to change your mind. How to make one: [`docs/modules.md`](modules.md); the
contract: [`docs/specs/card.md`](specs/card.md). Ready-made ones are in [`addons/`](../addons/), and
[cleanyour.tv/addons](https://cleanyour.tv/addons) shows the themes, switches and add-ons together.

## Clock screensaver

Pick "Clock" in the TV's screensaver settings, or over ADB:

```bash
tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream; settings put secure screensaver_enabled 1'
```

## Remote keys

- An optional accessibility service keeps the remote's **Input** button working on Google TV
  (`INPUT_KEY_SERVICE=1` at install).
- It can bring back other buttons only the stock launcher answered, such as the **YouTube** button
  (`app_keys` and `input_key_keycodes`, in config.xml or a theme; `HOME_KEY_SERVICE=1` reads `app_keys` too).
- A `hold:` prefix on an `app_keys` entry gives that button a second app: a normal press opens the
  unprefixed app, holding for half a second opens the hold one (by keycode or `hold:scan:...`). An entry can
  name an activity (`package/.Activity`), such as an add-on's quick bar, instead of the app's launcher entry.
- In place of an app, an `app_keys` entry can name an action: `back`, `home`, `recents`, `mute`, `sleep`,
  `screensaver`, `settings` or `wifi`, so a spare colour button can mute the TV or, held, put it in standby.
- `HOME_KEY_SERVICE=1` also fixes sticks whose Home key sends ALL_APPS, and opens Settings from the remote's
  **gear** button, even one Android does not map (scancodes or keycodes in config.xml).
- With `settings_key_panel` the gear opens a [small quick settings panel](quick-settings-panel.jpg)
  over Home or any app: TV settings, Display, Network, Apps and Launcher settings.

The full `app_keys` format is in the [`themes/README.md` keys table](../themes/README.md#keys).

## Navigation and artwork

Up and Down move one row. The top buttons never fall through to tiles, and Down from them returns to the
dock tile you came from. The last focused app is remembered, and installing or removing an app refreshes
the tiles while Home is open. App banners load at xhdpi or better, so low-density TV UIs do not downsample
them before the focus zoom.
