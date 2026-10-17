package com.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Model class: holds the data of one subscription (one row of the table).
 */
public class Subscription {

    public enum BillingCycle { MONTHLY, YEARLY }

    public enum Status { ACTIVE, CANCELLED }

    private final int id;
    private final String name;
    private final BigDecimal amount;
    private final BillingCycle billingCycle;
    private final LocalDate renewalDate;
    private final String email;
    private final Status status;
    private final LocalDate reminderSentFor; // null = no reminder sent yet

    public Subscription(int id, String name, BigDecimal amount, BillingCycle billingCycle,
                        LocalDate renewalDate, String email, Status status,
                        LocalDate reminderSentFor) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.renewalDate = renewalDate;
        this.email = email;
        this.status = status;
        this.reminderSentFor = reminderSentFor;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }

    public String getEmail() {
        return email;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDate getReminderSentFor() {
        return reminderSentFor;
    }

    /** Monthly cost of this subscription. Yearly plans are divided by 12. */
    public BigDecimal getMonthlyEquivalent() {
        if (billingCycle == BillingCycle.YEARLY) {
            return amount.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        }
        return amount;
    }

    @Override
    public String toString() {
        return String.format("%-4d %-15s %10s  %-8s %-12s %-10s %s",
                id, name, amount, billingCycle, renewalDate, status, email);
    }
}
