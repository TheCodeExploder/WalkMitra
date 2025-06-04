# WalkMitra

WalkMitra is a sample Android application written in Kotlin using Jetpack Compose. It tracks your walks using Google Maps and location services and stores your history with DataStore.

## Features
- Live map tracking of your walk path.
- Start, pause, resume and stop walk sessions.
- Statistics on distance, speed and calories.
- History screen showing past walks with optional map screenshot.
- User profile saved locally.

## Requirements
- **Android Studio** Hedgehog or later
- **Android SDK** 35+
- A Google Maps API key placed in `local.properties` as `MAPS_API_KEY`.

## Build & Run
1. Clone the repository.
2. Create a `local.properties` file in the project root and add your Google Maps key:
   
   ```
   MAPS_API_KEY=YOUR_KEY_HERE
   ```
3. Open the project in Android Studio and run the `app` configuration.

## Project Structure
```
app/
 └── src/
     ├── main/
     │   ├── java/com/prashant/walkmitra/  → app source code
     │   └── res/                           → resources
     ├── test/                              → unit tests
     └── androidTest/                       → instrumented tests
```

The `data` package contains DataStore helpers and models. UI composables live in the `ui` package. `LocationService` provides background location updates.
