# ChronoStudy - Study Companion with Pomodoro, Timetable, Reminders & Widget

A polished, high-performance study companion designed for students and learners to maximize focus, conquer deadlines, and maintain consistent study habits. Features an immersive Pomodoro focus timer, comprehensive task and assignment manager with notifications/reminders, a visual study calendar & timetable with device calendar sync, in-depth subject-wise analytics with streaks, and an interactive Android Home Screen Widget for quick timer controls and upcoming task monitoring.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been integrated based on your clarifying preferences:

- **Calendar Scheduling**: Built-in visual weekly timetable and daily schedule grid, with one-tap export/sync to the Android Device Calendar (`CalendarContract.Events` / system calendar intents) so your scheduled study sessions and exam deadlines appear seamlessly in your system calendar.
- **Home Screen Widget**: Custom Android App Widget (`ChronoStudyWidget`) displaying active/paused Pomodoro status, one-tap timer play/pause/reset actions, current study session subject, and the next top upcoming tasks with due badges.
- **Analytics & Tracking**: Dedicated analytics dashboard featuring subject-wise time distribution charts (using custom canvas donuts & progress bars), daily/weekly focus streaks with flame indicators, task completion rate metrics, and historical session logs.
- **Data Persistence**: Offline-first reactive local database using Android Room with KSP, guaranteeing instant load times, zero latency, and complete privacy.

---

## 1. Overview & Core Concept

- **What It Does**: ChronoStudy provides an all-in-one focus and academic organization platform. Users can create subjects with distinct color identities, plan out study tasks with deadlines and priority levels, run Pomodoro focus cycles (Work, Short Break, Long Break) tied to specific subjects or tasks, set precise reminders, schedule study blocks in a timetable, and review study velocity and focus streaks.
- **Target Audience**: Students, self-learners, researchers, and professionals preparing for exams, certifications, or deep-work projects.
- **Key Value**: Eliminates context switching between a separate timer, to-do list, and calendar. By seamlessly uniting the Pomodoro technique with scheduled timetable blocks and actionable home screen widgets, it keeps learners accountable without friction.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Focus Session (Pomodoro)**:
   - Select an active subject (e.g., *Mathematics*, *Computer Science*, *History*) or specific task.
   - Start 25-minute Pomodoro (with customizable durations: 25/50m work, 5/10m break).
   - Real-time animated circular progress ring, pulse glow, pause/skip controls, and optional ambient focus noise toggle (white noise, rain, library ambience synth).
   - Session completion triggers vibration/notification alert, increments streak, logs study seconds to the subject, and marks associated tasks completed or in-progress.
2. **Tasks & Reminders**:
   - Organized by Subject, Priority (High/Medium/Low), and Due Date (Today, Upcoming, Overdue).
   - Easy inline task creation with quick subject tags, estimated Pomodoros, and reminder alarms.
   - Built-in notification scheduling using Android `AlarmManager` and `NotificationManagerCompat` to deliver timely study reminders.
3. **Calendar & Study Timetable**:
   - Visual 7-day weekly timetable block view showing lecture hours, study slots, and assignment deadlines.
   - Add study blocks with start/end time and subject.
   - "Sync to Device Calendar" action to push study blocks into the user's Google Calendar or local device calendar via Android `Intent.ACTION_INSERT` / `CalendarContract`.
4. **Analytics & Performance Tracking**:
   - Weekly & monthly study time totals with comparison to targets.
   - Visual subject distribution (proportional time breakdown with subject color keys).
   - Focus streak counter (consecutive days with completed focus sessions) and daily activity heatmap.
   - Task completion rate gauge (completed vs total tasks).
5. **Home Screen App Widget**:
   - Glanceable widget layout displaying current Pomodoro countdown / status and top pending tasks.
   - Direct interactive tap targets to open the app directly to the active timer or task detail.

### Visual Identity & Theme
- **Aesthetic Direction**: *Nordic Academic Minimalist* — Clean, calm, focused, high readability, avoiding visual noise.
- **Color Palette**:
  - Primary: Deep Indigo / Slate Blue (`#3B5998` / `#6366F1`)
  - Accent / Focus: Warm Amber & Vibrant Emerald (`#F59E0B` for focus glow, `#10B981` for completed break/streak)
  - Surface Dark: Deep Onyx / Navy Tint (`#0F172A`, `#1E293B`)
  - Surface Light: Crisp Alabaster & Cream Gray (`#F8FAFC`, `#F1F5F9`)
  - Subject Colors: Indigo, Teal, Violet, Coral, Rose, Emerald, Amber
- **Typography & Hierarchy**:
  - Monospace / Tabular digits for timer countdowns (preventing layout jitter).
  - Clean M3 sans typography with clear weight contrast (`HeadlineSmall` for headers, `TitleMedium` for cards, `LabelSmall` for subject pills).
- **Interactive Feedback & Motion**:
  - Smooth circular timer progress animation with spring interpolators.
  - Haptic feedback when starting, pausing, or completing a Pomodoro cycle.
  - Card elevation transitions and swipe-to-complete gestures for tasks.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Device Calendar Integration Strategy**:
  - *Chosen Approach*: Dual support: In-app interactive timetable + Android System Calendar integration using standard `Intent.ACTION_INSERT` / `CalendarContract.Events`.
  - *Why*: Allows users to seamlessly push study appointments, exam dates, and revision blocks directly into their native Google Calendar or Outlook calendar without needing complex cloud OAuth consent screens or third-party server proxies.
  - *Alternatives Considered*: Requiring full Google OAuth was considered, but device calendar intents provide zero-friction, work with whatever calendar app the user already uses (Google Calendar, Samsung Calendar, etc.), and respect privacy.
