package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;

public class SwipeToDeleteMoveCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        void onTaskDeleteRequested(int position);
        void onTaskMoveRequested(int position);
    }

    private final SwipeListener listener;
    private final Context context;
    private final Drawable deleteIcon;
    private final Drawable moveIcon;
    private final float cornerRadius;

    public SwipeToDeleteMoveCallback(Context context, SwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.listener = listener;

        deleteIcon = ContextCompat.getDrawable(context, R.drawable.delete);
        moveIcon = ContextCompat.getDrawable(context, R.drawable.edit_calendar);
        this.cornerRadius = context.getResources().getDimension(R.dimen.card_corner_radius_large);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
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

        View swipeBackgroundContentView = LayoutInflater.from(itemView.getContext())
                .inflate(R.layout.view_swipe_background_dashboard, (ViewGroup) itemView.getParent(), false);
        LinearLayout startActionLayout = swipeBackgroundContentView.findViewById(R.id.layout_swipe_action_start_to_end);
        ImageView startActionIcon = swipeBackgroundContentView.findViewById(R.id.imageView_swipe_action_start);
        TextView startActionText = swipeBackgroundContentView.findViewById(R.id.textView_swipe_action_start);
        LinearLayout endActionLayout = swipeBackgroundContentView.findViewById(R.id.layout_swipe_action_end_to_start);
        ImageView endActionIcon = swipeBackgroundContentView.findViewById(R.id.imageView_swipe_action_end);
        TextView endActionText = swipeBackgroundContentView.findViewById(R.id.textView_swipe_action_end);


        int itemHeight = itemView.getBottom() - itemView.getTop();
        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setCornerRadius(cornerRadius);
        int backgroundColor = Color.TRANSPARENT;

        if (dX > 0) { // Свайп вправо (Переместить)
            backgroundColor = ContextCompat.getColor(context, R.color.swipe_action_move); // Зеленый
            startActionText.setText("Переместить");
            startActionIcon.setImageDrawable(moveIcon);

            startActionLayout.setVisibility(View.VISIBLE);
            endActionLayout.setVisibility(View.GONE);
            startActionText.setTextColor(Color.WHITE);
            ImageViewCompat.setImageTintList(startActionIcon, ColorStateList.valueOf(Color.WHITE));

            backgroundDrawable.setColor(backgroundColor);
            backgroundDrawable.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());

        } else if (dX < 0) { // Свайп влево (Удалить)
            backgroundColor = ContextCompat.getColor(context, R.color.swipe_action_delete); // Красный
            endActionText.setText("Удалить");
            endActionIcon.setImageDrawable(deleteIcon);

            startActionLayout.setVisibility(View.GONE);
            endActionLayout.setVisibility(View.VISIBLE);
            endActionText.setTextColor(Color.WHITE);
            ImageViewCompat.setImageTintList(endActionIcon, ColorStateList.valueOf(Color.WHITE));

            backgroundDrawable.setColor(backgroundColor);
            backgroundDrawable.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else {
            startActionLayout.setVisibility(View.GONE);
            endActionLayout.setVisibility(View.GONE);
            backgroundDrawable.setColor(Color.TRANSPARENT);
            backgroundDrawable.setBounds(0,0,0,0);
        }

        backgroundDrawable.draw(c); // Рисуем фон

        // Рисуем контент (иконка + текст) поверх фона
        if (Math.abs(dX) > 0) {
            int contentWidth = itemView.getWidth();
            int contentHeight = itemView.getHeight();
            swipeBackgroundContentView.measure(
                    View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY)
            );
            swipeBackgroundContentView.layout(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());

            c.save();
            if (dX > 0) { // Свайп вправо
                c.clipRect(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + dX, itemView.getBottom());
                // Позиционируем startActionLayout (из view_swipe_background_dashboard)
                startActionLayout.setAlpha(Math.min(1f, Math.abs(dX) / (float)(startActionLayout.getWidth() + context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe))));
                c.translate(itemView.getLeft() + context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe) , itemView.getTop() + (itemView.getHeight() - startActionLayout.getMeasuredHeight()) / 2f);
                startActionLayout.draw(c);
                // Восстанавливаем канвас после отрисовки смещенного элемента
                c.translate(-(itemView.getLeft() + context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe)), -(itemView.getTop() + (itemView.getHeight() - startActionLayout.getMeasuredHeight()) / 2f));

            } else { // Свайп влево
                c.clipRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                // Позиционируем endActionLayout
                endActionLayout.setAlpha(Math.min(1f, Math.abs(dX) / (float)(endActionLayout.getWidth() + context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe))));
                c.translate(itemView.getRight() - endActionLayout.getMeasuredWidth() - context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe) , itemView.getTop() + (itemView.getHeight() - endActionLayout.getMeasuredHeight()) / 2f);
                endActionLayout.draw(c);
                // Восстанавливаем канвас
                c.translate(-(itemView.getRight() - endActionLayout.getMeasuredWidth() - context.getResources().getDimensionPixelSize(R.dimen.padding_large_for_swipe)) , -(itemView.getTop() + (itemView.getHeight() - endActionLayout.getMeasuredHeight()) / 2f));
            }
            c.restore();
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}