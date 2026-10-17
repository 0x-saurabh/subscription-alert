package com.subscription;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * TEMPORARY version for Phase 6: tests every SubscriptionService operation.
 * In Phase 10 this file will be replaced by the real console menu.
 */
public class Main {

    public static void main(String[] args) {
        SubscriptionService service = new SubscriptionService();

        try {
            // Test 1: add a valid subscription
            System.out.println("=== Test 1: Add a valid subscription ===");
            int newId = service.addSubscription("Phase6 Test", new BigDecimal("99.00"),
                    Subscription.BillingCycle.MONTHLY, LocalDate.now().plusDays(10),
                    "test@example.com");
            System.out.println("Added with id " + newId);

            // Test 2: an invalid amount must be rejected
            System.out.println("\n=== Test 2: Reject an invalid amount ===");
            try {
                service.addSubscription("Bad", new BigDecimal("-5"),
                        Subscription.BillingCycle.MONTHLY, LocalDate.now().plusDays(1),
                        "test@example.com");
                System.out.println("FAIL: invalid amount was accepted");
            } catch (IllegalArgumentException e) {
                System.out.println("Rejected as expected: " + e.getMessage());
            }

            // Test 3: view all
            System.out.println("\n=== Test 3: All subscriptions ===");
            for (Subscription s : service.getAllSubscriptions()) {
                System.out.println(s);
            }

            // Test 4: upcoming renewals in the next 7 days
            System.out.println("\n=== Test 4: Active renewals in next 7 days ===");
            for (Subscription s : service.getUpcomingRenewals(7)) {
                System.out.println(s);
            }

            // Test 5: cancel the test subscription
            System.out.println("\n=== Test 5: Cancel subscription " + newId + " ===");
            System.out.println("First cancel:  " + service.cancelSubscription(newId));
            System.out.println("Second cancel: " + service.cancelSubscription(newId)
                    + " (already cancelled)");

            // Test 6: expenses (only ACTIVE subscriptions count)
            System.out.println("\n=== Test 6: Expenses ===");
            System.out.println("Monthly estimate: Rs. " + service.getMonthlyExpense());
            System.out.println("Yearly estimate:  Rs. " + service.getYearlyExpense());

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
