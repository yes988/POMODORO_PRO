package com.example.POMODORO_PRO.ui.progress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.POMODORO_PRO.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.widget.ImageButton;

public class ProgressFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressAdapter progressAdapter;
    private SearchView searchView;
    private List<ProgressItem> progressList;
    private TextView emptyStateText;
    private ChipGroup dateFilterChipGroup;
    private ChipGroup timeFilterChipGroup;
    private ProgressViewModel progressViewModel;
    private ImageButton btnFilter;
    private MaterialCardView filterCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.progress_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        progressList = new ArrayList<>();

        setupViews(view);
        setupRecyclerView();
        setupSearchView();
        setupDateFilterChips();
        setupTimeFilterChips();
        setupFilterButton();
        observeViewModel();
    }

    private void setupViews(View view) {
        emptyStateText = view.findViewById(R.id.emptyStateText);
        searchView = view.findViewById(R.id.searchView);
        dateFilterChipGroup = view.findViewById(R.id.dateFilterChipGroup);
        timeFilterChipGroup = view.findViewById(R.id.timeFilterChipGroup);
        recyclerView = view.findViewById(R.id.recyclerViewProgress);
        btnFilter = view.findViewById(R.id.btnFilter);
        filterCard = view.findViewById(R.id.filterCard);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        progressAdapter = new ProgressAdapter(progressList);
        progressAdapter.setOnDeleteClickListener(this::showDeleteConfirmationDialog);
        recyclerView.setAdapter(progressAdapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                progressAdapter.filterList(newText);
                return true;
            }
        });
    }

    private void setupDateFilterChips() {
        dateFilterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAllDate) {
                progressViewModel.setDateFilter(ProgressViewModel.DateFilter.ALL);
            } else if (checkedId == R.id.chipToday) {
                progressViewModel.setDateFilter(ProgressViewModel.DateFilter.TODAY);
            } else if (checkedId == R.id.chipWeek) {
                progressViewModel.setDateFilter(ProgressViewModel.DateFilter.WEEK);
            } else if (checkedId == R.id.chipMonth) {
                progressViewModel.setDateFilter(ProgressViewModel.DateFilter.MONTH);
            }
        });

        // Set the "All" chip as checked by default
        Chip chipAllDate = dateFilterChipGroup.findViewById(R.id.chipAllDate);
        if (chipAllDate != null) {
            chipAllDate.setChecked(true);
        }
    }
    
    private void setupTimeFilterChips() {
        timeFilterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAllTime) {
                progressViewModel.setTimeFilter(ProgressViewModel.TimeFilter.ALL);
            } else if (checkedId == R.id.chipShortDuration) {
                progressViewModel.setTimeFilter(ProgressViewModel.TimeFilter.SHORT);
            } else if (checkedId == R.id.chipMediumDuration) {
                progressViewModel.setTimeFilter(ProgressViewModel.TimeFilter.MEDIUM);
            } else if (checkedId == R.id.chipLongDuration) {
                progressViewModel.setTimeFilter(ProgressViewModel.TimeFilter.LONG);
            } else if (checkedId == R.id.chipVeryLongDuration) {
                progressViewModel.setTimeFilter(ProgressViewModel.TimeFilter.VERY_LONG);
            }
        });

        // Set the "All" chip as checked by default
        Chip chipAllTime = timeFilterChipGroup.findViewById(R.id.chipAllTime);
        if (chipAllTime != null) {
            chipAllTime.setChecked(true);
        }
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> {
            if (filterCard.getVisibility() == View.VISIBLE) {
                filterCard.setVisibility(View.GONE);
            } else {
                filterCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private void observeViewModel() {
        progressViewModel.getProgressItems().observe(getViewLifecycleOwner(), items -> {
            progressList.clear();
            progressList.addAll(items);
            progressAdapter.updateData(progressList);
            updateEmptyState();

            // Update progress indicators
            updateProgressIndicators();
        });
    }

    private void updateProgressIndicators() {
        // Calculate and update progress percentage
        int totalTasks = progressList.size();
        int completedTasks = (int) progressList.stream().filter(ProgressItem::isCompleted).count();
        
        // Calculate total accumulated time across all tasks
        int totalAccumulatedTime = progressList.stream()
                .mapToInt(ProgressItem::getAccumulatedTime)
                .sum();
        
        // Calculate average completion percentage - based on actual study minutes
        // compared to expected Pomodoro sessions
        float progressPercent;
        if (totalTasks > 0) {
            // Standard target: each task should have at least one Pomodoro (25 min)
            int targetTotalTime = totalTasks * 25 * 60; // 25 minutes in seconds per task
            progressPercent = Math.min(100f, (totalAccumulatedTime * 100f) / targetTotalTime);
        } else {
            progressPercent = 0;
        }

        TextView progressPercentView = getView().findViewById(R.id.txtProgressPercent);
        progressPercentView.setText(String.format(Locale.getDefault(), "%.0f%%", progressPercent));

        TextView tasksCompletedView = getView().findViewById(R.id.txtTasksCompleted);
        tasksCompletedView.setText(String.format(Locale.getDefault(), "%d/%d tasks", completedTasks, totalTasks));

        // Update progress bar
        com.google.android.material.progressindicator.LinearProgressIndicator progressBar =
                getView().findViewById(R.id.progressBar2);
        progressBar.setProgress((int) progressPercent);
    }

    private void updateEmptyState() {
        emptyStateText.setVisibility(progressList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDeleteConfirmationDialog(ProgressItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(R.string.delete_confirmation_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    progressViewModel.deleteProgressItem(item);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}