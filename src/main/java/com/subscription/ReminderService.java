package com.subscription;

import jakarta.mail.MessagingException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Finds subscriptions that renew tomorrow and emails a reminder for each one.
 *
 * Normal behaviour: target date = tomorrow.
 * For testing, set the environment variable REMINDER_TEST_DATE=yyyy-MM-dd
 * to pick any date. Unset it to go back to normal.
 */
public class ReminderService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy");   // 20 October 2026
    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SubscriptionService subscriptionService;
    private final EmailService emailService;

    public ReminderService(SubscriptionService subscriptionService, EmailService emailService) {
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
    }

    /** Runs one full reminder check. Returns the number of emails sent. */
    public int sendDueReminders() {
        LocalDate target = getTargetDate();
        log("Reminder check started. Looking for renewals on " + target);

        List<Subscription> due;
        try {
            due = subscriptionService.getActiveRenewingOn(target);
        } catch (SQLException e) {
            log("Database error, reminder check skipped: " + e.getMessage());
            return 0;
        }

        int sent = 0;
        for (Subscription s : due) {
            if (processOne(s, target)) {
                sent++;
            }
        }

        log("Reminder check finished. Found " + due.size() + ", sent " + sent + ".");
        return sent;
    }

    /** Handles one subscription. Returns true only if an email was sent. */
    private boolean processOne(Subscription s, LocalDate target) {
        String label = s.getName() + " (id " + s.getId() + ")";

        // Rule 1: skip if we already sent a reminder for this renewal date
        if (target.equals(s.getReminderSentFor())) {
            log("Skip " + label + ": reminder already sent for " + target);
            return false;
        }

        // Rule 2: skip if the email address is missing or broken
        String to = s.getEmail();
        if (to == null || !EMAIL_PATTERN.matcher(to.trim()).matches()) {
            log("Skip " + label + ": no valid email address");
            return false;
        }

        // Rule 3: send the email
        try {
            emailService.sendEmail(to.trim(), buildSubject(s, target), buildBody(s, target));
        } catch (MessagingException e) {
            // Leave reminder_sent_for unchanged, so the next run tries again.
            log("FAILED " + label + ": " + e.getMessage());
            return false;
        }

        // Rule 4: record success. If this step fails, the next run may send a duplicate.
        try {
            subscriptionService.markReminderSent(s.getId(), target);
            log("Sent " + label + " to " + to);
        } catch (SQLException e) {
            log("Sent " + label + " but could not record it: " + e.getMessage());
        }
        return true;
    }

    // ---------- Email text ----------

    private String buildSubject(Subscription s, LocalDate target) {
        return "Reminder: " + s.getName() + " renews " + whenWord(target);
    }

    private String buildBody(Subscription s, LocalDate target) {
        return "Hello,\n\n"
                + "This is a reminder that your " + s.getName()
                + " subscription renews " + whenWord(target) + ".\n\n"
                + "Subscription: " + s.getName() + "\n"
                + "Amount: ₹" + s.getAmount() + "\n"
                + "Billing cycle: " + s.getBillingCycle() + "\n"
                + "Renewal date: " + s.getRenewalDate().format(LONG_DATE) + "\n\n"
                + "Please review your subscription before renewal.\n\n"
                + "Thank you.";
    }

    /** "tomorrow" normally; a full date when a test date is being used. */
    private String whenWord(LocalDate target) {
        if (target.equals(LocalDate.now().plusDays(1))) {
            return "tomorrow";
        }
        return "on " + target.format(LONG_DATE);
    }

    // ---------- Helpers ----------

    /** Tomorrow, unless REMINDER_TEST_DATE is set. */
    static LocalDate getTargetDate() {
        String test = System.getenv("REMINDER_TEST_DATE");
        if (test != null && !test.isBlank()) {
            return LocalDate.parse(test.trim());
        }
        return LocalDate.now().plusDays(1);
    }

    private static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(LOG_TIME) + "] " + message);
    }
}
