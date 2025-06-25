package com.example.POMODORO_PRO.ui.task;


import java.io.Serializable;

public class Task implements Serializable {
    private int id;
    private String name, description, date, priority, category;
    private boolean completed;  // NEW COLUMN

    // No-argument constructor (required for serialization)
    public Task() {
        // Initialize default values
        this.id = 0;
        this.name = "";
        this.description = "";
        this.date = "";
        this.priority = "";
        this.category = "";
        this.completed = false;
    }
    public Task(int id, String name, String description, String due_date, String priority, String category,boolean completed) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = due_date;
        this.priority = priority;
        this.category = category;
        this.completed = completed;
    }

    public Task(String name, String description, String due_date,
                String priority, String category) {
        this.name = name;
        this.description = description;
        this.date = due_date;
        this.priority = priority;
        this.category = category;
        this.completed = false;  // Default value
    }



    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public boolean isCompleted() { return completed; }
    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.date = dueDate; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setCategory(String category) { this.category = category; }

    public void setCompleted(boolean completed) { this.completed = completed; }


    }



