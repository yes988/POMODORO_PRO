package com.example.POMODORO_PRO.ui.event;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.POMODORO_PRO.R;

import java.util.List;

public class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.DayViewHolder> {
    private final List<String> days;
    private int selectedPosition = 0;
    private final OnDayClickListener listener;
    private int lastPosition = -1;

    public interface OnDayClickListener {
        void onDayClick(int position);
    }

    public WeekDayAdapter(List<String> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_week_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String[] dayParts = days.get(position).split("\n");
        if (dayParts.length >= 2) {
            holder.dayNumberText.setText(dayParts[0]);
            holder.dayNameText.setText(dayParts[1]);
        }

        // For backward compatibility
        holder.dayText.setText(days.get(position));
        
        // Set selected state
        holder.itemView.setSelected(selectedPosition == position);
        
        // Handle click
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onDayClick(position);
        });
        
        // Add animation
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.scale_up);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull DayViewHolder holder) {
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        TextView dayNumberText;
        TextView dayNameText;
        View eventIndicator;
        
        DayViewHolder(View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.textDay);
            dayNumberText = itemView.findViewById(R.id.textDayNumber);
            dayNameText = itemView.findViewById(R.id.textDayName);
            eventIndicator = itemView.findViewById(R.id.eventIndicator);
        }
    }
}