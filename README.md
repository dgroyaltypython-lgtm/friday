# FRIDAY AI — Codemagic APK Build Project

This repository is prepared for Codemagic CI builds.

## Repository layout
- `android/`: Android app source
- `backend/`: FastAPI/OpenAI backend starter
- `codemagic.yaml`: Codemagic debug and release APK workflows

## Build APK with Codemagic
1. Upload this project to a GitHub repository. The `codemagic.yaml` file must be at the repository root.
2. Sign in to Codemagic and choose **Add application**.
3. Connect GitHub and select the repository.
4. Select configuration: **codemagic.yaml**.
5. Start the `friday-android-debug` workflow first.
6. Open the completed build and download `app-debug.apk` from Artifacts.
7. For release, run `friday-android-release`. The unsigned release APK is suitable for testing only; sign it for public distribution.

## Configure AI backend
The app's `BACKEND_BASE_URL` is in `android/app/build.gradle.kts`. Replace `https://YOUR-HTTPS-BACKEND.example/` with your deployed HTTPS backend URL, including a trailing slash. Re-run Codemagic after committing the change.
The backend needs `OPENAI_API_KEY` configured as a server environment variable. Never commit `.env` or put the OpenAI API key in Android source.

## Build details
- JDK 17
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Android SDK platform/build-tools 35
- Kotlin 2.0.21

## Before production release
- Configure backend HTTPS URL, server-side key, authentication, rate limiting and privacy policy.
- Add Android app signing in Codemagic using a keystore and secure environment variables.
- Test on the target device and check multilingual speech/voice packs.
- Phone calls, WhatsApp and social-media actions are not enabled by this starter; each needs a supported Android/third-party API and user-granted permissions.
