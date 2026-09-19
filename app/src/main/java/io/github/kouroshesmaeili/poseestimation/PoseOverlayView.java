package io.github.kouroshesmaeili.poseestimation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

/** Draws body landmarks and elbow angles in PreviewView coordinates. */
public final class PoseOverlayView extends View {
    private static final float MIN_LANDMARK_LIKELIHOOD = 0.5f;

    private static final int[] BODY_LANDMARKS = {
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW,
            PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_HEEL,
            PoseLandmark.RIGHT_HEEL,
            PoseLandmark.LEFT_FOOT_INDEX,
            PoseLandmark.RIGHT_FOOT_INDEX
    };

    private static final int[][] CONNECTIONS = {
            {PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER},
            {PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW},
            {PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST},
            {PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW},
            {PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST},
            {PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP},
            {PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP},
            {PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP},
            {PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE},
            {PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE},
            {PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE},
            {PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE},
            {PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_HEEL},
            {PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_FOOT_INDEX},
            {PoseLandmark.LEFT_HEEL, PoseLandmark.LEFT_FOOT_INDEX},
            {PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_HEEL},
            {PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_FOOT_INDEX},
            {PoseLandmark.RIGHT_HEEL, PoseLandmark.RIGHT_FOOT_INDEX}
    };

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint jointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint angleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint angleBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF angleBounds = new RectF();
    private final float density;
    private final float jointRadius;

    @Nullable
    private Pose pose;

    public PoseOverlayView(Context context) {
        this(context, null);
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        jointRadius = 5.0f * density;

        linePaint.setColor(ContextCompat.getColor(context, R.color.pose_accent));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(4.0f * density);

        jointPaint.setColor(ContextCompat.getColor(context, R.color.pose_joint));
        jointPaint.setStyle(Paint.Style.FILL);

        angleTextPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        angleTextPaint.setTextAlign(Paint.Align.CENTER);
        angleTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14.0f,
                getResources().getDisplayMetrics()
        ));
        angleTextPaint.setFakeBoldText(true);

        angleBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.angle_background));
        angleBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void setPose(@Nullable Pose newPose) {
        pose = newPose == null || newPose.getAllPoseLandmarks().isEmpty() ? null : newPose;
        invalidate();
    }

    public void clear() {
        setPose(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Pose currentPose = pose;
        if (currentPose == null) {
            return;
        }

        for (int[] connection : CONNECTIONS) {
            drawConnection(canvas, currentPose, connection[0], connection[1]);
        }

        for (int landmarkType : BODY_LANDMARKS) {
            PoseLandmark landmark = currentPose.getPoseLandmark(landmarkType);
            if (isVisible(landmark)) {
                PointF position = landmark.getPosition();
                canvas.drawCircle(position.x, position.y, jointRadius, jointPaint);
            }
        }

        drawElbowAngle(
                canvas,
                currentPose,
                PoseLandmark.LEFT_SHOULDER,
                PoseLandmark.LEFT_ELBOW,
                PoseLandmark.LEFT_WRIST
        );
        drawElbowAngle(
                canvas,
                currentPose,
                PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.RIGHT_WRIST
        );
    }

    private void drawConnection(Canvas canvas, Pose currentPose, int startType, int endType) {
        PoseLandmark start = currentPose.getPoseLandmark(startType);
        PoseLandmark end = currentPose.getPoseLandmark(endType);
        if (!isVisible(start) || !isVisible(end)) {
            return;
        }

        PointF startPosition = start.getPosition();
        PointF endPosition = end.getPosition();
        canvas.drawLine(
                startPosition.x,
                startPosition.y,
                endPosition.x,
                endPosition.y,
                linePaint
        );
    }

    private void drawElbowAngle(
            Canvas canvas,
            Pose currentPose,
            int shoulderType,
            int elbowType,
            int wristType
    ) {
        PoseLandmark shoulder = currentPose.getPoseLandmark(shoulderType);
        PoseLandmark elbow = currentPose.getPoseLandmark(elbowType);
        PoseLandmark wrist = currentPose.getPoseLandmark(wristType);
        if (!isVisible(shoulder) || !isVisible(elbow) || !isVisible(wrist)) {
            return;
        }

        double angle = PoseMath.angleDegrees(
                toVector(shoulder.getPosition3D()),
                toVector(elbow.getPosition3D()),
                toVector(wrist.getPosition3D())
        );
        if (!Double.isFinite(angle)) {
            return;
        }

        PointF elbowPosition = elbow.getPosition();
        drawAngleLabel(canvas, elbowPosition, Math.round(angle) + "\u00b0");
    }

    private void drawAngleLabel(Canvas canvas, PointF anchor, String label) {
        float horizontalPadding = 8.0f * density;
        float verticalPadding = 5.0f * density;
        float radius = 8.0f * density;
        float textWidth = angleTextPaint.measureText(label);
        Paint.FontMetrics metrics = angleTextPaint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float halfWidth = textWidth / 2.0f + horizontalPadding;
        float halfHeight = textHeight / 2.0f + verticalPadding;

        float centerX = Math.max(halfWidth, Math.min(getWidth() - halfWidth, anchor.x));
        float preferredCenterY = anchor.y - 20.0f * density;
        float centerY = Math.max(
                halfHeight,
                Math.min(getHeight() - halfHeight, preferredCenterY)
        );
        angleBounds.set(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight
        );
        canvas.drawRoundRect(angleBounds, radius, radius, angleBackgroundPaint);
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2.0f;
        canvas.drawText(label, centerX, baseline, angleTextPaint);
    }

    private static boolean isVisible(@Nullable PoseLandmark landmark) {
        return landmark != null && landmark.getInFrameLikelihood() >= MIN_LANDMARK_LIKELIHOOD;
    }

    private static PoseMath.Vector3 toVector(PointF3D point) {
        return new PoseMath.Vector3(point.getX(), point.getY(), point.getZ());
    }
}
