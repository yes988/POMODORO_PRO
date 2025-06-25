package com.example.POMODORO_PRO.ui.progress;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.POMODORO_PRO.SessionDatabaseHelper;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProgressViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ProgressItem>> progressItems = new MutableLiveData<>();
    private final SessionDatabaseHelper dbHelper;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    // Filter state
    public enum DateFilter { ALL, TODAY, WEEK, MONTH }
    public enum TimeFilter { ALL, SHORT, MEDIUM, LONG, VERY_LONG }
    
    private DateFilter currentDateFilter = DateFilter.ALL;
    private TimeFilter currentTimeFilter = TimeFilter.ALL;
    
    // Date range for current filter
    private String startDate = null;
    private String endDate = null;

    // Constants for time filtering
    private static final int SECONDS_30_MIN = 30 * 60;
    private static final int SECONDS_60_MIN = 60 * 60;
    private static final int SECONDS_10_HOURS = 10 * 60 * 60;

    public ProgressViewModel(Application application) {
        super(application);
        dbHelper = new SessionDatabaseHelper(application);
        loadAllProgress();
    }

    public LiveData<List<ProgressItem>> getProgressItems() {
        return progressItems;
    }
    
    public void setDateFilter(DateFilter filter) {
        currentDateFilter = filter;
        updateDateRange();
        applyFilters();
    }
    
    public void setTimeFilter(TimeFilter filter) {
        currentTimeFilter = filter;
        applyFilters();
    }
    
    private void updateDateRange() {
        switch (currentDateFilter) {
            case TODAY:
                startDate = dateFormat.format(Calendar.getInstance().getTime());
                endDate = startDate;
                break;
            case WEEK:
                Calendar weekCal = Calendar.getInstance();
                weekCal.set(Calendar.DAY_OF_WEEK, weekCal.getFirstDayOfWeek());
                startDate = dateFormat.format(weekCal.getTime());
                weekCal.add(Calendar.DAY_OF_YEAR, 6);
                endDate = dateFormat.format(weekCal.getTime());
                break;
            case MONTH:
                Calendar monthCal = Calendar.getInstance();
                monthCal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormat.format(monthCal.getTime());
                monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = dateFormat.format(monthCal.getTime());
                break;
            case ALL:
            default:
                startDate = null;
                endDate = null;
                break;
        }
    }
    
    private void applyFilters() {
        new Thread(() -> {
            // First get items based on date filter
            List<ProgressItem> filteredItems;
            if (startDate != null && endDate != null) {
                filteredItems = dbHelper.getSessionsBetweenDates(startDate, endDate);
            } else {
                filteredItems = dbHelper.getAllSessions();
            }
            
            // Then apply time filter if needed
            if (currentTimeFilter != TimeFilter.ALL) {
                filteredItems = filteredItems.stream()
                        .filter(item -> {
                            int time = item.getAccumulatedTime();
                            switch (currentTimeFilter) {
                                case SHORT:
                                    return time < SECONDS_30_MIN;
                                case MEDIUM:
                                    return time >= SECONDS_30_MIN && time < SECONDS_60_MIN;
                                case LONG:
                                    return time >= SECONDS_60_MIN && time < SECONDS_10_HOURS;
                                case VERY_LONG:
                                    return time >= SECONDS_10_HOURS;
                                default:
                                    return true;
                            }
                        })
                        .collect(Collectors.toList());
            }
            
            progressItems.postValue(filteredItems);
        }).start();
    }

    public void filterByToday() {
        setDateFilter(DateFilter.TODAY);
    }

    public void filterByWeek() {
        setDateFilter(DateFilter.WEEK);
    }

    public void filterByMonth() {
        setDateFilter(DateFilter.MONTH);
    }
    
    public void filterByShortDuration() {
        setTimeFilter(TimeFilter.SHORT);
    }
    
    public void filterByMediumDuration() {
        setTimeFilter(TimeFilter.MEDIUM);
    }
    
    public void filterByLongDuration() {
        setTimeFilter(TimeFilter.LONG);
    }
    
    public void filterByVeryLongDuration() {
        setTimeFilter(TimeFilter.VERY_LONG);
    }

    public void loadAllProgress() {
        currentDateFilter = DateFilter.ALL;
        currentTimeFilter = TimeFilter.ALL;
        startDate = null;
        endDate = null;
        
        new Thread(() -> {
            List<ProgressItem> items = dbHelper.getAllSessions();
            progressItems.postValue(items);
        }).start();
    }

    public void deleteProgressItem(ProgressItem item) {
        new Thread(() -> {
            boolean success = dbHelper.deleteSession(item.getDate(), item.getTaskName());
            if (success) {
                applyFilters(); // Apply current filters after deletion
            }
        }).start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        dbHelper.close();
    }
}