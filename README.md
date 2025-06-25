# POMODORO_PRO

POMODORO_PRO is a productivity mobile application designed to help users manage their tasks efficiently using the Pomodoro technique. The app enables users to organize tasks by urgency and importance, track their progress, and visualize productivity analytics through a modern, user-friendly interface. It also integrates with the Google Calendar API, allowing users to schedule and synchronize events directly with their Google Calendar for better time management and planning.

## Features

- **Task Management:** Organize tasks using a quadrant system inspired by the Eisenhower Matrix (Urgent & Important, Important but Not Urgent, etc.).
- **Pomodoro Timer:** Focus on tasks using the Pomodoro technique to boost productivity.
- **Progress Tracking:** Visualize completed tasks and monitor productivity statistics.
- **Reports & Analytics:** View charts and summaries of your productivity.
- **Google Calendar Integration:** Schedule events and sync tasks with your Google Calendar for seamless event management.
- **Modern UI:** Clean, Material Design-inspired interface for an intuitive user experience.

## Technologies & Tools Used

- **Android Studio:** Android build and testing.
- **Java/Kotlin:** Main programming languages for Android development.
- **Google Calendar API:** For event scheduling and calendar synchronization.
- **Gradle:** Android build configuration.
- **ESLint & Prettier:** Code linting and formatting.
- **Watchman:** File watching during development.

## Getting Started

1. **Clone the repository:**
    ```sh
    git clone https://github.com/yourusername/POMODORO_PRO.git
    cd POMODORO_PRO
    ```

2. **Open in Android Studio and build the project.**

3. **Configure Google Calendar API:**
   - Set up a project in the [Google Cloud Console](https://console.cloud.google.com/).
   - Enable the Google Calendar API.
   - Download your `google-services.json` and place it in the `app` directory.
   - Follow the in-app instructions to authenticate and sync your calendar.

4. **Run the app on your Android device or emulator.**

## Database Sample Data

Quadrant table:
```sql
INSERT INTO quadrant (quadrant_id, title, description, color_code) VALUES
(1, 'Urgent & Important', 'High priority tasks requiring immediate attention', '#FF5252'),
(2, 'Important, Not Urgent', 'Strategic tasks for long-term success', '#4CAF50'),
(3, 'Urgent, Not Important', 'Time-sensitive but lower value tasks', '#FFC107'),
(4, 'Not Urgent & Not Important', 'Low priority tasks with minimal impact', '#9E9E9E');
```

Task table:
```sql
INSERT INTO task (task_id, title, description, quadrant_id, due_date, estimated_pomodoros, completed_pomodoros, status) VALUES
(1, 'Complete Project Proposal', 'Finish and submit Q3 project proposal', 1, '2025-06-20', 4, 0, 'pending'),
(2, 'Client Meeting Preparation', 'Prepare presentation for key client meeting', 1, '2025-06-18', 3, 1, 'in_progress'),
(3, 'Learn New Framework', 'Study new Android development fundamentals', 2, '2025-07-01', 8, 2, 'in_progress'),
(4, 'Code Review', 'Review team pull requests', 2, '2025-06-25', 3, 0, 'pending'),
(5, 'Email Responses', 'Clear inbox and respond to pending emails', 3, '2025-06-19', 2, 1, 'in_progress'),
(6, 'Team Meeting', 'Weekly sync-up with development team', 3, '2025-06-18', 1, 0, 'pending'),
(7, 'Organize Desktop', 'Clean up and organize work files', 4, '2025-06-30', 1, 0, 'pending'),
(8, 'Update Documentation', 'Review and update old documentation', 4, '2025-07-05', 2, 0, 'pending');
```

## Future Work

- **Mini Game:** Plans for a gamified productivity experience (currently unfinished).
- **Website/Blog:** Potential for a companion website or blog to share productivity tips and user stories.

## License

This project is for educational and personal use.

---
