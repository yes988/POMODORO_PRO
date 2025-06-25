package com.example.POMODORO_PRO.ui.report;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.SessionDatabaseHelper;
import com.example.POMODORO_PRO.ui.progress.ProgressItem;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFragment extends Fragment {

    private PieChart pieChart;
    private Spinner reportTypeSpinner;
    private Spinner timeRangeSpinner;
    private SessionDatabaseHelper dbHelper;
    private View rootView;
    private CardView filterCard, chartCard, achievementsCard;
    private TextView reportHeaderText, chartTitle;

    private AchievementManager achievementManager;
    private RecyclerView achievementsRecyclerView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_report, container, false);

        dbHelper = new SessionDatabaseHelper(getContext());
        achievementManager = new AchievementManager(getContext());

        initializeViews();
        setupSpinners();
        setupPieChart();
        setupAchievementsView();

        // Animate the UI components when the view is created
        animateUIComponents();

        // Load default report (today's daily report)
        loadReport("daily", dateFormat.format(new Date()));

        return rootView;
    }

    private void initializeViews() {
        pieChart = rootView.findViewById(R.id.pieChart);
        reportTypeSpinner = rootView.findViewById(R.id.reportTypeSpinner);
        timeRangeSpinner = rootView.findViewById(R.id.timeRangeSpinner);
        achievementsRecyclerView = rootView.findViewById(R.id.achievementsRecyclerView);
        
        // Get card views for animations
        filterCard = rootView.findViewById(R.id.filterCard);
        chartCard = rootView.findViewById(R.id.chartCard);
        achievementsCard = rootView.findViewById(R.id.achievementsCard);
        
        // Get text views
        reportHeaderText = rootView.findViewById(R.id.reportHeaderText);
        chartTitle = rootView.findViewById(R.id.chartTitle);
        
        // Set up view all achievements button
        MaterialButton viewAllAchievements = rootView.findViewById(R.id.viewAllAchievements);
        viewAllAchievements.setOnClickListener(v -> {
            // Add button click animation
            Animation scaleAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.button_press);
            v.startAnimation(scaleAnimation);
            showAllAchievementsDialog();
        });
    }

    private void animateUIComponents() {
        // Animate the header text first
        reportHeaderText.setAlpha(0f);
        reportHeaderText.setTranslationY(-50f);
        reportHeaderText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Staggered animation for cards
        animateCardWithDelay(filterCard, 200);
        animateCardWithDelay(chartCard, 400);
        animateCardWithDelay(achievementsCard, 600);
    }

    private void animateCardWithDelay(View card, int delay) {
        card.setAlpha(0f);
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void setupAchievementsView() {
        List<Achievement> unlockedAchievements = achievementManager.getUnlockedAchievements();
        AchievementAdapter adapter = new AchievementAdapter(unlockedAchievements, getContext());

        // Use a grid layout with 3 columns for better visual organization
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        achievementsRecyclerView.setLayoutManager(layoutManager);
        achievementsRecyclerView.setAdapter(adapter);
        
        // Add item animation for achievement items
        achievementsRecyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator() {
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                holder.itemView.setAlpha(0f);
                holder.itemView.setScaleX(0.8f);
                holder.itemView.setScaleY(0.8f);
                
                holder.itemView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
                return false;
            }
        });
        
        // Add decoration for spacing between items
        int spacing = getResources().getDimensionPixelSize(R.dimen.achievement_grid_spacing);
        achievementsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, spacing, true));
    }

    private void setupSpinners() {
        // Report type spinner (daily, weekly, monthly)
        ArrayAdapter<CharSequence> reportTypeAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.report_types,
                R.layout.spinner_item
        );
        reportTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        reportTypeSpinner.setAdapter(reportTypeAdapter);

        // Time range spinner (will be populated dynamically)
        ArrayAdapter<String> timeRangeAdapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                new ArrayList<>()
        );
        timeRangeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        timeRangeSpinner.setAdapter(timeRangeAdapter);

        reportTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimeRangeOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        timeRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String reportType = reportTypeSpinner.getSelectedItem().toString().toLowerCase();
                String timeRange = timeRangeSpinner.getSelectedItem().toString();
                
                // Update chart title with selection
                updateChartTitle(reportType, timeRange);
                
                // Animate the chart container before loading new data
                animateChartRefresh(() -> loadReport(reportType, timeRange));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    
    private void updateChartTitle(String reportType, String timeRange) {
        String title;
        try {
            // Format the title based on the report type
            switch (reportType) {
                case "daily":
                    Date date = dateFormat.parse(timeRange);
                    title = "Daily Report: " + displayDateFormat.format(date);
                    break;
                case "weekly":
                    Date weekStart = dateFormat.parse(timeRange);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(weekStart);
                    cal.add(Calendar.DAY_OF_YEAR, 6);
                    String weekEnd = displayDateFormat.format(cal.getTime());
                    title = "Weekly Report: " + displayDateFormat.format(weekStart) + " - " + weekEnd;
                    break;
                case "monthly":
                    Date monthDate = monthFormat.parse(timeRange);
                    title = "Monthly Report: " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthDate);
                    break;
                default:
                    title = "Time Distribution";
            }
        } catch (Exception e) {
            title = "Time Distribution";
        }
        
        // Make title effectively final for use in the inner class
        final String finalTitle = title;
        
        // Animate the title change
        Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(150);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                chartTitle.setText(finalTitle);
                Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(150);
                chartTitle.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        chartTitle.startAnimation(fadeOut);
    }
    
    private void animateChartRefresh(Runnable onAnimationEnd) {
        // Animate the chart card
        ValueAnimator colorAnim = ValueAnimator.ofArgb(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F5F5F5"),
                Color.parseColor("#FFFFFF"));
        
        colorAnim.addUpdateListener(animator -> 
                chartCard.setCardBackgroundColor((int) animator.getAnimatedValue()));
        
        colorAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });
        
        colorAnim.setDuration(400);
        colorAnim.start();
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
                for (int i = 0; i < 4; i++) {
                    // Get start of week (Monday)
                    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                    }
                    adapter.add(dateFormat.format(calendar.getTime()));
                    calendar.add(Calendar.DAY_OF_YEAR, -7);
                }
                break;

            case "monthly":
                // Add last 6 months
                for (int i = 0; i < 6; i++) {
                    adapter.add(monthFormat.format(calendar.getTime()));
                    calendar.add(Calendar.MONTH, -1);
                }
                break;
        }

        adapter.notifyDataSetChanged();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(8, 8, 8, 8);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        
        // Center hole settings
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(55f);
        
        // Text settings
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setCenterText("Your Focus");
        pieChart.setCenterTextSize(16f);
        
        // Legend settings
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        
        // Initial empty state
        pieChart.setNoDataText("No data available");
        pieChart.setNoDataTextColor(Color.GRAY);
    }

    private void loadReport(String reportType, String timeRange) {
        List<ProgressItem> sessions;

        // Show loading state
        pieChart.clear();
        pieChart.setNoDataText("Loading...");
        
        switch (reportType) {
            case "daily":
                sessions = dbHelper.getDailyReport(timeRange);
                break;
            case "weekly":
                sessions = dbHelper.getWeeklyReport(timeRange);
                break;
            case "monthly":
                sessions = dbHelper.getMonthlyReport(timeRange);
                break;
            default:
                sessions = new ArrayList<>();
        }

        if (sessions == null || sessions.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No data available for selected period");
            
            // Show a toast message for better user feedback
            Toast.makeText(getContext(), 
                "No Pomodoro sessions found for this period", 
                Toast.LENGTH_SHORT).show();
        } else {
            updatePieChart(sessions);
        }
    }

    private void updatePieChart(List<ProgressItem> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No data available for selected period");
            return;
        }

        // Aggregate data by task name
        Map<String, Float> taskTimeMap = new HashMap<>();
        for (ProgressItem item : sessions) {
            String taskName = item.getTaskName();
            float timeInHours = item.getAccumulatedTime() / 3600f; // Convert seconds to hours

            if (taskTimeMap.containsKey(taskName)) {
                taskTimeMap.put(taskName, taskTimeMap.get(taskName) + timeInHours);
            } else {
                taskTimeMap.put(taskName, timeInHours);
            }
        }

        // Prepare data for pie chart
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : taskTimeMap.entrySet()) {
            // Format task name if too long
            String taskName = entry.getKey();
            if (taskName.length() > 15) {
                taskName = taskName.substring(0, 12) + "...";
            }
            entries.add(new PieEntry(entry.getValue(), taskName));
        }

        // Enhanced styling for the pie chart
        PieDataSet dataSet = new PieDataSet(entries, "Tasks");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        
        // Use a custom color scheme
        dataSet.setColors(
            Color.parseColor("#6200EE"), // Purple
            Color.parseColor("#03DAC5"), // Teal
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#3700B3"), // Dark Purple
            Color.parseColor("#FFDE03"), // Yellow
            Color.parseColor("#018786"), // Dark Teal
            Color.parseColor("#FF0266")  // Pink
        );
        
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(100f);
        dataSet.setValueLinePart1Length(0.6f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineColor(Color.LTGRAY);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        
        // Animate the chart
        pieChart.animateY(1400, Easing.EaseInOutQuad);
        
        // Calculate total hours for center text
        float totalHours = 0;
        for (float hours : taskTimeMap.values()) {
            totalHours += hours;
        }
        
        pieChart.setCenterText(String.format(Locale.getDefault(), "%.1f hrs", totalHours));
        
        pieChart.invalidate(); // refresh
    }

    private void showAllAchievementsDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_all_achievements, null);
        RecyclerView allAchievementsRecyclerView = dialogView.findViewById(R.id.allAchievementsRecyclerView);
        TextView progressDetail = dialogView.findViewById(R.id.achievementProgressDetail);

        List<Achievement> allAchievements = achievementManager.getAllAchievements();
        List<Achievement> unlockedAchievements = achievementManager.getUnlockedAchievements();
        
        // Set progress text
        progressDetail.setText(String.format("You've unlocked %d of %d achievements", 
                unlockedAchievements.size(), allAchievements.size()));
        
        AchievementAdapter allAdapter = new AchievementAdapter(allAchievements, getContext(), true);

        // Use the same grid layout with spacing for consistency
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        allAchievementsRecyclerView.setLayoutManager(layoutManager);
        allAchievementsRecyclerView.setAdapter(allAdapter);
        
        // Add decoration for spacing between items
        int spacing = getResources().getDimensionPixelSize(R.dimen.achievement_grid_spacing);
        allAchievementsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, spacing, true));

        // Create and show the dialog with animations
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext(), R.style.AchievementDialogStyle);
        builder.setView(dialogView)
               .setPositiveButton("Close", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Animate dialog content with staggered item animations
        dialogView.setAlpha(0f);
        dialogView.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Enable animations in adapter for a nice entry effect
        allAdapter.enableAnimations();
    }

    /**
     * ItemDecoration for adding spacing between grid items
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}