package com.example.projectquestonjava.feature.pomodoro.presentation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.utils.RingtoneItem;

import java.util.List;
import java.util.Objects;

public class RingtoneDropdownAdapter extends ArrayAdapter<RingtoneItem> {

    private final LayoutInflater inflater;
    private final List<RingtoneItem> items;
    private final RingtoneInteractionListener listener;
    private String playingUri = null;
    private String selectedUri = null;

    public interface RingtoneInteractionListener {
        void onPreviewClicked(RingtoneItem item, boolean isPlaying);
        void onRemoveClicked(RingtoneItem item);
    }

    public RingtoneDropdownAdapter(@NonNull Context context, @NonNull List<RingtoneItem> items,
                                   @NonNull RingtoneInteractionListener listener, @Nullable String currentlySelectedUri) {
        super(context, R.layout.item_ringtone_dropdown, items); // item_ringtone_dropdown - макет для элемента списка
        this.inflater = LayoutInflater.from(context);
        this.items = items;
        this.listener = listener;
        this.selectedUri = currentlySelectedUri;
    }

    public void setPlayingUri(@Nullable String uri) {
        this.playingUri = uri;
        notifyDataSetChanged();
    }

    public void setSelectedUri(@Nullable String uri) {
        this.selectedUri = uri;
        // Можно не вызывать notifyDataSetChanged, т.к. AutoCompleteTextView сам обновит выбранный текст
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Этот метод для отображения выбранного элемента в AutoCompleteTextView (когда он не раскрыт)
        // Обычно используется стандартный layout, но можно и кастомный
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }
        TextView textView = view.findViewById(android.R.id.text1);
        RingtoneItem item = getItem(position);
        if (item != null) {
            textView.setText(item.title());
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Этот метод для отображения элементов в раскрывающемся списке
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = inflater.inflate(R.layout.item_ringtone_dropdown, parent, false);
            holder = new ViewHolder();
            holder.textViewTitle = view.findViewById(R.id.textView_ringtone_title);
            holder.buttonPreview = view.findViewById(R.id.button_preview_ringtone);
            holder.buttonRemove = view.findViewById(R.id.button_remove_ringtone);
            holder.imageViewSelectedCheck = view.findViewById(R.id.imageView_ringtone_selected_check);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        RingtoneItem item = getItem(position);
        if (item != null) {
            holder.textViewTitle.setText(item.title());

            boolean isCurrentlyPlaying = Objects.equals(item.uri(), playingUri);
            holder.buttonPreview.setImageResource(isCurrentlyPlaying ? R.drawable.stop : R.drawable.play_arrow);
            holder.buttonPreview.setContentDescription(isCurrentlyPlaying ? "Остановить" : "Прослушать");

            if (item.uri() != null) {
                holder.buttonPreview.setVisibility(View.VISIBLE);
                holder.buttonPreview.setOnClickListener(v -> listener.onPreviewClicked(item, isCurrentlyPlaying));
            } else { // "Без звука"
                holder.buttonPreview.setVisibility(View.GONE);
            }

            if (item.isCustom() && item.uri() != null) {
                holder.buttonRemove.setVisibility(View.VISIBLE);
                holder.buttonRemove.setOnClickListener(v -> listener.onRemoveClicked(item));
            } else {
                holder.buttonRemove.setVisibility(View.GONE);
            }

            holder.imageViewSelectedCheck.setVisibility(Objects.equals(item.uri(), selectedUri) ? View.VISIBLE : View.INVISIBLE);
        }
        return view;
    }

    private static class ViewHolder {
        TextView textViewTitle;
        ImageButton buttonPreview;
        ImageButton buttonRemove;
        ImageView imageViewSelectedCheck;
    }
}