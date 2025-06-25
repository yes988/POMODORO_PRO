package com.example.POMODORO_PRO.ui.event;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.POMODORO_PRO.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EditEventDialog extends DialogFragment {
    private static final String TAG = "EditEventDialog";
    private static final String ARG_EVENT = "event";
    private static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    private Event event;
    private EventUpdateListener listener;
    private EditText eventTitle;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private DatePicker startDatePicker;
    private Calendar startCalendar;
    private Calendar endCalendar;


    public interface EventUpdateListener {
        void onUpdate(GoogleSignInAccount account, Context context,
                      String eventId, String title,
                      long startMillis, long endMillis);
    }
    public static EditEventDialog newInstance(Event event, EventUpdateListener listener) {
        EditEventDialog dialog = new EditEventDialog();
        Bundle args = new Bundle();

        // Just pass basic fields
        args.putString("eventId", event.getId());
        args.putString("title", event.getSummary());
        args.putLong("startMillis", event.getStart().getDateTime().getValue());
        args.putLong("endMillis", event.getEnd().getDateTime().getValue());

        dialog.setArguments(args);
        dialog.listener = listener;
        return dialog;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        if (getArguments() != null) {
            String title = getArguments().getString("title");
            long startMillis = getArguments().getLong("startMillis");
            long endMillis = getArguments().getLong("endMillis");

            startCalendar.setTimeInMillis(startMillis);
            endCalendar.setTimeInMillis(endMillis);

            event = new Event();
            event.setId(getArguments().getString("eventId"));
            event.setSummary(title);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.edit_event_dialog, null);

        eventTitle = view.findViewById(R.id.eventTitle);
        startTimePicker = view.findViewById(R.id.startTimePicker);
        endTimePicker = view.findViewById(R.id.endTimePicker);

        Button btnSelectDate = view.findViewById(R.id.btnSelectDate);
        btnSelectDate.setOnClickListener(v -> showDatePicker(true));

        btnSelectDate.setText(DateFormat.getDateInstance().format(startCalendar.getTime()));

        // Initialize with default values or event values
        if (event != null) {
            // Set title
            if (event.getSummary() != null) {
                eventTitle.setText(event.getSummary());
            }

            // Handle start time
            if (event.getStart() != null) {
                if (event.getStart().getDateTime() != null) {
                    try {
                        Date startDate = ISO8601_FORMAT.parse(event.getStart().getDateTime().toStringRfc3339());
                        startCalendar.setTime(startDate);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing start date", e);
                        Toast.makeText(getContext(), "Error parsing start time", Toast.LENGTH_SHORT).show();
                    }
                } else if (event.getStart().getDate() != null) {
                    try {
                        Date startDate = ISO8601_FORMAT.parse(event.getStart().getDate().toStringRfc3339());
                        startCalendar.setTime(startDate);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing start date", e);
                    }
                }
            }

            // Handle end time
            if (event.getEnd() != null) {
                if (event.getEnd().getDateTime() != null) {
                    try {
                        Date endDate = ISO8601_FORMAT.parse(event.getEnd().getDateTime().toStringRfc3339());
                        endCalendar.setTime(endDate);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing end date", e);
                        Toast.makeText(getContext(), "Error parsing end time", Toast.LENGTH_SHORT).show();
                    }
                } else if (event.getEnd().getDate() != null) {
                    try {
                        Date endDate = ISO8601_FORMAT.parse(event.getEnd().getDate().toStringRfc3339());
                        endCalendar.setTime(endDate);
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing end date", e);
                    }
                }
            }
        }

        // Set initial values to time pickers
        startTimePicker.setHour(startCalendar.get(Calendar.HOUR_OF_DAY));
        startTimePicker.setMinute(startCalendar.get(Calendar.MINUTE));
        endTimePicker.setHour(endCalendar.get(Calendar.HOUR_OF_DAY));
        endTimePicker.setMinute(endCalendar.get(Calendar.MINUTE));

        // Date picker setup

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(event == null ? "Create Event" : "Edit Event")
                .setPositiveButton("Save", (dialog, which) -> {
                    if (validateInput()) {
                        saveEvent();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    private boolean validateInput() {
        String title = eventTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Update calendars with current picker values
        startCalendar.set(Calendar.HOUR_OF_DAY, startTimePicker.getHour());
        startCalendar.set(Calendar.MINUTE, startTimePicker.getMinute());

        endCalendar.set(Calendar.HOUR_OF_DAY, endTimePicker.getHour());
        endCalendar.set(Calendar.MINUTE, endTimePicker.getMinute());

        if (endCalendar.before(startCalendar)) {
            Toast.makeText(getContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startCalendar : endCalendar;
        DatePickerDialog datePicker = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                // Optionally, update both calendars to keep them on the same day
                if (isStartDate) {
                    endCalendar.set(Calendar.YEAR, year);
                    endCalendar.set(Calendar.MONTH, month);
                    endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                }

                Button btnSelectDate = getDialog().findViewById(R.id.btnSelectDate);
                if (btnSelectDate != null) {
                    btnSelectDate.setText(DateFormat.getDateInstance().format(calendar.getTime()));
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void saveEvent() {
        String title = eventTitle.getText().toString().trim();

        // Final update of calendar objects with current picker values
        startCalendar.set(Calendar.HOUR_OF_DAY, startTimePicker.getHour());
        startCalendar.set(Calendar.MINUTE, startTimePicker.getMinute());

        endCalendar.set(Calendar.HOUR_OF_DAY, endTimePicker.getHour());
        endCalendar.set(Calendar.MINUTE, endTimePicker.getMinute());

        long startMillis = startCalendar.getTimeInMillis();
        long endMillis = endCalendar.getTimeInMillis();

        String eventId = event != null ? event.getId() : null;

        if (listener != null) {
            listener.onUpdate(
                    GoogleSignIn.getLastSignedInAccount(requireContext()),
                    requireContext(),
                    eventId,
                    title,
                    startMillis,
                    endMillis
            );
        }

        // Update event times
        event.setStart(new EventDateTime()
            .setDateTime(new DateTime(startMillis))
            .setTimeZone(TimeZone.getDefault().getID()));
        event.setEnd(new EventDateTime()
            .setDateTime(new DateTime(endMillis))
            .setTimeZone(TimeZone.getDefault().getID()));
    }
}