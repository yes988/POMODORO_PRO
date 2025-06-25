package com.example.POMODORO_PRO.ui.event;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.POMODORO_PRO.R;
import com.example.POMODORO_PRO.databinding.FragmentCalendarBinding;
import com.example.POMODORO_PRO.ui.NetworkUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.services.calendar.model.Event;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment implements EventAdapter.EventClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final String TAG = "GoogleSignIn";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly";

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private EventAdapter eventsAdapter;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(task);
                } else {
                    showToast("Sign-in cancelled");
                }
            }
    );

    // In your Activity/Fragment
    private void showGoogleCalendar() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://calendar.google.com/calendar/r"));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Fallback to web browser
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://calendar.google.com")));
        }
    }
    private final ActivityResultLauncher<Intent> authLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                    if (account != null) {
                        viewModel.fetchEvents(account, requireContext());
                    }
                } else {
                    showToast("Authorization denied");
                }
            }
    );

    private static final List<String> SCOPES =
            Arrays.asList(
                    "https://www.googleapis.com/auth/calendar",
                    "https://www.googleapis.com/auth/calendar.readonly"
            );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private int selectedDayPosition = 0;
    private WeekDayAdapter weekDayAdapter;
    private RecyclerView weekRecyclerView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupObservers();
        initializeGoogleSignIn();
        updateMonthYearText();

        // --- Account Button and Dropdown Menu ---
        FloatingActionButton btnAccount = view.findViewById(R.id.btnAccount);
        MaterialCardView accountDropdownMenu = view.findViewById(R.id.accountDropdownMenu);
        MaterialButton btnLogOut = view.findViewById(R.id.btnLogOut);
        MaterialButton btnSwitchAccount = view.findViewById(R.id.btnSwitchAccount);

        // Initially hide the dropdown menu
        accountDropdownMenu.setVisibility(View.GONE);
        
        // Set up account button click to toggle dropdown menu
        btnAccount.setOnClickListener(v -> {
            // Add animation to button press
            v.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                    
                    // Toggle dropdown visibility with animation
                    if (accountDropdownMenu.getVisibility() == View.VISIBLE) {
                        // Hide dropdown with animation
                        accountDropdownMenu.animate()
                            .alpha(0f)
                            .translationY(-10f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                accountDropdownMenu.setVisibility(View.GONE);
                                accountDropdownMenu.setTranslationY(0f);
                            })
                            .start();
                    } else {
                        // Show dropdown with animation
                        accountDropdownMenu.setAlpha(0f);
                        accountDropdownMenu.setTranslationY(-10f);
                        accountDropdownMenu.setVisibility(View.VISIBLE);
                        accountDropdownMenu.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(200)
                            .start();
                    }
                })
                .start();
        });

        // Set up Log Out button
        btnLogOut.setOnClickListener(v -> {
            // Hide dropdown first
            accountDropdownMenu.setVisibility(View.GONE);
            
            // Button animation and feedback
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    // Haptic feedback
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
                    
                    // Show loading state on button
                    MaterialButton btn = (MaterialButton) v;
                    btn.setEnabled(false);
                    
                    // Sign out process
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        // Reset button state
                        btn.setEnabled(true);
                        
                        // Show sign-in button with animation
                        binding.btnSignIn.setAlpha(0f);
                        binding.btnSignIn.setTranslationY(-50f);
                        binding.btnSignIn.setVisibility(View.VISIBLE);
                        binding.btnSignIn.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .start();
                        
                        // Show empty state with animation
                        binding.eventsRecyclerView.setVisibility(View.GONE);
                        binding.emptyState.setAlpha(0f);
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.emptyState.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                        
                        // Hide account button
                        btnAccount.setVisibility(View.GONE);
                        
                        showToast("Successfully signed out");
                    });
                })
                .start();
        });

        // Set up Switch Account button
        btnSwitchAccount.setOnClickListener(v -> {
            // Hide dropdown first
            accountDropdownMenu.setVisibility(View.GONE);
            
            // Button animation and feedback
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    // Haptic feedback
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
                    
                    // Show loading state
                    MaterialButton btn = (MaterialButton) v;
                    btn.setEnabled(false);
                    
                    // Sign out and switch account
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        // Reset button state
                        btn.setEnabled(true);
                        
                        // Launch sign-in with slight delay for better UX
                        new Handler().postDelayed(() -> {
                            Intent signInIntent = googleSignInClient.getSignInIntent();
                            signInLauncher.launch(signInIntent);
                        }, 200);
                    });
                })
                .start();
        });

        // Show/hide account button based on sign-in state
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            btnAccount.setVisibility(View.VISIBLE);
        } else {
            btnAccount.setVisibility(View.GONE);
        }

        // Close dropdown when clicking outside
        view.setOnTouchListener((v, event) -> {
            if (accountDropdownMenu.getVisibility() == View.VISIBLE) {
                accountDropdownMenu.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        List<String> weekDays = getCurrentWeekDays();
        weekDayAdapter = new WeekDayAdapter(weekDays, position -> {
            selectedDayPosition = position;
            // Only call showEventsForDate if we have data
            CalendarResult result = viewModel.getEventsResult().getValue();
            if (result != null && result.isSuccess() && result.getEvents() != null) {
                showEventsForDate(position);
            }
        });
        weekRecyclerView = binding.weekRecyclerView;
        weekRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        weekRecyclerView.setAdapter(weekDayAdapter);

        // --- Auto-select today ---
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayIndex = 0;
        for (int i = 0; i < weekDays.size(); i++) {
            String dayStr = weekDays.get(i).split("\n")[0];
            if (Integer.parseInt(dayStr) == todayDay) {
                todayIndex = i;
                break;
            }
        }
        selectedDayPosition = todayIndex;
        weekDayAdapter.setSelectedPosition(todayIndex); // Update selected position without moving to day
        weekRecyclerView.scrollToPosition(todayIndex); // Use scrollToPosition instead of smoothScrollToPosition during initialization
        
        // Add animation to the floating action button
        binding.btnAddEvent.setAlpha(0f);
        binding.btnAddEvent.setScaleX(0.7f);
        binding.btnAddEvent.setScaleY(0.7f);
        binding.btnAddEvent.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
    }

    private void moveToDay(int position) {
        List<String> weekDays = getCurrentWeekDays();
        if (position < 0 || position >= weekDays.size()) return;
        selectedDayPosition = position;
        weekDayAdapter.setSelectedPosition(position); // Add this method to your adapter
        weekRecyclerView.smoothScrollToPosition(position);
        
        // Only show events if we have data available
        CalendarResult result = viewModel.getEventsResult().getValue();
        if (result != null && result.isSuccess() && result.getEvents() != null) {
            showEventsForDate(position);
        }
    }

    @Override
    public void onUpdateClick(Event event) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            showToast("Please sign in first");
            return;
        }

        EditEventDialog dialog = EditEventDialog.newInstance(event,
                (googleAccount, context, eventId, updatedTitle, updatedStart, updatedEnd) -> {
                    viewModel.updateEvent(
                            googleAccount,
                            context,
                            eventId,
                            updatedTitle,
                            updatedStart,
                            updatedEnd
                    );
                });
        dialog.show(getChildFragmentManager(), "EditEventDialog");
    }

    @Override
    public void onDeleteClick(String eventId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                    if (account != null) {
                        viewModel.deleteEvent(account, requireContext(), eventId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupUI() {
        eventsAdapter = new EventAdapter(this);
        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(eventsAdapter);

        // Apply layout animation to recycler view
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.layout_animation_from_bottom);
        binding.eventsRecyclerView.setLayoutAnimation(animation);

        binding.swipeRefresh.setOnRefreshListener(this);
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500, R.color.purple_700);
        
        binding.btnSignIn.setOnClickListener(v -> {
            // Add button click animation
            v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                    signIn();
                }).start();
        });
        
        binding.btnAddEvent.setOnClickListener(v -> {
            // Add button click animation
            v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100);
                    showAddEventDialog();
                }).start();
        });

        // Make monthYearText clickable to redirect to Google Calendar
        binding.monthYearText.setOnClickListener(v -> {
            // Add animation feedback
            v.animate()
                .alpha(0.5f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate().alpha(1f).setDuration(100);
                    showGoogleCalendar(); // Redirect to Google Calendar
                })
                .start();
        });

        // Set fixed size for items
        int spacingPx = (int) (8 * requireContext().getResources().getDisplayMetrics().density);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.weekRecyclerView.setLayoutManager(layoutManager);
        binding.weekRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = spacingPx;
                outRect.right = spacingPx;
            }
        });
    }

    private void setupObservers() {
        viewModel.getEventsResult().observe(getViewLifecycleOwner(), result -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.swipeRefresh.setRefreshing(false);

            if (result.isSuccess()) {
                List<Event> events = result.getEvents();
                if (events != null && !events.isEmpty()) {
                    binding.eventsRecyclerView.setVisibility(View.VISIBLE);
                    eventsAdapter.submitList(events);
                    showEmptyState(false);
                } else {
                    binding.eventsRecyclerView.setVisibility(View.GONE);
                    showEmptyState(true);
                }
            } else {
                // Handle error case
                binding.eventsRecyclerView.setVisibility(View.GONE);
                
                // Update empty state message through the TextView in LinearLayout
                TextView emptyStateText = binding.emptyState.findViewById(android.R.id.text1);
                if (emptyStateText == null) {
                    // If the text view doesn't exist, add it programmatically
                    emptyStateText = new TextView(requireContext());
                    emptyStateText.setId(android.R.id.text1);
                    emptyStateText.setTextSize(18);
                    emptyStateText.setGravity(Gravity.CENTER);
                    emptyStateText.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                    binding.emptyState.addView(emptyStateText);
                }
                
                if (result.getErrorMessage() != null) {
                    emptyStateText.setText(result.getErrorMessage());
                }
                
                showEmptyState(true);
                
                if (result.getErrorMessage() != null && 
                    !result.getErrorMessage().equals("Loading events...")) {
                    showToast(result.getErrorMessage());
                }
            }
        });
    }

    private void showEmptyState(boolean show) {
        if (show) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.eventsRecyclerView.setVisibility(View.GONE);
            
            // Add animation to empty state
            binding.emptyState.setAlpha(0f);
            binding.emptyState.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void initializeGoogleSignIn() {
        try {
            String webClientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Using web client ID: " + webClientId);

            // Create a more focused set of scopes to avoid permission dialog overload
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(webClientId)
                    .requestScopes(new Scope("https://www.googleapis.com/auth/calendar.readonly"))
                    .requestScopes(new Scope("https://www.googleapis.com/auth/calendar.events"))
                    .build();

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

            // Check if the user is already signed in
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null && account.getGrantedScopes().contains(new Scope("https://www.googleapis.com/auth/calendar.readonly"))) {
                Log.d(TAG, "User already signed in with calendar scope");
                updateUI(account);
            } else if (account != null) {
                Log.d(TAG, "User signed in but missing calendar scope, requesting additional scopes");
                // User is signed in but might be missing calendar scope, revoking to force re-auth
                googleSignInClient.revokeAccess().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        signIn();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Google SignIn initialization failed", e);
            showToast("Initialization error: " + e.getMessage());
        }
    }

    private void signIn() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showToast("No internet connection");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        showToast("Connecting to Google...");

        int playServicesCode = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(requireContext());

        if (playServicesCode == ConnectionResult.SUCCESS) {
            try {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                signInLauncher.launch(signInIntent);
            } catch (Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error launching sign-in intent", e);
                showToast("Sign-in error: " + e.getMessage());
            }
        } else {
            binding.progressBar.setVisibility(View.GONE);
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(requireActivity(), playServicesCode, 0).show();
            showToast("Google Play Services required");
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> task) {
        GoogleSignInAccount account = null;
        try {
            account = task.getResult(ApiException.class);
            if (account != null) {
                updateUI(account);
            } else {
                showToast("Sign-in failed: account is null");
            }
        } catch (ApiException e) {
            // Handle cancellation as a non-error case
            if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Log.d(TAG, "Sign-in was cancelled by the user");
                showToast("Sign-in cancelled");
                return;
            }
            
            String errorMessage = getSignInErrorMessage(e);
            Log.e(TAG, errorMessage, e);
            showToast(errorMessage);

            if (e.getStatusCode() == 10) { // DEVELOPER_ERROR
                logConfigurationDetails();
            }
        }

        if (account != null) {
            for (Scope scope : account.getGrantedScopes()) {
                Log.d("Scopes", "Granted: " + scope.getScopeUri());
            }
        }
    }

    private String getSignInErrorMessage(ApiException e) {
        switch (e.getStatusCode()) {
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                return "Sign-in cancelled";
            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                return "Sign-in failed. Please try again";
            case 10: // DEVELOPER_ERROR
                return "App configuration error";
            default:
                return "Error code: " + e.getStatusCode();
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        binding.btnSignIn.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Show account button when signed in
        View root = getView();
        if (root != null) {
            FloatingActionButton btnAccount = root.findViewById(R.id.btnAccount);
            if (btnAccount != null) {
                btnAccount.setVisibility(View.VISIBLE);
                
                // Add a nice animation to the account button
                btnAccount.setScaleX(0f);
                btnAccount.setScaleY(0f);
                btnAccount.setAlpha(0f);
                btnAccount.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
            }
        }
        
        // Fetch events
        viewModel.fetchEvents(account, requireContext());
        
        // Observe the events result to update UI when events are loaded
        viewModel.getEventsResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getEvents() != null && !result.getEvents().isEmpty()) {
                // After events are loaded, show events for the selected day
                showEventsForDate(selectedDayPosition);
            }
        });
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddEventDialog() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            showToast("Please sign in first");
            return;
        }

        // Get the selected date from the weekRecyclerView
        Calendar selectedDate = Calendar.getInstance();
        List<String> weekDays = getCurrentWeekDays();
        
        if (selectedDayPosition >= 0 && selectedDayPosition < weekDays.size()) {
            // Parse the selected day from the adapter
            String dayText = weekDays.get(selectedDayPosition).split("\n")[0];
            int day = Integer.parseInt(dayText);
            
            // Set the day in the calendar, keeping current month and year
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
        }

        AddEventDialog dialog = new AddEventDialog((googleAccount, title, startMillis, endMillis) -> {
            viewModel.addEvent(googleAccount, requireContext(), title, startMillis, endMillis);
        }, account, selectedDate.getTimeInMillis());

        dialog.show(getChildFragmentManager(), "AddEventDialog");
    }

    private void logConfigurationDetails() {
        try {
            Context context = requireContext();
            String packageName = context.getPackageName();
            String sha1 = getSHA1(context);
            String webClientId = getString(R.string.default_web_client_id);

            Log.e(TAG, "Configuration Details:");
            Log.e(TAG, "Package Name: " + packageName);
            Log.e(TAG, "SHA-1: " + sha1);
            Log.e(TAG, "Web Client ID: " + webClientId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log configuration details", e);
        }
    }

    private String getSHA1(Context context) throws Exception {
        PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        for (Signature signature : info.signatures) {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(signature.toByteArray());
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        }
        return "unknown";
    }
    private void loadEvents(GoogleSignInAccount account) {
        binding.swipeRefresh.setRefreshing(true);
        viewModel.fetchEvents(account, requireContext());
    }
    @Override
    public void onRefresh() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            loadEvents(account);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<String> getCurrentWeekDays() {
        List<String> weekDays = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat weekDayFormat = new SimpleDateFormat("E", Locale.getDefault()); // "Mon", "Tue", etc.

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            String weekDay = weekDayFormat.format(calendar.getTime());
            weekDays.add(day + "\n" + weekDay);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return weekDays;
    }
    private void showEventsForDate(int position) {
        // Get the selected date from the week
        List<String> weekDays = getCurrentWeekDays();
        if (position < 0 || position >= weekDays.size()) return;

        // Parse the selected date (e.g., "08\nMon" -> "08")
        String selectedDay = weekDays.get(position).split("\n")[0];

        // Get today's month and year
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        // Build a date string for comparison (e.g., "2024-06-08")
        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%s", year, month + 1, selectedDay);

        // Safely get events with null checks
        CalendarResult result = viewModel.getEventsResult().getValue();
        List<Event> allEvents = new ArrayList<>();
        
        if (result != null && result.isSuccess() && result.getEvents() != null) {
            allEvents = result.getEvents();
        }

        List<Event> filteredEvents = new ArrayList<>();
        for (Event event : allEvents) {
            // Assuming event.getStart().getDateTime() or event.getStart().getDate() returns a string like "2024-06-08T10:00:00"
            String eventDate = "";
            if (event.getStart() != null) {
                if (event.getStart().getDateTime() != null) {
                    eventDate = event.getStart().getDateTime().toStringRfc3339().substring(0, 10);
                } else if (event.getStart().getDate() != null) {
                    eventDate = event.getStart().getDate().toStringRfc3339().substring(0, 10);
                }
            }
            if (eventDate.equals(selectedDate)) {
                filteredEvents.add(event);
            }
        }

        // Update the adapter with filtered events
        eventsAdapter.submitList(filteredEvents);

        // Optionally show/hide empty state
        if (filteredEvents.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.eventsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateMonthYearText() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = format.format(calendar.getTime());
        
        if (binding != null) {
            binding.monthYearText.setText(monthYear);
            
            // If we have events, show them
            if (viewModel.getEventsResult().getValue() != null && 
                viewModel.getEventsResult().getValue().isSuccess() && 
                viewModel.getEventsResult().getValue().getEvents() != null) {
                
                eventsAdapter.submitList(viewModel.getEventsResult().getValue().getEvents());
            }
        }
    }
}