package io.github.kouroshesmaeili.poseestimation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.kouroshesmaeili.poseestimation.databinding.ActivityMainBinding;

/** Hosts camera permission state and the lifecycle-aware CameraX/ML Kit pipeline. */
public final class MainActivity extends AppCompatActivity {
    private static final String TAG = "PoseEstimation";

    private ActivityMainBinding binding;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Executor mainExecutor;
    private ExecutorService analysisExecutor;

    private LifecycleCameraController cameraController;
    private PoseDetector poseDetector;
    private boolean cameraInitializationStarted;
    private boolean cameraRunning;
    @StringRes
    private int displayedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainExecutor = ContextCompat.getMainExecutor(this);
        analysisExecutor = Executors.newSingleThreadExecutor();

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::handleCameraPermissionResult
        );

        if (hasCameraPermission()) {
            startCamera();
        } else {
            showPermissionRequest(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        if (!hasCameraPermission()) {
            if (cameraRunning || cameraInitializationStarted) {
                showPermissionRequest(true);
            }
            return;
        }
        if (!cameraRunning) {
            startCamera();
        }
    }

    private void handleCameraPermissionResult(boolean granted) {
        if (granted) {
            startCamera();
            return;
        }

        boolean mustUseSettings = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
        showPermissionRequest(mustUseSettings);
    }

    private void showPermissionRequest(boolean mustUseSettings) {
        releaseCameraResources();
        binding.poseOverlay.clear();

        if (mustUseSettings) {
            showMessage(
                    R.string.permission_settings_title,
                    R.string.permission_settings_body,
                    R.string.action_open_settings,
                    ignored -> openApplicationSettings()
            );
        } else {
            showMessage(
                    R.string.permission_title,
                    R.string.permission_body,
                    R.string.action_allow_camera,
                    ignored -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            );
        }
    }

    private void startCamera() {
        if (!hasCameraPermission() || cameraInitializationStarted || cameraRunning) {
            return;
        }

        cameraInitializationStarted = true;
        showStatus(R.string.status_initializing);

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        PoseDetector newPoseDetector = PoseDetection.getClient(options);
        poseDetector = newPoseDetector;

        LifecycleCameraController newCameraController = new LifecycleCameraController(this);
        cameraController = newCameraController;
        newCameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS);
        newCameraController.setImageAnalysisBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        );
        newCameraController.setImageAnalysisAnalyzer(
                analysisExecutor,
                new MlKitAnalyzer(
                        Collections.singletonList(newPoseDetector),
                        ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        mainExecutor,
                        result -> handlePoseResult(result, newPoseDetector)
                )
        );

        ListenableFuture<Void> initialization = newCameraController.getInitializationFuture();
        initialization.addListener(
                () -> completeCameraInitialization(initialization, newCameraController),
                mainExecutor
        );
    }

    private void completeCameraInitialization(
            ListenableFuture<Void> initialization,
            LifecycleCameraController initializedController
    ) {
        if (isDestroyed() || initializedController != cameraController) {
            return;
        }

        try {
            initialization.get();
            if (!initializedController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                releaseCameraResources();
                showMessage(
                        R.string.camera_unavailable_title,
                        R.string.camera_unavailable_body,
                        0,
                        null
                );
                return;
            }

            initializedController.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA);
            binding.previewView.setController(initializedController);
            initializedController.bindToLifecycle(this);
            cameraRunning = true;
            cameraInitializationStarted = false;
            showStatus(R.string.status_no_pose);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            handleCameraStartupFailure(error);
        } catch (ExecutionException | RuntimeException error) {
            handleCameraStartupFailure(error);
        }
    }

    private void handlePoseResult(
            @NonNull MlKitAnalyzer.Result result,
            PoseDetector resultDetector
    ) {
        if (resultDetector != poseDetector || binding == null) {
            return;
        }

        Throwable error = result.getThrowable(resultDetector);
        if (error != null) {
            Log.e(TAG, "Pose estimation failed for a camera frame", error);
            binding.poseOverlay.clear();
            showStatus(R.string.status_detection_error);
            return;
        }

        Pose pose = result.getValue(resultDetector);
        binding.poseOverlay.setPose(pose);
        showStatus(
                pose == null || pose.getAllPoseLandmarks().isEmpty()
                        ? R.string.status_no_pose
                        : R.string.status_tracking
        );
    }

    private void handleCameraStartupFailure(Throwable error) {
        Log.e(TAG, "Camera initialization failed", error);
        releaseCameraResources();
        showMessage(
                R.string.camera_error_title,
                R.string.camera_error_body,
                R.string.action_retry,
                ignored -> startCamera()
        );
    }

    private void showStatus(@StringRes int textResource) {
        binding.messagePanel.setVisibility(View.GONE);
        if (displayedStatus != textResource) {
            binding.statusChip.setText(textResource);
            displayedStatus = textResource;
        }
        binding.statusChip.setVisibility(View.VISIBLE);
    }

    private void showMessage(
            @StringRes int titleResource,
            @StringRes int bodyResource,
            @StringRes int actionResource,
            View.OnClickListener action
    ) {
        displayedStatus = 0;
        binding.statusChip.setVisibility(View.GONE);
        binding.messageTitle.setText(titleResource);
        binding.messageBody.setText(bodyResource);
        binding.messagePanel.setVisibility(View.VISIBLE);

        if (action == null || actionResource == 0) {
            binding.actionButton.setVisibility(View.GONE);
            binding.actionButton.setOnClickListener(null);
        } else {
            binding.actionButton.setText(actionResource);
            binding.actionButton.setOnClickListener(action);
            binding.actionButton.setVisibility(View.VISIBLE);
        }
    }

    private void openApplicationSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)
        );
        startActivity(intent);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void releaseCameraResources() {
        cameraRunning = false;
        cameraInitializationStarted = false;

        LifecycleCameraController controllerToRelease = cameraController;
        cameraController = null;
        if (controllerToRelease != null) {
            controllerToRelease.clearImageAnalysisAnalyzer();
            controllerToRelease.unbind();
            if (binding != null) {
                binding.previewView.setController(null);
            }
        }

        PoseDetector detectorToClose = poseDetector;
        poseDetector = null;
        if (detectorToClose != null) {
            detectorToClose.close();
        }
    }

    @Override
    protected void onDestroy() {
        releaseCameraResources();
        analysisExecutor.shutdown();
        binding = null;
        super.onDestroy();
    }
}
