# Publishing Brahmi Keyboard

Package ID (permanent): `io.github.initindia.brahmikeyboard`. First version: `0.9.0` (versionCode 1).
Licence: GPL-3.0-or-later. Privacy policy: `PRIVACY.md`.

## 0. Before anything: commit the font (one click)

GitHub, Actions tab, **Fetch Brahmi font**, Run workflow. It downloads Noto Sans Brahmi (SIL OFL) and commits it. F-Droid and the release build need it in the repository.

## 1. Signing key for Google Play (you do this, on your computer)

The key proves updates come from you. Never put it in the repository. Back it up in two places: if you lose it you cannot update the app under the same key.

```
keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload
base64 -w0 upload.jks > upload.jks.base64        # macOS: base64 -i upload.jks -o upload.jks.base64
```

GitHub, Settings, Secrets and variables, Actions, add four secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | contents of `upload.jks.base64` |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | the key password |

Use **Play App Signing** when Google Play Console offers it (the default). Google then holds the real signing key and your key is only an upload key, which can be reset if lost.

## 2. Google Play

1. Create a Play Console developer account (one-time fee, identity verification needed): https://play.google.com/console
2. **Create app**: name *Brahmi Keyboard*, language English, type App, Free.
3. **Store listing**: use the files in `fastlane/metadata/android/en-US/` (title, short description, full description, `images/icon.png` 512x512, `images/featureGraphic.png` 1024x500). **Phone screenshots: at least 2, take them on your phone** with the keyboard open (Brahmi mode, Pure Brahmi mode, Custom mode, Settings). Save them as `fastlane/metadata/android/en-US/images/phoneScreenshots/1.png` and so on; F-Droid uses the same folder.
4. **Privacy policy URL**: `https://github.com/init-india/brahmikeyboard/blob/main/PRIVACY.md`
5. **App content**: ads: No. Data safety: collects no data, shares no data, no data leaves the device. Content rating: fill the questionnaire (a utility app, no user content, no violence); expect Everyone. Target audience: 13 and over is simplest. App category: Tools or Productivity.
6. **Release**: push a tag, for example `git tag v0.9.0 && git push origin v0.9.0`. The **Release** workflow builds a signed `.aab` and attaches it to a GitHub release. Download the `.aab` and upload it in Play Console under **Testing, Closed testing**.
7. **Closed test rule.** Personal developer accounts created after 13 November 2023 must run a closed test with at least 12 testers opted in continuously for 14 days before they can apply for production access ([Google's rules](https://support.google.com/googleplay/android-developer/answer/14151465)). Recruit 12 or more people (friends, learners of Brahmi, a community), add their Google accounts as testers, and keep them opted in. After 14 days use **Apply for production**. Review usually takes up to seven days. Organisation accounts do not have this rule.
8. Later releases: raise `versionCode` (it must only go up) and `versionName` in `android/app/build.gradle.kts`, add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters or fewer), tag `vX.Y.Z`, upload the new `.aab`.

## 3. F-Droid

F-Droid builds the app from source itself and signs it with its own key, so you do not upload anything. Details: [Submitting to F-Droid](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

1. The repository already has: GPL licence, no Google libraries, no network permission, fastlane metadata, and the font (after step 0).
2. Create a tag that matches `versionName`: `v0.9.0`. F-Droid builds that tag.
3. Create a GitLab account and fork https://gitlab.com/fdroid/fdroiddata.
4. In your fork make a branch named `io.github.initindia.brahmikeyboard` and copy `docs/fdroid/io.github.initindia.brahmikeyboard.yml` to `metadata/io.github.initindia.brahmikeyboard.yml`.
5. Run the checks (from the fdroiddata folder, with fdroidserver installed, or let the pipeline on your fork run them): `fdroid readmeta`, `fdroid rewritemeta io.github.initindia.brahmikeyboard`, `fdroid lint io.github.initindia.brahmikeyboard`, `fdroid build io.github.initindia.brahmikeyboard`.
6. Commit as `New App: io.github.initindia.brahmikeyboard`, push, and open a merge request to fdroiddata. Reviewers may ask for changes (for example to the `subdir` or `gradle` lines, because the Gradle project root is `android/`). Reply promptly.
7. After merging, expect about one to two days before the app appears.

New versions after that are picked up automatically from tags (`AutoUpdateMode: Version`).

## 4. Two stores, two signatures

A Play install and an F-Droid install are signed by different keys, so a user cannot update one over the other. Users should stay with one source.

## 5. App Store (later)

Needs a Mac, Xcode and an Apple Developer account, and a separate Swift app and keyboard extension that reads the same `data/` files. Not started.
