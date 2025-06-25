package com.example.POMODORO_PRO.ui.event;

import com.google.api.services.calendar.model.Event;

import java.util.List;

public class CalendarResult {
    private final List<Event> events;
    private final String errorMessage;
    private final boolean isSuccess;

    // Success constructor
    public CalendarResult(List<Event> events) {
        this.events = events;
        this.errorMessage = null;
        this.isSuccess = true;
    }

    // Error constructor
    public CalendarResult(String errorMessage) {
        this.events = null;
        this.errorMessage = errorMessage;
        this.isSuccess = false;
    }

    // Static factory method for error
    public static CalendarResult error(String errorMessage) {
        return new CalendarResult(errorMessage);
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean hasEvents() {
        return isSuccess && events != null && !events.isEmpty();
    }
}