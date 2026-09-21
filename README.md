# Velune

**A minimalist Android launcher that helps you use your phone with intention — not by accident.**

No icons. No feeds. No mindless scrolling. Just the apps you actually meant to open.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-Free%20%26%20Open-lightgrey)

---

## Why Velune?

Most "minimalist launcher" apps lock their best features — app blocking, notification filtering, blocking schedules — behind a paywall. Velune doesn't. Every feature below is free, always.

##  Features

###  Home Screen
- Icon-free, text-only interface — nothing designed to grab your attention
- Live clock (tap it to jump straight to your alarms) and date
- Quick-access **Favorites**
- Search-first app drawer — type a name, don't scroll a grid

###  Organization
- **Rename** any app to whatever makes sense to you
- **Folders** — group apps together instead of scrolling past them
- **Hide** apps you don't want to see at all

###  Focus & Blocking
- **App Blocking** — block any app from 1 hour up to 30 days with a smooth slide-to-confirm gesture
- **Blocking Schedules** — set a recurring weekly window (e.g. block Instagram 9am–5pm on weekdays) and it blocks itself, automatically
- **Mindful Launch Delay** — add a short pause before any app opens, so opening it is a decision, not a reflex
- **Notification Filter** — notifications from currently-blocked apps are held back instead of interrupting you, and saved for later
- **Monochrome Mode** — dims distracting apps with a muted overlay while you're in them

###  Appearance
- Choose an accent color and a font style to make it yours
- Guided onboarding on first launch

###  Real Data, Not Guesswork
- Per-app screen time (last 7 days) pulled from Android's own usage stats — used to show you exactly how much time an app is costing you, and to suggest what else might be worth blocking

## 📲 Download

Grab the latest APK from the **[Releases](https://github.com/memohammedikhlas/Velune/releases)** page — no Play Store, no account, no sign-up.

## 🛠️ Build from Source

```bash
git clone https://github.com/memohammedikhlas/Velune.git
cd Velune
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Every push to `main` is built automatically via GitHub Actions — see [`.github/workflows/build.yml`](.github/workflows/build.yml).

##  Permissions

Velune asks for a few permissions, and only uses them for what's described here:

| Permission | Used for |
|---|---|
| Display over other apps | Showing the Mindful Launch Delay countdown and Monochrome Mode overlay |
| Usage Access | Reading real per-app screen time to power App Blocking |
| Notification Access | Filtering notifications from currently-blocked apps (Notification Filter) |

None of these are used for tracking, analytics, or anything that leaves your device.

##  Built With

- **Kotlin** — 100% Kotlin, no Java
- **AndroidX** (AppCompat, ViewPager2, ConstraintLayout, Material3)
- **UsageStatsManager** for real screen-time data
- **NotificationListenerService** for the Notification Filter
- No third-party analytics, ads, or trackers

##  Status

Actively developed. Found a bug or have a feature idea? Open an issue.
