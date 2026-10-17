package com.subscription;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console menu. Entry point of the application.
 */
public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static final SubscriptionService service = new SubscriptionService();

    private static ReminderService reminderService; // null when email is not configured
    private static ReminderScheduler scheduler;     // null when email is not configured

    public static void main(String[] args) {
        System.out.println("=== Subscription Reminder Manager ===");
        startReminders();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> addSubscription();
                    case "2" -> viewAll();
                    case "3" -> viewUpcoming();
                    case "4" -> viewExpenses();
                    case "5" -> cancelSubscription();
                    case "6" -> sendRemindersNow();
                    case "0" -> running = false;
                    default -> System.out.println("Please enter a number from the menu.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }

        if (scheduler != null) {
            scheduler.stop();
        }
        System.out.println("Goodbye!");
    }

    // ---------- Start-up ----------

    /** Starts the daily reminder scheduler if email credentials are present. */
    private static void startReminders() {
        try {
            EmailService emailService = new EmailService();
            reminderService = new ReminderService(service, emailService);
            scheduler = new ReminderScheduler(reminderService);
            scheduler.start();
        } catch (IllegalStateException e) {
            System.out.println("WARNING: " + e.getMessage());
            System.out.println("The app will work, but no reminder emails will be sent.");
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Add subscription");
        System.out.println("2. View all subscriptions");
        System.out.println("3. View renewals in the next 7 days");
        System.out.println("4. View monthly / yearly expenses");
        System.out.println("5. Cancel subscription");
        System.out.println("6. Send due reminders now");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    // ---------- Menu actions ----------

    private static void addSubscription() throws SQLException {
        System.out.println("\n--- Add subscription ---");
        String name = readText("Name (e.g. Netflix): ");
        BigDecimal amount = readAmount();
        Subscription.BillingCycle cycle = readCycle();
        LocalDate renewalDate = readDate();
        String email = readEmail();

        try {
            int id = service.addSubscription(name, amount, cycle, renewalDate, email);
            System.out.println("Added " + name + " with id " + id + ".");
            System.out.println("A reminder will be emailed on " + renewalDate.minusDays(1)
                    + " (if the app is running).");
        } catch (IllegalArgumentException e) {
            System.out.println("Not saved: " + e.getMessage());
        }
    }

    private static void viewAll() throws SQLException {
        System.out.println("\n--- All subscriptions ---");
        printTable(service.getAllSubscriptions());
    }

    private static void viewUpcoming() throws SQLException {
        System.out.println("\n--- Active renewals in the next 7 days ---");
        printTable(service.getUpcomingRenewals(7));
    }

    private static void viewExpenses() throws SQLException {
        System.out.println("\n--- Estimated expenses (active subscriptions) ---");
        System.out.println("Monthly: Rs. " + service.getMonthlyExpense());
        System.out.println("Yearly:  Rs. " + service.getYearlyExpense());
    }

    private static void cancelSubscription() throws SQLException {
        System.out.println("\n--- Cancel subscription ---");
        int id = readInt("Enter the id to cancel: ");
        if (service.cancelSubscription(id)) {
            System.out.println("Subscription " + id + " is now CANCELLED.");
        } else {
            System.out.println("No active subscription found with id " + id + ".");
        }
    }

    private static void sendRemindersNow() {
        if (reminderService == null) {
            System.out.println("Email is not configured, so reminders cannot be sent.");
            return;
        }
        int sent = reminderService.sendDueReminders();
        System.out.println("Emails sent: " + sent);
    }

    // ---------- Input helpers (keep asking until the input is valid) ----------

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = in.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static BigDecimal readAmount() {
        while (true) {
            System.out.print("Amount in Rs. (e.g. 649 or 649.50): ");
            try {
                BigDecimal amount = new BigDecimal(in.nextLine().trim());
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    return amount;
                }
                System.out.println("Amount must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static Subscription.BillingCycle readCycle() {
        while (true) {
            System.out.print("Billing cycle - M for monthly, Y for yearly: ");
            String value = in.nextLine().trim().toUpperCase();
            if (value.equals("M") || value.equals("MONTHLY")) {
                return Subscription.BillingCycle.MONTHLY;
            }
            if (value.equals("Y") || value.equals("YEARLY")) {
                return Subscription.BillingCycle.YEARLY;
            }
            System.out.println("Please type M or Y.");
        }
    }

    private static LocalDate readDate() {
        while (true) {
            System.out.print("Next renewal date (yyyy-MM-dd, e.g. 2026-11-05): ");
            try {
                LocalDate date = LocalDate.parse(in.nextLine().trim());
                if (!date.isBefore(LocalDate.now())) {
                    return date;
                }
                System.out.println("The date cannot be in the past.");
            } catch (DateTimeParseException e) {
                System.out.println("Please use the format yyyy-MM-dd.");
            }
        }
    }

    private static String readEmail() {
        String defaultEmail = System.getenv("EMAIL_USERNAME");
        String prompt = (defaultEmail == null || defaultEmail.isBlank())
                ? "Reminder email address: "
                : "Reminder email address [Enter = " + defaultEmail + "]: ";

        while (true) {
            System.out.print(prompt);
            String value = in.nextLine().trim();
            if (value.isEmpty() && defaultEmail != null && !defaultEmail.isBlank()) {
                return defaultEmail;
            }
            if (value.contains("@") && value.contains(".")) {
                return value;
            }
            System.out.println("That does not look like an email address.");
        }
    }

    // ---------- Output helper ----------

    private static void printTable(List<Subscription> list) {
        if (list.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        System.out.printf("%-4s %-15s %10s  %-8s %-12s %-10s %s%n",
                "ID", "Name", "Amount", "Cycle", "Renews on", "Status", "Email");
        for (Subscription s : list) {
            System.out.println(s);
        }
    }
}
