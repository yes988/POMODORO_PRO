package com.example.POMODORO_PRO.ui.report;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private boolean unlocked;
    private int iconResId;

    public Achievement(String id, String name, String description, int iconResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResId = iconResId;
        this.unlocked = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
    public int getIconResId() { return iconResId; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}