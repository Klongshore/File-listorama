# 1. Native Kotlin with Jetpack Compose

Status: accepted

The app is Android-only. Storage access (All files access, scoped storage) and the cloud auth libraries (MSAL, Google Identity) are first-class on native Android, so cross-platform toolchains add friction without a stated benefit. We use Kotlin, Jetpack Compose and Material 3, minSdk 30 (one consistent storage model; the target device runs Android 15).
