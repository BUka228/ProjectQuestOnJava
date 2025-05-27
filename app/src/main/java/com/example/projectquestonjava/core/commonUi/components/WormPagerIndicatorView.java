package com.example.projectquestonjava.core.commonUi.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.projectquestonjava.R; // Для доступа к R.styleable

public class WormPagerIndicatorView extends View {

    private int pageCount = 0;
    private int currentPage = 0;
    private float pageOffset = 0f;

    private Paint activePaint;
    private Paint inactivePaint;

    private float indicatorSize;
    private float spacing;
    private int activeColor;
    private int inactiveColor;

    private RectF wormRect = new RectF();
    private ValueAnimator wormAnimator;


    public WormPagerIndicatorView(Context context) {
        this(context, null);
    }

    public WormPagerIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WormPagerIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.WormPagerIndicatorView, // Нужно создать attrs.xml
                0, 0);

        try {
            indicatorSize = a.getDimensionPixelSize(R.styleable.WormPagerIndicatorView_wpi_indicatorSize, dpToPx(8));
            spacing = a.getDimensionPixelSize(R.styleable.WormPagerIndicatorView_wpi_spacing, dpToPx(12));
            activeColor = a.getColor(R.styleable.WormPagerIndicatorView_wpi_activeColor, ContextCompat.getColor(context, R.color.primaryLight)); // Используйте цвета из вашей темы
            inactiveColor = a.getColor(R.styleable.WormPagerIndicatorView_wpi_inactiveColor, ContextCompat.getColor(context, R.color.surfaceVariantLight));
            pageCount = a.getInt(R.styleable.WormPagerIndicatorView_wpi_pageCount, 0);
        } finally {
            a.recycle();
        }

        activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activePaint.setColor(activeColor);

        inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inactivePaint.setColor(inactiveColor);

        wormAnimator = ValueAnimator.ofFloat(0f, 1f);
        wormAnimator.setDuration(300); // Длительность анимации "червячка"
        wormAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        wormAnimator.addUpdateListener(animation -> {
            // Здесь можно обновлять свойства для анимации, если нужно
            invalidate();
        });
    }

    public void setPageCount(int pageCount) {
        if (this.pageCount != pageCount) {
            this.pageCount = pageCount;
            requestLayout(); // Пересчитать размеры, если количество изменилось
            invalidate();
        }
    }

    public void setCurrentPage(int currentPage) {
        if (this.currentPage != currentPage) {
            this.currentPage = currentPage;
            // Плавная анимация к новой позиции
            // (здесь может быть сложнее, если pageOffset тоже анимируется)
            invalidate();
        }
    }

    public void setPageOffset(float pageOffset) {
        if (this.pageOffset != pageOffset) {
            this.pageOffset = pageOffset;
            invalidate();
        }
    }

    // Метод для подключения к ViewPager2
    public void attachToViewPager(ViewPager2 viewPager) {
        if (viewPager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager2 has no adapter set.");
        }
        setPageCount(viewPager.getAdapter().getItemCount());
        setCurrentPage(viewPager.getCurrentItem());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setCurrentPage(position);
                setPageOffset(positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                // setCurrentPage(position); // Уже вызывается в onPageScrolled
                // setPageOffset(0f);
            }
        });
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) ((pageCount * indicatorSize) + (Math.max(0, pageCount - 1) * spacing));
        int desiredHeight = (int) indicatorSize;

        int width = resolveSize(desiredWidth + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        int height = resolveSize(desiredHeight + getPaddingTop() + getPaddingBottom(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pageCount <= 0) return;

        float totalWidthOfIndicators = (pageCount * indicatorSize) + (Math.max(0, pageCount - 1) * spacing);
        float startX = (getWidth() - totalWidthOfIndicators) / 2f; // Центрируем индикаторы
        float centerY = getHeight() / 2f;

        // Рисуем неактивные точки
        for (int i = 0; i < pageCount; i++) {
            float cx = startX + i * (indicatorSize + spacing) + indicatorSize / 2;
            canvas.drawCircle(cx, centerY, indicatorSize / 2, inactivePaint);
        }

        // Рисуем активный "червячок"
        float indicatorUnitWidth = indicatorSize + spacing;
        float currentOverallOffset = currentPage * indicatorUnitWidth + pageOffset * indicatorUnitWidth;

        float wormStart = startX + currentOverallOffset;
        // Логика растяжения червячка
        float wormWidthFactor = Math.abs(pageOffset) * 2f; // 0 при 0, 1 при 0.5, 0 при 1
        float additionalWidth = spacing * Math.min(1f, wormWidthFactor); // Увеличиваем на spacing в пике
        float wormCurrentWidth = indicatorSize + additionalWidth;


        // Если смещение идет влево (pageOffset отрицательный), то червячок должен начинаться раньше
        // и его ширина должна учитывать это смещение, чтобы "поглотить" предыдущую точку.
        // Аналогично для смещения вправо.

        // Упрощенная логика для начального смещения и ширины:
        // Более точная потребует вычисления `left` и `right` для RectF на основе `pageOffset`.

        // Начальная позиция левого края червячка
        float wormLeft = startX + currentOverallOffset;

        // Если pageOffset > 0, то червячок растягивается вправо
        // Если pageOffset < 0, то червячок "начинается" раньше и растягивается влево

        // Этот расчет сложен и требует точного повторения логики animateDpAsState из Compose
        // Для простоты, нарисуем капсулу, которая двигается:
        wormRect.set(wormLeft, centerY - indicatorSize / 2, wormLeft + wormCurrentWidth, centerY + indicatorSize / 2);

        // Рисуем скругленный прямоугольник (капсулу)
        float cornerRadius = indicatorSize / 2;
        canvas.drawRoundRect(wormRect, cornerRadius, cornerRadius, activePaint);

    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}