# Android Human Pose Estimation

[![Android CI](https://github.com/KouroshEsmaeili/android-pose-estimation/actions/workflows/android.yml/badge.svg)](https://github.com/KouroshEsmaeili/android-pose-estimation/actions/workflows/android.yml)
[![API](https://img.shields.io/badge/API-24%2B-3DDC84.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

A focused Android application that estimates a single person’s body pose from the front camera, renders a body skeleton over the preview, and reports left and right elbow angles. Camera frames are processed locally with CameraX and ML Kit; the application has no network permission and does not store images.

## What this project demonstrates

- Lifecycle-aware front-camera capture with CameraX
- On-device human pose landmark estimation with ML Kit
- Preview-aligned landmark rendering without manual rotation or scaling constants
- Confidence-aware skeleton visualization
- Relative 3D joint-angle calculation with numerical edge-case handling
- Runtime permission, unavailable-camera, and inference-error states
- Unit-tested geometry and automated Android build checks

This is a deployment and application-integration project. It does not train or own the underlying pose model.

## Processing flow

```text
Front camera
    │
    ▼
CameraX ImageAnalysis (keep latest frame)
    │
    ▼
ML Kit Pose Detector (stream mode, bundled model)
    │
    ├── 33 body landmarks + in-frame likelihood
    └── relative depth coordinates
    │
    ▼
CameraX view-coordinate transform
    │
    ▼
Confidence-filtered skeleton + elbow angles
```

`LifecycleCameraController` and `MlKitAnalyzer` handle frame ownership, backpressure, device rotation, front-camera mirroring, and conversion into `PreviewView` coordinates. The custom overlay only renders the transformed results.

## Technology

| Component | Version | Purpose |
| --- | --- | --- |
| Android Gradle Plugin | 8.13.2 | Android build tooling |
| Gradle | 8.13 | Reproducible wrapper build |
| CameraX | 1.5.3 | Preview, lifecycle, and image analysis |
| ML Kit Pose Detection | 18.0.0-beta5 | Bundled on-device pose model |
| Java | 17 | Application source language |

The ML Kit Pose Detection API is still beta. Its API and model behavior may change between releases.

## Requirements

- JDK 17
- Android SDK 36 with Build Tools 36.0.0
- Android Studio with support for Android Gradle Plugin 8.13
- Android device running API 24 or newer
- Front-facing camera

A physical device is recommended. An emulator can launch the application but may not provide representative camera or inference behavior.

## Build and run

1. Clone the repository.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to finish and install Android SDK 36 if prompted.
4. Connect a compatible device.
5. Run the `app` configuration and grant camera permission.

Command-line verification:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

On Windows:

```powershell
gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
```

## Project structure

```text
app/src/main/java/io/github/kouroshesmaeili/poseestimation/
├── MainActivity.java       # Permission and lifecycle-aware camera setup
├── PoseOverlayView.java    # Confidence filtering and preview rendering
└── PoseMath.java           # Pure 3D angle geometry

app/src/test/
└── PoseMathTest.java       # Geometry edge cases
```

## Privacy

- Camera access is the only runtime permission.
- Frames are processed in memory on the device.
- Images and pose results are not written to storage.
- The final merged manifest removes optional network permissions contributed by ML Kit.
- Application backup is disabled.

## Limitations

- ML Kit returns the most prominent person; multi-person estimation is not supported.
- A visible face and sufficient full-body context generally improve detection.
- Landmarks below the configured in-frame likelihood threshold are not rendered.
- The Z coordinate is relative depth in image units, not metric 3D reconstruction.
- Elbow angles inherit uncertainty from the estimated landmarks.
- The repository makes no accuracy, latency, frame-rate, or robustness claims.
- Pose classification and activity recognition are outside the current scope.

## Validation scope

Automated checks validate compilation, Android lint, debug and minified release packaging, and the pure geometry layer. Camera behavior, overlay alignment, and device performance still require testing on physical hardware across portrait and landscape orientations.

## License

Licensed under the [Apache License 2.0](LICENSE).
