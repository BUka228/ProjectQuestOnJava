package com.example.projectquestonjava.feature.gamification.presentation.ui_elements;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.projectquestonjava.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WateringEffectView extends View {

    private Paint particlePaint;
    private List<Particle> particles;
    private Random random = new Random();
    private ValueAnimator overlayAnimator;
    private boolean effectActive = false;
    private int particleCount = 70; // Увеличим для лучшего эффекта
    private int particleDuration = 1000; // мс

    private static class Particle {
        float x, y;
        float startX, startY; // Начальные относительные координаты
        float radius;
        float alpha;
        float progress; // 0 to 1
        long startTime;
        long duration;
        float horizontalSpeed; // Для небольшого горизонтального дрейфа

        Particle(float viewWidth, float viewHeight, long baseDuration, Random random) {
            this.startX = random.nextFloat();
            this.startY = random.nextFloat() * 0.8f; // Капли появляются в верхней 80% экрана
            this.x = startX * viewWidth;
            this.y = startY * viewHeight;
            this.radius = random.nextFloat() * dpToPx(3) + dpToPx(2); // Размер капли
            this.alpha = 0f;
            this.progress = 0f;
            this.duration = baseDuration + random.nextInt((int) (baseDuration * 0.5f)) - (int) (baseDuration * 0.2f);
            this.startTime = System.currentTimeMillis() + random.nextInt((int) (baseDuration / 2));
            this.horizontalSpeed = (random.nextFloat() - 0.5f) * dpToPx(0);
        }

        void update(long currentTime, float viewHeight) {
            if (currentTime < startTime) {
                progress = 0;
                alpha = 0;
                return;
            }
            progress = Math.min(1f, (float) (currentTime - startTime) / duration);

            // Анимация альфа: появляется (0 -> 0.5) и исчезает (0.5 -> 1)
            float alphaProgress = (progress < 0.5f) ? progress * 2f : (1f - progress) * 2f;
            alpha = Math.min(1f, alphaProgress * 0.8f); // Макс альфа 0.8

            // Движение вниз
            y = (startY * viewHeight) + (progress * viewHeight * 0.5f); // Падают на 50% высоты
            x += horizontalSpeed; // Горизонтальный дрейф
        }
    }

    public WateringEffectView(Context context) {
        super(context);
        init();
    }

    public WateringEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WateringEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int dropColor = ContextCompat.getColor(getContext(), R.color.primaryLight); // Или ваш цвет
        particlePaint.setColor(dropColor);
        particles = new ArrayList<>();

        // Аниматор для плавного исчезновения всего View
        overlayAnimator = ValueAnimator.ofFloat(1f, 0f);
        overlayAnimator.setDuration(400); // Длительность исчезновения
        overlayAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        overlayAnimator.addUpdateListener(animation -> {
            setAlpha((Float) animation.getAnimatedValue());
            if ((Float) animation.getAnimatedValue() == 0f) {
                setVisibility(GONE); // Скрываем после завершения
            }
        });
    }

    public void startEffect() {
        if (getWidth() == 0 || getHeight() == 0) {
            // Если View еще не отрисован, отложим запуск
            post(this::startEffectInternal);
            return;
        }
        startEffectInternal();
    }

    private void startEffectInternal() {
        effectActive = true;
        particles.clear();
        for (int i = 0; i < particleCount; i++) {
            particles.add(new Particle(getWidth(), getHeight(), particleDuration, random));
        }
        setAlpha(1f);
        setVisibility(VISIBLE);
        invalidate(); // Запускаем перерисовку

        // Аниматор для обновления частиц
        ValueAnimator particleUpdater = ValueAnimator.ofFloat(0f, 1f);
        particleUpdater.setDuration(particleDuration + particleDuration / 2); // Общая длительность эффекта
        particleUpdater.setInterpolator(new LinearInterpolator());
        particleUpdater.addUpdateListener(animation -> invalidate());
        particleUpdater.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                effectActive = false;
                // Запускаем анимацию исчезновения самого View
                overlayAnimator.setStartDelay(particleDuration / 3); // Небольшая задержка перед исчезновением
                overlayAnimator.start();
            }
        });
        particleUpdater.start();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!effectActive || particles.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        for (Particle particle : particles) {
            particle.update(currentTime, getHeight());
            if (particle.alpha > 0) {
                particlePaint.setAlpha((int) (particle.alpha * 255));
                // Рисуем овал (каплю)
                canvas.drawOval(
                        particle.x - particle.radius * 0.7f,
                        particle.y - particle.radius,
                        particle.x + particle.radius * 0.7f,
                        particle.y + particle.radius,
                        particlePaint
                );
            }
        }
    }

    private static int dpToPx(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}