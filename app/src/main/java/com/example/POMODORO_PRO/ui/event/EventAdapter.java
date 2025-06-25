package com.example.POMODORO_PRO.ui.event;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.databinding.EventItemBinding;
import com.google.api.services.calendar.model.Event;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class EventAdapter extends ListAdapter<Event, EventAdapter.EventViewHolder> {
    private final EventClickListener clickListener;
    private int lastPosition = -1;
    private final int[] eventColors = {
            Color.parseColor("#4285F4"), // Google Blue
            Color.parseColor("#EA4335"), // Google Red
            Color.parseColor("#FBBC05"), // Google Yellow
            Color.parseColor("#34A853"), // Google Green
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#795548")  // Brown
    };
    private final Random random = new Random();

    public interface EventClickListener {
        void onUpdateClick(Event event);
        void onDeleteClick(String eventId);
    }

    public EventAdapter(EventClickListener clickListener) {
        super(new EventDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        EventItemBinding binding = EventItemBinding.inflate(inflater, parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.bind(event, clickListener);

        // Set a consistent color based on event ID
        int colorIndex = Math.abs(event.getId().hashCode()) % eventColors.length;
        holder.binding.eventColorIndicator.setBackgroundColor(eventColors[colorIndex]);

        // Add animation for items
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.item_animation_fall_down);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull EventViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final EventItemBinding binding;

        EventViewHolder(EventItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Event event, EventClickListener clickListener) {
            binding.eventTitle.setText(event.getSummary());

            String dateTimeStr = "";
            if (event.getStart() != null) {
                if (event.getStart().getDateTime() != null) {
                    // Example: 2024-06-08T10:00:00
                    String dateTime = event.getStart().getDateTime().toStringRfc3339();
                    dateTimeStr = formatDateTime(dateTime);
                } else if (event.getStart().getDate() != null) {
                    String date = event.getStart().getDate().toStringRfc3339();
                    dateTimeStr = formatDate(date);
                }
            }
            binding.eventTime.setText(dateTimeStr);

            binding.btnEdit.setOnClickListener(v -> {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    clickListener.onUpdateClick(event);
                }).start();
            });

            binding.btnDelete.setOnClickListener(v -> {
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    clickListener.onDeleteClick(event.getId());
                }).start();
            });

            // Add ripple effect to the entire card
            binding.getRoot().setOnClickListener(v -> {
                clickListener.onUpdateClick(event);
            });
        }

        private String formatDateTime(String dateTime) {
            // Parse and format as "08 Jun 10:00"
            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
                return output.format(input.parse(dateTime.substring(0, 19)));
            } catch (Exception e) {
                return dateTime;
            }
        }

        private String formatDate(String date) {
            // Parse and format as "08 Jun (All day)"
            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("dd MMM", Locale.getDefault());
                return output.format(input.parse(date)) + " (All day)";
            } catch (Exception e) {
                return date;
            }
        }
    }

    static class EventDiffCallback extends DiffUtil.ItemCallback<Event> {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getSummary().equals(newItem.getSummary()) &&
                    oldItem.getStart().equals(newItem.getStart()) &&
                    oldItem.getEnd().equals(newItem.getEnd());
        }
    }
}