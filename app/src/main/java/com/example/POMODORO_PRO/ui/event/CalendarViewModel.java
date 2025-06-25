// File: CalendarViewModel.java
package com.example.POMODORO_PRO.ui.event;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import com.example.POMODORO_PRO.ui.event.CalendarResult;
public class CalendarViewModel extends ViewModel {
    private final MutableLiveData<com.example.POMODORO_PRO.ui.event.CalendarResult> eventsResult = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<CalendarResult> getEventsResult() {
        return eventsResult;
    }

    private static final int REQUEST_AUTHORIZATION = 1001;

    public void fetchEvents(GoogleSignInAccount account, Context context) {
        // Post a loading state first
        eventsResult.postValue(new CalendarResult("Loading events..."));
        
        executor.execute(() -> {
            try {
                Calendar service = GoogleCalendarServiceHelper.getCalendarService(account, context);
                DateTime now = new DateTime(System.currentTimeMillis());

                Events events = service.events().list("primary")
                        .setMaxResults(10)
                        .setTimeMin(now)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();

                eventsResult.postValue(new CalendarResult(events.getItems()));
                
            } catch (UserRecoverableAuthIOException e) {
                handleAuthException(e, context);
                eventsResult.postValue(new CalendarResult("Authorization required"));
            } catch (Exception e) {
                Log.e("FetchEvents", "Error", e);
                eventsResult.postValue(new CalendarResult("Failed to fetch events: " + e.getMessage()));
            }
        });
    }

    public void addEvent(GoogleSignInAccount account, Context context,
                         String title, long startMillis, long endMillis) {
        executor.execute(() -> {
            try {
                Calendar service = GoogleCalendarServiceHelper.getCalendarService(account, context);

                EventDateTime start = new EventDateTime()
                        .setDateTime(new DateTime(startMillis))
                        .setTimeZone(TimeZone.getDefault().getID());

                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(endMillis))
                        .setTimeZone(TimeZone.getDefault().getID());

                Event event = new Event()
                        .setSummary(title)
                        .setStart(start)
                        .setEnd(end);

                service.events().insert("primary", event).execute();
                fetchEvents(account, context); // Refresh list

            } catch (Exception e) {
                Log.e("AddEvent", "Error", e);
                eventsResult.postValue(new CalendarResult("Failed to add event"));
            }
        });
    }

    // UPDATE
    public void updateEvent(GoogleSignInAccount account, Context context,
                            String eventId, String newTitle,
                            long newStartMillis, long newEndMillis) {
        executor.execute(() -> {
            try {
                HttpTransport transport = new NetHttpTransport();
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context,
                        Collections.singleton("https://www.googleapis.com/auth/calendar")
                );
                credential.setSelectedAccount(account.getAccount());

                Calendar service = new Calendar.Builder(transport, jsonFactory, credential)
                        .setApplicationName("My Calendar App")
                        .build();

                // Get existing event
                Event event = service.events().get("primary", eventId).execute();

                // Update fields
                event.setSummary(newTitle)
                        .setStart(new EventDateTime()
                                .setDateTime(new com.google.api.client.util.DateTime(newStartMillis))
                                .setTimeZone(TimeZone.getDefault().getID()))
                        .setEnd(new EventDateTime()
                                .setDateTime(new com.google.api.client.util.DateTime(newEndMillis))
                                .setTimeZone(TimeZone.getDefault().getID()));

                // Save updated event
                service.events().update("primary", eventId, event).execute();

                // Refresh events list
                fetchEvents(account, context);

            } catch (UserRecoverableAuthIOException e) {
                handleAuthException(e, context);
                eventsResult.postValue(CalendarResult.error("Authorization required to update event"));
            } catch (IOException e) {
                eventsResult.postValue(CalendarResult.error("Failed to update event: " + e.getMessage()));
            }
        });
    }

    // DELETE
    public void deleteEvent(GoogleSignInAccount account, Context context, String eventId) {
        executor.execute(() -> {
            try {
                HttpTransport transport = new NetHttpTransport();
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context,
                        Collections.singleton("https://www.googleapis.com/auth/calendar")
                );
                credential.setSelectedAccount(account.getAccount());

                Calendar service = new Calendar.Builder(transport, jsonFactory, credential)
                        .setApplicationName("My Calendar App")
                        .build();

                // Delete the event
                service.events().delete("primary", eventId).execute();

                // Refresh events list
                fetchEvents(account, context);

            } catch (UserRecoverableAuthIOException e) {
                handleAuthException(e, context);
                eventsResult.postValue(CalendarResult.error("Authorization required to delete event"));
            } catch (IOException e) {
                eventsResult.postValue(CalendarResult.error("Failed to delete event: " + e.getMessage()));
            }
        });
    }

    private void handleAuthException(UserRecoverableAuthIOException e, Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(
                        e.getIntent(),
                        REQUEST_AUTHORIZATION
                );
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }

    public static String formatEventDate(Event event) {
        try {
            EventDateTime start = event.getStart();
            Date date = start.getDateTime() != null ?
                    new Date(start.getDateTime().getValue()) :
                    new Date(start.getDate().getValue());
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "Date unavailable";
        }
    }
}
