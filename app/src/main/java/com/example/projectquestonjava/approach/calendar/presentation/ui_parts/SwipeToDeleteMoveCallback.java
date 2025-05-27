package com.example.projectquestonjava.approach.calendar.presentation.ui_parts; // Или другой подходящий пакет

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import android.content.res.ColorStateList; // Для Tinting

public class SwipeToDeleteMoveCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        void onTaskDeleteRequested(int position);
        void onTaskMoveRequested(int position);
    }

    private final SwipeListener listener;
    private final Context context;
    private final Drawable deleteBackground;
    private final Drawable moveBackground;
    private final Drawable deleteIcon;
    private final Drawable moveIcon;

    public SwipeToDeleteMoveCallback(Context context, SwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.listener = listener;

        // Загружаем drawable для фона
        deleteBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.swipe_action_delete));
        moveBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.swipe_action_move));

        // Загружаем иконки
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.delete);
        moveIcon = ContextCompat.getDrawable(context, R.drawable.edit_calendar); // Иконка для перемещения
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Не поддерживаем drag & drop
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            if (direction == ItemTouchHelper.LEFT) {
                listener.onTaskDeleteRequested(position);
            } else if (direction == ItemTouchHelper.RIGHT) {
                listener.onTaskMoveRequested(position);
            }
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        View swipeActionContentView = LayoutInflater.from(context).inflate(R.layout.view_swipe_action_content, null);
        ImageView iconView = swipeActionContentView.findViewById(R.id.imageView_swipe_content_icon);
        TextView textView = swipeActionContentView.findViewById(R.id.textView_swipe_content_text);
        LinearLayout rootContentLayout = swipeActionContentView.findViewById(R.id.layout_swipe_content_root);


        int itemHeight = itemView.getBottom() - itemView.getTop();
        int itemWidth = itemView.getWidth();

        if (dX > 0) { // Свайп вправо (Переместить)
            moveBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
            moveBackground.draw(c);

            iconView.setImageDrawable(moveIcon);
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(Color.WHITE));
            textView.setText("Переместить");
            textView.setTextColor(Color.WHITE);

            // Позиционирование иконки и текста
            int contentHeight = itemHeight; // Используем высоту элемента
            int contentWidth = (int) (itemWidth * 0.8); // Занимаем 80% ширины
            swipeActionContentView.measure(
                    View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY)
            );
            int left = itemView.getLeft() + context.getResources().getDimensionPixelSize(R.dimen.padding_large);
            int top = itemView.getTop() + (itemHeight - swipeActionContentView.getMeasuredHeight()) / 2;
            int right = left + swipeActionContentView.getMeasuredWidth();
            int bottom = top + swipeActionContentView.getMeasuredHeight();
            swipeActionContentView.layout(left, top, right, bottom);


        } else if (dX < 0) { // Свайп влево (Удалить)
            deleteBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            deleteBackground.draw(c);

            iconView.setImageDrawable(deleteIcon);
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(Color.WHITE));
            textView.setText("Удалить");
            textView.setTextColor(Color.WHITE);

            int contentHeight = itemHeight;
            int contentWidth = (int) (itemWidth * 0.8);
            swipeActionContentView.measure(
                    View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY)
            );
            // Позиционируем справа
            int right = itemView.getRight() - context.getResources().getDimensionPixelSize(R.dimen.padding_large);
            int top = itemView.getTop() + (itemHeight - swipeActionContentView.getMeasuredHeight()) / 2;
            int left = right - swipeActionContentView.getMeasuredWidth();
            int bottom = top + swipeActionContentView.getMeasuredHeight();
            swipeActionContentView.layout(left, top, right, bottom);

        } else { // Нет свайпа
            // Ничего не рисуем поверх
        }
        if (Math.abs(dX) > 0) { // Рисуем только если есть сдвиг
            c.save();
            // Устанавливаем правильное смещение для отрисовки контента
            if (dX > 0) {
                c.translate(itemView.getLeft() + context.getResources().getDimensionPixelSize(R.dimen.padding_large), itemView.getTop() + (itemHeight - swipeActionContentView.getMeasuredHeight()) / 2f);
            } else {
                c.translate(itemView.getRight() - swipeActionContentView.getMeasuredWidth() - context.getResources().getDimensionPixelSize(R.dimen.padding_large), itemView.getTop() + (itemHeight - swipeActionContentView.getMeasuredHeight()) / 2f);
            }
            swipeActionContentView.draw(c);
            c.restore();
        }


        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}