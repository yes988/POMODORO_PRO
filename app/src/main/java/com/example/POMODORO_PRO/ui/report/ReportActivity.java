package com.example.POMODORO_PRO.ui.report;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.SessionDatabaseHelper;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private PieChart pieChart;
    private Spinner reportTypeSpinner;
    private Spinner timeRangeSpinner;
    private TextView totalTimeTextView;
    private SessionDatabaseHelper dbHelper;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new SessionDatabaseHelper(this);

        pieChart = findViewById(R.id.pieChart);
        reportTypeSpinner = findViewById(R.id.reportTypeSpinner);
        timeRangeSpinner = findViewById(R.id.timeRangeSpinner);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);

        setupSpinners();
        setupPieChart();

        // Load default report (today's daily report)
        loadReport("daily", dateFormat.format(new Date()));
    }

    private void setupSpinners() {
        // Report type spinner
        ArrayAdapter<CharSequence> reportTypeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.report_types,
                android.R.layout.simple_spinner_item
        );
        reportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(reportTypeAdapter);

        // Time range spinner
        ArrayAdapter<String> timeRangeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRangeSpinner.setAdapter(timeRangeAdapter);

        reportTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                updateTimeRangeOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        timeRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String reportType = reportTypeSpinner.getSelectedItem().toString().toLowerCase();
                String timeRange = timeRangeSpinner.getSelectedItem().toString();
                loadReport(reportType, timeRange);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTimeRangeOptions() {
        String reportType = reportTypeSpinner.getSelectedItem().toString().toLowerCase();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) timeRangeSpinner.getAdapter();
        adapter.clear();

        Calendar calendar = Calendar.getInstance();

        switch (reportType) {
            case "daily":
                // Add last 7 days
                for (int i = 0; i < 7; i++) {
                    adapter.add(dateFormat.format(calendar.getTime()));
                    calendar.add(Calendar.DAY_OF_YEAR, -1);
                }
                break;

            case "weekly":
                // Add last 4 weeks
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                for (int i = 0; i < 4; i++) {
                    adapter.add(dateFormat.format(calendar.getTime()));
                    calendar.add(Calendar.WEEK_OF_YEAR, -1);
                }
                break;

            case "monthly":
                // Add last 6 months
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                for (int i = 0; i < 6; i++) {
                    adapter.add(monthFormat.format(calendar.getTime()));
                    calendar.add(Calendar.MONTH, -1);
                }
                break;
        }

        adapter.notifyDataSetChanged();
        timeRangeSpinner.setSelection(0);
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
    }

    private void loadReport(String reportType, String timeRange) {
        long startTimestamp = 0;
        long endTimestamp = 0;

        try {
            Calendar cal = Calendar.getInstance();

            switch (reportType) {
                case "daily":
                    Date date = dateFormat.parse(timeRange);
                    startTimestamp = date.getTime();
                    endTimestamp = startTimestamp + 86400000; // Add 1 day
                    break;

                case "weekly":
                    Date weekStart = dateFormat.parse(timeRange);
                    cal.setTime(weekStart);
                    cal.add(Calendar.DAY_OF_YEAR, 6); // End of week
                    startTimestamp = weekStart.getTime();
                    endTimestamp = cal.getTimeInMillis() + 86400000;
                    break;

                case "monthly":
                    cal.setTime(monthFormat.parse(timeRange));
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    startTimestamp = cal.getTimeInMillis();
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    endTimestamp = cal.getTimeInMillis() + 86400000;
                    break;
            }

            updatePieChart(startTimestamp, endTimestamp);
            updateTotalTime(startTimestamp, endTimestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePieChart(long startTimestamp, long endTimestamp) {
        Map<String, Integer> distribution = dbHelper.getTaskDistribution(startTimestamp, endTimestamp);

        if (distribution.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No data available for selected period");
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Task Time Distribution");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateTotalTime(long startTimestamp, long endTimestamp) {
        int totalSeconds = dbHelper.getTotalTimeForPeriod(startTimestamp, endTimestamp);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        String timeText;
        if (hours > 0) {
            timeText = String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            timeText = String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
        } else {
            timeText = String.format(Locale.getDefault(), "%ds", seconds);
        }

        totalTimeTextView.setText(getString(R.string.total_time_format, timeText));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}