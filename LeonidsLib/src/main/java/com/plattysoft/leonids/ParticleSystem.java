package com.plattysoft.leonids;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.plattysoft.leonids.initializers.AccelerationInitializer;
import com.plattysoft.leonids.initializers.ParticleInitializer;
import com.plattysoft.leonids.initializers.RotationInitializer;
import com.plattysoft.leonids.initializers.RotationSpeedInitializer;
import com.plattysoft.leonids.initializers.ScaleInitializer;
import com.plattysoft.leonids.initializers.SpeedModuleAndRangeInitializer;
import com.plattysoft.leonids.initializers.SpeeddByComponentsInitializer;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.modifiers.ParticleModifier;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ParticleSystem {

    private static long TIMER_TASK_INTERVAL = 33; // Default 30fps
    private final ArrayList<Particle> mActiveParticles = new ArrayList<>();
    private final ParticleTimerTask mTimerTask = new ParticleTimerTask(this);
    private ViewGroup mParentView;
    private final int mMaxParticles;
    private final Random mRandom;
    private ParticleField mDrawingView;
    private final List<Particle> mParticles;
    private final long mTimeToLive;
    private long mCurrentTime = 0;
    private float mParticlesPerMillisecond;
    private int mActivatedParticles;
    private long mEmittingTime;
    private final List<ParticleModifier> mModifiers;
    private final List<ParticleInitializer> mInitializers;
    private ValueAnimator mAnimator;
    private Timer mTimer;
    private final float mDpToPxScale;
    private final int[] mParentLocation;

    private int mEmitterXMin;
    private int mEmitterXMax;
    private int mEmitterYMin;
    private int mEmitterYMax;

    private final Random random = new Random();

    /**
     * Sets the frames per second of <em>ALL</em> ParticleSystems
     *
     * @param fps the desired frames per second
     */
    public static void setFPS(double fps) {
        TIMER_TASK_INTERVAL = Math.round(1000 / fps);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a             The parent activity
     * @param maxParticles  The maximum number of particles
     * @param drawableResId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, int drawableResId, long timeToLive) {
        this(a, maxParticles, new int[]{drawableResId}, timeToLive, android.R.id.content);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a              The parent activity
     * @param maxParticles   The maximum number of particles
     * @param drawableResIds The drawable resources to use as particles (supports Bitmaps and Animations)
     * @param timeToLive     The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, int[] drawableResIds, long timeToLive) {
        this(a, maxParticles, drawableResIds, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives a Drawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param drawables    The drawables to use as particles (supports Bitmaps and Animations)
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, Drawable[] drawables, long timeToLive) {
        this(a, maxParticles, drawables, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives a Bitmap
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param bitmaps      The bitmaps to use as particles
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, Bitmap[] bitmaps, long timeToLive) {
        this(a, maxParticles, bitmaps, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives an AnimationDrawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param animations   The animations to use as particles
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, AnimationDrawable[] animations, long timeToLive) {
        this(a, maxParticles, animations, timeToLive, android.R.id.content);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a             The parent activity
     * @param maxParticles  The maximum number of particles
     * @param drawableResId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     * @param parentViewId  The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, int drawableResId, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, new int[]{drawableResId}, timeToLive);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a              The parent activity
     * @param maxParticles   The maximum number of particles
     * @param drawableResIds The drawable resources to use as particles (supports Bitmaps and Animations)
     * @param timeToLive     The time to live for the particles
     * @param parentViewId   The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, int[] drawableResIds, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, drawableResIds, timeToLive);
    }

    /**
     * Utility constructor that receives a Drawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param drawables    The drawables to use as particles (supports Bitmaps and Animations)
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, Drawable[] drawables, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, drawables, timeToLive);
    }

    /**
     * Utility constructor that receives a Bitmap
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param bitmaps      The bitmaps to use as particles
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, Bitmap[] bitmaps, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, timeToLive);
        for (int i = 0; i < mMaxParticles; i++) {
            int randomIndex = random.nextInt(bitmaps.length);
            Bitmap bitmap = bitmaps[randomIndex];
            mParticles.add(new Particle(bitmap));
        }
    }

    /**
     * Utility constructor that receives an AnimationDrawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param animations   The animations to use as particles
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, AnimationDrawable[] animations, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, timeToLive);
        // Create the particles
        for (int i = 0; i < mMaxParticles; i++) {
            int randomIndex = random.nextInt(animations.length);
            AnimationDrawable animation = animations[randomIndex];
            mParticles.add(new AnimatedParticle(animation));
        }
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param parentView    The parent view group
     * @param drawableResId The drawable resource to use as particle
     * @param maxParticles  The maximum number of particles
     * @param timeToLive    The time to live for the particles
     */
    public ParticleSystem(ViewGroup parentView, int maxParticles, int drawableResId, long timeToLive) {
        this(parentView, maxParticles, timeToLive);
        setParticles(maxParticles, getDrawables(parentView.getContext(), new int[]{drawableResId}));
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param parentView     The parent view group
     * @param drawableResIds The drawable resources to use as particles
     * @param maxParticles   The maximum number of particles
     * @param timeToLive     The time to live for the particles
     */
    public ParticleSystem(ViewGroup parentView, int maxParticles, int[] drawableResIds, long timeToLive) {
        this(parentView, maxParticles, timeToLive);
        setParticles(maxParticles, getDrawables(parentView.getContext(), drawableResIds));
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param parentView   The parent view group
     * @param drawables    The drawables to use as particles
     * @param maxParticles The maximum number of particles
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(ViewGroup parentView, int maxParticles, Drawable[] drawables, long timeToLive) {
        this(parentView, maxParticles, timeToLive);
        setParticles(maxParticles, drawables);
    }

    private ParticleSystem(ViewGroup parentView, int maxParticles, long timeToLive) {
        mRandom = new Random();
        mParentLocation = new int[2];

        setParentViewGroup(parentView);

        mModifiers = new ArrayList<>();
        mInitializers = new ArrayList<>();

        mMaxParticles = maxParticles;
        // Create the particles

        mParticles = new ArrayList<>();
        mTimeToLive = timeToLive;

        DisplayMetrics displayMetrics = parentView.getContext().getResources().getDisplayMetrics();
        mDpToPxScale = (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Adds a modifier to the Particle system, it will be executed on each update.
     *
     * @param modifier modifier to be added to the ParticleSystem
     */
    public ParticleSystem addModifier(ParticleModifier modifier) {
        mModifiers.add(modifier);
        return this;
    }

    public ParticleSystem setSpeedRange(float speedMin, float speedMax) {
        mInitializers.add(new SpeedModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), 0, 360));
        return this;
    }

    /**
     * Initializes the speed range and angle range of emitted particles. Angles are in degrees
     * and non negative:
     * 0 meaning to the right, 90 to the bottom,... in clockwise orientation. Speed is non
     * negative and is described in pixels per millisecond.
     *
     * @param speedMin The minimum speed to emit particles.
     * @param speedMax The maximum speed to emit particles.
     * @param minAngle The minimum angle to emit particles in degrees.
     * @param maxAngle The maximum angle to emit particles in degrees.
     * @return This.
     */
    public ParticleSystem setSpeedModuleAndAngleRange(float speedMin, float speedMax, int minAngle, int maxAngle) {
        // else emitting from top (270°) to bottom (90°) range would not be possible if someone
        // entered minAngle = 270 and maxAngle=90 since the module would swap the values
        while (maxAngle < minAngle) {
            maxAngle += 360;
        }
        mInitializers.add(new SpeedModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the speed components ranges that particles will be emitted. Speeds are
     * measured in density pixels per millisecond.
     *
     * @param speedMinX The minimum speed in x direction.
     * @param speedMaxX The maximum speed in x direction.
     * @param speedMinY The minimum speed in y direction.
     * @param speedMaxY The maximum speed in y direction.
     * @return This.
     */
    public ParticleSystem setSpeedByComponentsRange(float speedMinX, float speedMaxX, float speedMinY, float speedMaxY) {
        mInitializers.add(new SpeeddByComponentsInitializer(dpToPx(speedMinX), dpToPx(speedMaxX),
                dpToPx(speedMinY), dpToPx(speedMaxY)));
        return this;
    }

    /**
     * Initializes the initial rotation range of emitted particles. The rotation angle is
     * measured in degrees with 0° being no rotation at all and 90° tilting the image to the right.
     *
     * @param minAngle The minimum tilt angle.
     * @param maxAngle The maximum tilt angle.
     * @return This.
     */
    public ParticleSystem setInitialRotationRange(int minAngle, int maxAngle) {
        mInitializers.add(new RotationInitializer(minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the scale range of emitted particles. Will scale the images around their
     * center multiplied with the given scaling factor.
     *
     * @param minScale The minimum scaling factor
     * @param maxScale The maximum scaling factor.
     * @return This.
     */
    public ParticleSystem setScaleRange(float minScale, float maxScale) {
        mInitializers.add(new ScaleInitializer(minScale, maxScale));
        return this;
    }

    /**
     * Initializes the rotation speed of emitted particles. Rotation speed is measured in degrees
     * per second.
     *
     * @param rotationSpeed The rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeed(float rotationSpeed) {
        mInitializers.add(new RotationSpeedInitializer(rotationSpeed, rotationSpeed));
        return this;
    }

    /**
     * Initializes the rotation speed range for emitted particles. The rotation speed is measured
     * in degrees per second and can be positive or negative.
     *
     * @param minRotationSpeed The minimum rotation speed.
     * @param maxRotationSpeed The maximum rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeedRange(float minRotationSpeed, float maxRotationSpeed) {
        mInitializers.add(new RotationSpeedInitializer(minRotationSpeed, maxRotationSpeed));
        return this;
    }

    /**
     * Initializes the acceleration range and angle range of emitted particles. The acceleration
     * components in x and y direction are controlled by the acceleration angle. The acceleration
     * is measured in density pixels per square millisecond. The angle is measured in degrees
     * with 0° pointing to the right and going clockwise.
     *
     * @param minAcceleration
     * @param maxAcceleration
     * @param minAngle
     * @param maxAngle
     * @return
     */
    public ParticleSystem setAccelerationModuleAndAndAngleRange(float minAcceleration, float maxAcceleration, int minAngle, int maxAngle) {
        mInitializers.add(new AccelerationInitializer(dpToPx(minAcceleration), dpToPx(maxAcceleration),
                minAngle, maxAngle));
        return this;
    }

    /**
     * Adds a custom initializer for emitted particles. The most common use case is the ability to
     * update the initializer in real-time instead of adding new ones ontop of the existing one.
     *
     * @param initializer The non-null initializer to add.
     * @return This.
     */
    public ParticleSystem addInitializer(ParticleInitializer initializer) {
        if (initializer != null) {
            mInitializers.add(initializer);
        }
        return this;
    }

    /**
     * Initializes the acceleration for emitted particles with the given angle. Acceleration is
     * measured in pixels per square millisecond. The angle is measured in degrees with 0°
     * meaning to the right and orientation being clockwise. The angle controls the acceleration
     * direction.
     *
     * @param acceleration The acceleration.
     * @param angle        The acceleration direction.
     * @return This.
     */
    public ParticleSystem setAcceleration(float acceleration, int angle) {
        mInitializers.add(new AccelerationInitializer(acceleration, acceleration, angle, angle));
        return this;
    }

    /**
     * Initializes the parent view group. This needs to be done before any other configuration or
     * emitting is done. Drawing will be done to a child that is added to this view. So this view
     * needs to allow displaying an arbitrary sized view on top of its other content.
     *
     * @param viewGroup The view group to use.
     * @return This.
     */
    public ParticleSystem setParentViewGroup(ViewGroup viewGroup) {
        mParentView = viewGroup;
        if (mParentView != null) {
            mParentView.getLocationInWindow(mParentLocation);
        }
        return this;
    }

    public ParticleSystem setStartTime(long time) {
        mCurrentTime = time;
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param millisecondsBeforeEnd fade out duration in milliseconds
     * @param interpolator          the interpolator for the fade out (default is linear)
     */
    public ParticleSystem setFadeOut(long millisecondsBeforeEnd, Interpolator interpolator) {
        mModifiers.add(new AlphaModifier(255, 0, mTimeToLive - millisecondsBeforeEnd, mTimeToLive, interpolator));
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param duration fade out duration in milliseconds
     */
    public ParticleSystem setFadeOut(long duration) {
        return setFadeOut(duration, new LinearInterpolator());
    }

    /**
     * Starts emitting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emitter            View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emittingTime       time the emitter will be emitting particles
     */
    public void emit(View emitter, int particlesPerSecond, int emittingTime) {
        emitWithGravity(emitter, Gravity.CENTER, particlesPerSecond, emittingTime);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond, int emittingTime) {
        configureEmitter(emitterX, emitterY);
        startEmitting(particlesPerSecond, emittingTime);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond) {
        configureEmitter(emitterX, emitterY);
        startEmitting(particlesPerSecond);
    }

    /**
     * Starts emitting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emitter            View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emit(View emitter, int particlesPerSecond) {
        // Setup emitter
        emitWithGravity(emitter, Gravity.CENTER, particlesPerSecond);
    }

    /**
     * Starts emitting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emitter            View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emitWithGravity(View emitter, int gravity, int particlesPerSecond) {
        // Setup emitter
        configureEmitter(emitter, gravity);
        startEmitting(particlesPerSecond);
    }

    /**
     * Starts emitting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emitter            View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emittingTime       time the emitter will be emitting particles
     */
    public void emitWithGravity(View emitter, int gravity, int particlesPerSecond, int emittingTime) {
        // Setup emitter
        configureEmitter(emitter, gravity);
        startEmitting(particlesPerSecond, emittingTime);
    }

    private void startEmitting(int particlesPerSecond) {
        mActivatedParticles = 0;
        mParticlesPerMillisecond = particlesPerSecond / 1000f;
        // Add a full size view to the parent view
        mDrawingView = new ParticleField(mParentView.getContext());
        mParentView.addView(mDrawingView);
        mEmittingTime = -1; // Meaning infinite
        mDrawingView.setParticles(mActiveParticles);
        updateParticlesBeforeStartTime(particlesPerSecond);
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 0, TIMER_TASK_INTERVAL);
    }

    private void startEmitting(int particlesPerSecond, int emittingTime) {
        mActivatedParticles = 0;
        mParticlesPerMillisecond = particlesPerSecond / 1000f;
        // Add a full size view to the parent view
        mDrawingView = new ParticleField(mParentView.getContext());
        mParentView.addView(mDrawingView);

        mDrawingView.setParticles(mActiveParticles);
        updateParticlesBeforeStartTime(particlesPerSecond);
        mEmittingTime = emittingTime;
        startAnimator(new LinearInterpolator(), emittingTime + mTimeToLive);
    }

    private void configureEmitter(int emitterX, int emitterY) {
        // We configure the emitter based on the window location to fix the offset of action bar if present
        mEmitterXMin = emitterX - mParentLocation[0];
        mEmitterXMax = mEmitterXMin;
        mEmitterYMin = emitterY - mParentLocation[1];
        mEmitterYMax = mEmitterYMin;
    }

    private void configureEmitter(View emitter, int gravity) {
        // It works with an emission range
        int[] location = new int[2];
        emitter.getLocationInWindow(location);

        // Check horizontal gravity and set range
        if (hasGravity(gravity, Gravity.LEFT)) {
            mEmitterXMin = location[0] - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else if (hasGravity(gravity, Gravity.RIGHT)) {
            mEmitterXMin = location[0] + emitter.getWidth() - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else if (hasGravity(gravity, Gravity.CENTER_HORIZONTAL)) {
            mEmitterXMin = location[0] + emitter.getWidth() / 2 - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else {
            // All the range
            mEmitterXMin = location[0] - mParentLocation[0];
            mEmitterXMax = location[0] + emitter.getWidth() - mParentLocation[0];
        }

        // Now, vertical gravity and range
        if (hasGravity(gravity, Gravity.TOP)) {
            mEmitterYMin = location[1] - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else if (hasGravity(gravity, Gravity.BOTTOM)) {
            mEmitterYMin = location[1] + emitter.getHeight() - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else if (hasGravity(gravity, Gravity.CENTER_VERTICAL)) {
            mEmitterYMin = location[1] + emitter.getHeight() / 2 - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else {
            // All the range
            mEmitterYMin = location[1] - mParentLocation[1];
            mEmitterYMax = location[1] + emitter.getHeight() - mParentLocation[1];
        }
    }

    public void updateEmitPoint(int emitterX, int emitterY) {
        configureEmitter(emitterX, emitterY);
    }

    public void updateEmitPoint(View emitter, int gravity) {
        configureEmitter(emitter, gravity);
    }

    /**
     * Launches particles in one Shot
     *
     * @param emitter      View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     */
    public void oneShot(View emitter, int numParticles) {
        oneShot(emitter, numParticles, new LinearInterpolator());
    }

    /**
     * Launches particles in one Shot using a special Interpolator
     *
     * @param emitter      View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     * @param interpolator the interpolator for the time
     */
    public void oneShot(View emitter, int numParticles, Interpolator interpolator) {
        configureEmitter(emitter, Gravity.CENTER);
        mActivatedParticles = 0;
        mEmittingTime = mTimeToLive;
        // We create particles based in the parameters
        for (int i = 0; i < numParticles && i < mMaxParticles; i++) {
            activateParticle(0);
        }
        // Add a full size view to the parent view
        mDrawingView = new ParticleField(mParentView.getContext());
        mParentView.addView(mDrawingView);
        mDrawingView.setParticles(mActiveParticles);
        // We start a property animator that will call us to do the update
        // Animate from 0 to timeToLiveMax
        startAnimator(interpolator, mTimeToLive);
    }

    /**
     * Stops emitting new particles, but will draw the existing ones until their timeToLive expire
     * For an cancellation and stop drawing of the particles, use cancel instead.
     */
    public void stopEmitting() {
        // The time to be emitting is the current time (as if it was a time-limited emitter
        mEmittingTime = mCurrentTime;
    }

    /**
     * Cancels the particle system and all the animations.
     * To stop emitting but animate until the end, use stopEmitting instead.
     */
    public void cancel() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            cleanupAnimation();
        }
    }

    private void setParticles(int maxParticles, Drawable[] drawables) {
        for (Drawable drawable : drawables) {
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animation = (AnimationDrawable) drawable;
                mParticles.add(new AnimatedParticle(animation));
            } else {
                Bitmap bitmap;
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                } else {
                    bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }
                mParticles.add(new Particle(bitmap));
            }
        }

        for (int i = 0; i < maxParticles / drawables.length; i++) {
            mParticles.addAll(new ArrayList<>(mParticles));
        }
        mParticles.addAll(new ArrayList<>(mParticles).subList(0, maxParticles % drawables.length));

        Collections.shuffle(mParticles);
    }


    private void startAnimator(Interpolator interpolator, long animnationTime) {
        mAnimator = ValueAnimator.ofInt(0, (int) animnationTime);
        mAnimator.setDuration(animnationTime);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int milliseconds = (Integer) animation.getAnimatedValue();
                onUpdate(milliseconds);
            }
        });
        mAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cleanupAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cleanupAnimation();
            }
        });
        mAnimator.setInterpolator(interpolator);
        mAnimator.start();
    }

    private void updateParticlesBeforeStartTime(int particlesPerSecond) {
        if (particlesPerSecond == 0) {
            return;
        }
        long currentTimeInMs = mCurrentTime / 1000;
        long framesCount = currentTimeInMs / particlesPerSecond;
        if (framesCount == 0) {
            return;
        }
        long frameTimeInMs = mCurrentTime / framesCount;
        for (int i = 1; i <= framesCount; i++) {
            onUpdate(frameTimeInMs * i + 1);
        }
    }

    private boolean hasGravity(int gravity, int gravityToCheck) {
        return (gravity & gravityToCheck) == gravityToCheck;
    }

    private void onUpdate(long milliseconds) {
        while (((mEmittingTime > 0 && milliseconds < mEmittingTime) || mEmittingTime == -1) && // This point should emit
                !mParticles.isEmpty() && // We have particles in the pool
                mActivatedParticles < mParticlesPerMillisecond * milliseconds) { // and we are under the number of particles that should be launched
            // Activate a new particle
            activateParticle(milliseconds);
        }
        synchronized (mActiveParticles) {
            for (int i = 0; i < mActiveParticles.size(); i++) {
                boolean active = mActiveParticles.get(i).update(milliseconds);
                if (!active) {
                    Particle p = mActiveParticles.remove(i);
                    i--; // Needed to keep the index at the right position
                    mParticles.add(p);
                }
            }
        }
        mDrawingView.postInvalidate();
    }

    private void cleanupAnimation() {
        mParentView.removeView(mDrawingView);
        mDrawingView = null;
        mParentView.postInvalidate();
        mParticles.addAll(mActiveParticles);
    }

    private void activateParticle(long delay) {
        Particle p = mParticles.remove(0);
        p.init();
        // Initialization goes before configuration, scale is required before can be configured properly
        for (int i = 0; i < mInitializers.size(); i++) {
            mInitializers.get(i).initParticle(p, mRandom);
        }
        int particleX = getFromRange(mEmitterXMin, mEmitterXMax);
        int particleY = getFromRange(mEmitterYMin, mEmitterYMax);
        p.configure(mTimeToLive, particleX, particleY);
        p.activate(delay, mModifiers);
        mActiveParticles.add(p);
        mActivatedParticles++;
    }

    private int getFromRange(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        if (minValue < maxValue) {
            return mRandom.nextInt(maxValue - minValue) + minValue;
        } else {
            return mRandom.nextInt(minValue - maxValue) + maxValue;
        }
    }

    private Drawable[] getDrawables(Context context, int[] drawableResIds) {
        Drawable[] drawables = new Drawable[drawableResIds.length];
        for (int i = 0; i < drawableResIds.length; i++) {
            drawables[i] = context.getResources().getDrawable(drawableResIds[i]);
        }
        return drawables;
    }

    private float dpToPx(float dp) {
        return dp * mDpToPxScale;
    }

    private static class ParticleTimerTask extends TimerTask {

        private final WeakReference<ParticleSystem> mPs;

        public ParticleTimerTask(ParticleSystem ps) {
            mPs = new WeakReference<>(ps);
        }

        @Override
        public void run() {
            if (mPs.get() != null) {
                ParticleSystem ps = mPs.get();
                ps.onUpdate(ps.mCurrentTime);
                ps.mCurrentTime += TIMER_TASK_INTERVAL;
            }
        }
    }
}
