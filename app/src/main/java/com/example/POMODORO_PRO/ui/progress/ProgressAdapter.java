package com.example.POMODORO_PRO.ui.progress;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.SessionDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ViewHolder> {

    private List<ProgressItem> progressList;
    private List<ProgressItem> progressListFull;
    private String currentFilter = "";
    private OnDeleteClickListener deleteListener;

    public ProgressAdapter(List<ProgressItem> progressList) {
        this.progressList = new ArrayList<>(progressList);
        this.progressListFull = new ArrayList<>(progressList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_item, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgressItem item = progressList.get(position);
        Log.d("AdapterDebug", "Displaying - " + item.getDate() + " " +
                item.getTaskName() + " " + item.getAccumulatedTime());
        holder.txtDate.setText(item.getDisplayDate());
        holder.txtTaskName.setText(item.getTaskName());
        holder.txtTime.setText(item.formatTime());
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    public void updateData(List<ProgressItem> newList) {
        progressListFull.clear();
        progressListFull.addAll(newList);
        progressList.clear();
        progressList.addAll(progressListFull);
        notifyDataSetChanged();
    }

    public void filterList(String keyword) {
        currentFilter = keyword != null ? keyword : "";
        progressList.clear();

        if (currentFilter.isEmpty()) {
            progressList.addAll(progressListFull);
        } else {
            String searchText = currentFilter.toLowerCase().trim();
            for (ProgressItem item : progressListFull) {
                if (item.getTaskName().toLowerCase().contains(searchText)) {
                    progressList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByToday(SessionDatabaseHelper dbHelper) {
        applyFilters(dbHelper, "Today", "All");
    }

    public void filterByWeek(SessionDatabaseHelper dbHelper) {
        applyFilters(dbHelper, "This Week", "All");
    }

    public void filterByMonth(SessionDatabaseHelper dbHelper) {
        applyFilters(dbHelper, "This Month", "All");
    }

    public void loadAllProgress(SessionDatabaseHelper dbHelper) {
        applyFilters(dbHelper, "All", "All");
    }

    private void applyFilters(SessionDatabaseHelper dbHelper, String timeFilter, String taskFilter) {
        List<ProgressItem> filteredList = new ArrayList<>();

        if (timeFilter.equals("Today")) {
            filteredList = dbHelper.getDailyReport(getCurrentDate());
        } else if (timeFilter.equals("This Week")) {
            filteredList = dbHelper.getWeeklyReport(getStartOfWeekDate());
        } else if (timeFilter.equals("This Month")) {
            filteredList = dbHelper.getMonthlyReport(getCurrentYearMonth());
        } else {
            filteredList = dbHelper.getAllSessions();
        }

        updateData(filteredList);
    }

    private String getCurrentDate() {
        // Implement date formatting as needed
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getStartOfWeekDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    private String getCurrentYearMonth() {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
    }

    public void deleteProgressItem(SessionDatabaseHelper dbHelper, ProgressItem item) {
        if (dbHelper.deleteSession(item.getDate(), item.getTaskName())) {
            progressList.remove(item);
            progressListFull.remove(item);
            notifyDataSetChanged();
        }
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ProgressItem item);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtTaskName, txtTime;
        ImageButton btnDelete;
        ProgressAdapter adapter;

        public ViewHolder(@NonNull View itemView, ProgressAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            txtDate = itemView.findViewById(R.id.txtDate);
            txtTaskName = itemView.findViewById(R.id.txtTaskName);
            txtTime = itemView.findViewById(R.id.txtTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && adapter.deleteListener != null) {
                    adapter.deleteListener.onDeleteClick(adapter.progressList.get(position));
                }
            });
        }
    }
}