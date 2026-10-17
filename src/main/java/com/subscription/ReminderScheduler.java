package com.subscription;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the reminder check automatically: once right after start-up,
 * then every day at REMINDER_HOUR (default 9 = 9:00 AM).
 *
 * The app must stay running for this to work. If it is closed, nothing is sent.
 */
public class ReminderScheduler {

    private static final int DEFAULT_HOUR = 9;

    private final ReminderService reminderService;
    private final int hour;
    private final ScheduledExecutorService executor;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
        this.hour = readHour();
        // One background thread is enough: reminders run one at a time.
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "reminder-scheduler");
            t.setDaemon(true); // does not keep the JVM alive on its own
            return t;
        });
    }

    /** Starts the daily schedule. */
    public void start() {
        // Run once now: useful if the laptop was off at the scheduled hour.
        executor.submit(this::safeRun);

        long secondsUntilFirst = secondsUntilNext(hour);
        long oneDay = TimeUnit.DAYS.toSeconds(1);
        executor.scheduleAtFixedRate(this::safeRun, secondsUntilFirst, oneDay, TimeUnit.SECONDS);

        System.out.printf("Reminder scheduler started. Daily check at %02d:00 (next in %d h %d min).%n",
                hour, secondsUntilFirst / 3600, (secondsUntilFirst % 3600) / 60);
    }

    /** Stops the schedule cleanly. Called when the user exits the app. */
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wraps the real work in try/catch. This matters: if a scheduled task throws
     * an exception, scheduleAtFixedRate silently cancels ALL future runs.
     */
    private void safeRun() {
        try {
            reminderService.sendDueReminders();
        } catch (Exception e) {
            System.out.println("Reminder run failed: " + e.getMessage());
        }
    }

    /** Seconds from now until the next time the clock shows hour:00. */
    static long secondsUntilNext(int hour) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(LocalTime.of(hour, 0));
        if (!next.isAfter(now)) {
            next = next.plusDays(1); // today's slot already passed, use tomorrow's
        }
        return Duration.between(now, next).getSeconds();
    }

    /** Reads REMINDER_HOUR (0-23). Falls back to 9 if missing or invalid. */
    private static int readHour() {
        String value = System.getenv("REMINDER_HOUR");
        if (value == null || value.isBlank()) {
            return DEFAULT_HOUR;
        }
        try {
            int h = Integer.parseInt(value.trim());
            if (h >= 0 && h <= 23) {
                return h;
            }
        } catch (NumberFormatException ignored) {
            // fall through to default
        }
        System.out.println("REMINDER_HOUR '" + value + "' is not valid (0-23). Using " + DEFAULT_HOUR + ".");
        return DEFAULT_HOUR;
    }
}
