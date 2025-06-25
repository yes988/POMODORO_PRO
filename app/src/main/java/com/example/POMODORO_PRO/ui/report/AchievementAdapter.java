package com.example.POMODORO_PRO.ui.report;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.POMODORO_PRO.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private List<Achievement> achievements;
    private Context context;
    private boolean showLockEffect = false;
    private int lastPosition = -1;
    private boolean enableAnimations = true;

    public AchievementAdapter(List<Achievement> achievements, Context context) {
        this(achievements, context, false);
    }

    public AchievementAdapter(List<Achievement> achievements, Context context, boolean showLockEffect) {
        this.achievements = achievements;
        this.context = context;
        this.showLockEffect = showLockEffect;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);
        holder.name.setText(achievement.getName());
        holder.description.setText(achievement.getDescription());
        holder.icon.setImageResource(achievement.getIconResId());

        // Apply card styling based on unlock status
        if (showLockEffect && !achievement.isUnlocked()) {
            // Locked achievement styling
            holder.icon.setColorFilter(context.getResources().getColor(android.R.color.darker_gray));
            holder.name.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.description.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.lock.setVisibility(View.VISIBLE);
            holder.card.setCardElevation(1f);
            holder.card.setStrokeWidth(0);
        } else {
            // Unlocked achievement styling
            holder.icon.clearColorFilter();
            holder.name.setTextColor(context.getResources().getColor(R.color.primary_text));
            holder.description.setTextColor(context.getResources().getColor(R.color.secondary_text));
            holder.lock.setVisibility(View.GONE);
            holder.card.setCardElevation(4f);
            
            // Add a gold stroke to unlocked achievements
            if (achievement.isUnlocked()) {
                holder.card.setStrokeColor(context.getResources().getColor(R.color.achievement_gold));
                holder.card.setStrokeWidth(2);
            } else {
                holder.card.setStrokeWidth(0);
            }
        }

        // Apply enter animation
        setEnterAnimation(holder.itemView, position);
        
        // Add click effect
        holder.itemView.setOnClickListener(v -> {
            if (achievement.isUnlocked()) {
                playUnlockedAnimation(holder);
            } else {
                playLockedAnimation(holder);
            }
        });
    }
    
    private void playUnlockedAnimation(AchievementViewHolder holder) {
        // Create a bounce animation for the card
        AnimatorSet animatorSet = new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(holder.card, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(holder.card, "scaleY", 1f, 1.1f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(holder.icon, "rotation", 0f, 360f);
        
        animatorSet.playTogether(scaleX, scaleY, rotation);
        animatorSet.setDuration(800);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.start();
    }
    
    private void playLockedAnimation(AchievementViewHolder holder) {
        // Create a shake animation for locked achievements
        Animation shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake_animation);
        holder.lock.startAnimation(shakeAnimation);
    }

    private void setEnterAnimation(View viewToAnimate, int position) {
        if (!enableAnimations || position <= lastPosition) {
            return;
        }
        
        // Staggered fade-in and slide-up animation
        viewToAnimate.setAlpha(0f);
        viewToAnimate.setTranslationY(50f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator alpha = ObjectAnimator.ofFloat(viewToAnimate, "alpha", 0f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(viewToAnimate, "translationY", 50f, 0f);
        
        animatorSet.playTogether(alpha, translationY);
        animatorSet.setStartDelay(position * 50); // Staggered delay
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
        
        lastPosition = position;
    }
    
    public void disableAnimations() {
        this.enableAnimations = false;
    }
    
    public void enableAnimations() {
        this.enableAnimations = true;
        this.lastPosition = -1;
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView icon, lock;
        TextView name, description;
        View iconContainer;
        MaterialCardView card;
        
        AchievementViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            icon = itemView.findViewById(R.id.achievementIcon);
            lock = itemView.findViewById(R.id.lockIcon);
            name = itemView.findViewById(R.id.achievementName);
            description = itemView.findViewById(R.id.achievementDescription);
            iconContainer = itemView.findViewById(R.id.iconContainer);
        }
    }
}