- **Decision 2: Persistence Engine (Room with KSP)**:
  - *Chosen Approach*: Jetpack Room Database with Flow observables and ViewModel repository.
  - *Why*: Study sessions and timer logs must never lag or fail if network is flaky; Room provides reliable ACID local persistence with fast reactive UI updates.
- **Decision 3: Pomodoro Foreground Service & Notifications**:
  - *Chosen Approach*: Kotlin Coroutine-backed Timer Manager with Android `NotificationChannel` support and `AlarmManager` for task reminders.
  - *Why*: Guarantees that timer progress and task reminder alerts continue reliably when the user minimizes the app or turns off their screen.
- **Decision 4: Home Screen Widget Implementation**:
  - *Chosen Approach*: Standard Android `AppWidgetProvider` with `RemoteViews`, updating via `AppWidgetManager` whenever sessions or tasks change.
  - *Why*: Zero additional heavy runtime overhead, fully compatible with all Android launcher versions, and highly responsive.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          ChronoStudy UI Layer                          │
├───────────────┬────────────────┬───────────────────┬───────────────────┤
│ TimerScreen   │ TaskListScreen │ CalendarScreen    │ AnalyticsScreen   │
│ (Pomodoro UI, │ (Tasks, Dues,  │ (Weekly Grid,     │ (Streaks, Donut,  │
│  Lottie/Ring) │  Filter Pills) │  Calendar Export) │  Subject Metrics) │
└───────┬───────┴────────┬───────┴─────────┬─────────┴─────────┬─────────┘
        │                │                 │                   │
        ▼                ▼                 ▼                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        StudyViewModel & StateFlow                      │
│ - activeSessionState: Work/Break, RemainingSeconds, IsRunning          │
│ - tasksState: Filtered task list, completion statuses                  │
│ - subjectsState: Custom subjects with color palettes                   │
│ - statsState: Total study time, current streak, subject breakdown      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
        ┌───────────────────────────┴───────────────────────────┐
        ▼                                                       ▼
┌───────────────────────────────┐       ┌───────────────────────────────┐
│       StudyRepository         │       │    Notification & Reminders   │
│  - TaskDao, SubjectDao,       │       │  - AlarmManager for task dues │
│    SessionDao                 │       │  - NotificationManagerCompat  │
└───────────────┬───────────────┘       └───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐       ┌───────────────────────────────┐
│     AppDatabase (Room)        │       │       ChronoStudyWidget       │
│  - TaskEntity                 │──────▶│  - RemoteViews App Widget     │
│  - SubjectEntity              │       │  - Quick Play/Pause & Next Task│
│  - FocusSessionEntity         │       └───────────────────────────────┘
└───────────────────────────────┘
```

### Data Model & Entities
1. **SubjectEntity**:
   - `id: Long` (Auto-increment)
   - `name: String` (e.g. "Biology", "Calculus")
   - `colorHex: String` (Color badge)
   - `targetWeeklyHours: Int`
2. **TaskEntity**:
   - `id: Long` (Auto-increment)
   - `title: String`
   - `notes: String`
   - `subjectId: Long`
   - `dueDate: Long?` (Epoch millis)
   - `hasReminder: Boolean`
   - `priority: String` (`HIGH`, `MEDIUM`, `LOW`)
   - `estimatedPomodoros: Int`
   - `completedPomodoros: Int`
   - `isCompleted: Boolean`
   - `createdAt: Long`
3. **FocusSessionEntity**:
   - `id: Long` (Auto-increment)
   - `subjectId: Long?`
   - `taskId: Long?`
   - `durationMinutes: Int`
   - `sessionType: String` (`WORK`, `SHORT_BREAK`, `LONG_BREAK`)
   - `completedAt: Long` (Timestamp)
4. **TimetableSlotEntity**:
   - `id: Long` (Auto-increment)
   - `subjectId: Long`
   - `dayOfWeek: Int` (1 = Monday, 7 = Sunday)
   - `startTimeMinuteOfDay: Int` (e.g. 540 = 9:00 AM)
   - `endTimeMinuteOfDay: Int`
   - `locationOrRoom: String`

### Implementation Steps
1. **Metadata & Manifest**: Update `metadata.json` with app name "ChronoStudy" and description. Update `AndroidManifest.xml` with required notification permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `VIBRATE`), receiver for reminders, and `AppWidgetProvider`.
2. **Database & Repository**: Implement Room database (`AppDatabase`), DAOs (`SubjectDao`, `TaskDao`, `SessionDao`, `TimetableDao`), default starter subjects (Mathematics, Science, Literature, History), and `StudyRepository`.
3. **Timer Engine & Service/Notification Helper**: Create `PomodoroManager` handling countdown intervals, state transitions (Work -> Break), sound/vibrate alerts, and notification updates.
4. **Home Screen Widget**: Create `ChronoStudyWidgetProvider` with `res/xml/widget_info.xml` and `res/layout/widget_layout.xml` displaying current timer state and top upcoming study tasks.
5. **UI Screens & Navigation**:
   - **FocusScreen**: Circular progress countdown ring, subject switcher, duration selector (25/5, 50/10), play/pause/skip buttons, session statistics.
   - **TasksScreen**: Filterable task list by status/subject/priority, Add Task dialog with date picker, estimated Pomodoro chips, and reminder toggles.
   - **TimetableScreen**: 7-day grid view, slot creation, and "Export to Device Calendar" action button.
   - **AnalyticsScreen**: Focus streaks counter, study hours ring, subject breakdown bar/donut chart, and historical session logs.
6. **Polishing & Verification**: Ensure Material 3 theme alignment, edge-to-edge support, test tags, and compile verification.
