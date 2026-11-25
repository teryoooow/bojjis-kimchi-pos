# Bojji's POS 🧾

Android point-of-sale app for **Bojji's Kimchi** — track sales, payments, and expenses offline.

Companion to the menu website: [teryoooow/bojjis-kimchi](https://github.com/teryoooow/bojjis-kimchi)

## Features

- **Cart & checkout** — Kimchi (₱200) and Fried Rice Kimchi (₱100) with toppings
- **Toppings** — Egg, Odeng, Tteokbokki, Korean Sausage (1st topping included; extras ₱25)
- **Payments** — Cash (tendered + change) and GCash
- **Sales history** — today / all-time totals, per-product sales, Cash vs GCash balances, per-transaction delete
- **Expenses** — categories (Ingredients, Packaging, Transport, Utilities, Other), Cash/GCash, list + delete
- **Net profit** — All-time sales − expenses
- **Offline** — data stored on-device via SharedPreferences (JSON)

## Download

Grab **`BojjisPOS-v1.0.apk`** from this repo and sideload it on an Android device (Android 7.0+).

## Build

Requirements:

- JDK 17
- Android SDK (compileSdk 34)
- Gradle 8.x (or wrapper if you add one)

```bash
# from repo root
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Or open the project in **Android Studio** and Run / Build APK.

### Release signing

`app/build.gradle.kts` expects a keystore at `bojjis-release.jks` (gitignored). Provide your own keystore and update the `signingConfigs` block, or build a debug APK:

```bash
gradle assembleDebug
```

## Package

`com.bojjis.kimchi.pos`

## Stack

- Kotlin + Android Views (ViewBinding)
- Material Design 3 components
- No backend — fully offline

## Related

- Menu website: https://teryoooow.github.io/bojjis-kimchi/
- Facebook: https://www.facebook.com/share/19oVDJXUGh/?mibextid=wwXIfr
