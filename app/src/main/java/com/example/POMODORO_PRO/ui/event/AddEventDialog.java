package com.example.POMODORO_PRO.ui.event;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.POMODORO_PRO.databinding.DialogAddEventBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventDialog extends DialogFragment {
    private DialogAddEventBinding binding;
    private final GoogleSignInAccount account;
    private final EventSubmitListener listener;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface EventSubmitListener {
        void onSubmit(GoogleSignInAccount account, String title, long startMillis, long endMillis);
    }

    public AddEventDialog(EventSubmitListener listener, GoogleSignInAccount account) {
        this.listener = listener;
        this.account = account;

        // Set default end time to 1 hour after start
        endCalendar.add(Calendar.HOUR, 1);
    }
    
    public AddEventDialog(EventSubmitListener listener, GoogleSignInAccount account, long selectedDateMillis) {
        this.listener = listener;
        this.account = account;
        
        // Set the calendars to the selected date
        startCalendar.setTimeInMillis(selectedDateMillis);
        endCalendar.setTimeInMillis(selectedDateMillis);
        
        // Set default end time to 1 hour after start
        endCalendar.add(Calendar.HOUR, 1);
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddEventBinding.inflate(LayoutInflater.from(requireContext()));

        setupDatePicker();
        setupTimePickers();

        return new android.app.AlertDialog.Builder(requireContext())
                .setView(binding.getRoot())
                .setTitle("Add New Event")
                .setPositiveButton("Add", (dialog, which) -> validateAndSubmit())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void setupDatePicker() {
        binding.pickDate.setText(dateFormat.format(startCalendar.getTime()));

        binding.pickDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                    (view, year, month, day) -> {
                        startCalendar.set(Calendar.YEAR, year);
                        startCalendar.set(Calendar.MONTH, month);
                        startCalendar.set(Calendar.DAY_OF_MONTH, day);

                        // Keep end date same as start date by default
                        endCalendar.set(Calendar.YEAR, year);
                        endCalendar.set(Calendar.MONTH, month);
                        endCalendar.set(Calendar.DAY_OF_MONTH, day);

                        binding.pickDate.setText(dateFormat.format(startCalendar.getTime()));
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH));

            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePicker.show();
        });
    }

    private void setupTimePickers() {
        binding.pickStartTime.setText(timeFormat.format(startCalendar.getTime()));
        binding.pickEndTime.setText(timeFormat.format(endCalendar.getTime()));

        binding.pickStartTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                    (view, hour, minute) -> {
                        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
                        startCalendar.set(Calendar.MINUTE, minute);
                        binding.pickStartTime.setText(timeFormat.format(startCalendar.getTime()));

                        // Auto-adjust end time if it's before start time
                        if (endCalendar.before(startCalendar)) {
                            endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
                            endCalendar.add(Calendar.HOUR, 1);
                            binding.pickEndTime.setText(timeFormat.format(endCalendar.getTime()));
                        }
                    },
                    startCalendar.get(Calendar.HOUR_OF_DAY),
                    startCalendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });

        binding.pickEndTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                    (view, hour, minute) -> {
                        endCalendar.set(Calendar.HOUR_OF_DAY, hour);
                        endCalendar.set(Calendar.MINUTE, minute);
                        binding.pickEndTime.setText(timeFormat.format(endCalendar.getTime()));
                    },
                    endCalendar.get(Calendar.HOUR_OF_DAY),
                    endCalendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });
    }

    private void validateAndSubmit() {
        if (TextUtils.isEmpty(binding.eventTitle.getText())) {
            Toast.makeText(getContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endCalendar.before(startCalendar)) {
            Toast.makeText(getContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        listener.onSubmit(
                account,
                binding.eventTitle.getText().toString(),
                startCalendar.getTimeInMillis(),
                endCalendar.getTimeInMillis()
        );
    }
}