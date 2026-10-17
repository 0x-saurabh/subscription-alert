package com.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * All database operations for subscriptions: add, view, cancel,
 * upcoming renewals and expense totals.
 */
public class SubscriptionService {

    // Simple email check: something@something.something (no spaces)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // DECIMAL(10,2) in MySQL can hold at most 99,999,999.99
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    // ---------- Add ----------

    /** Validates the input, saves a new ACTIVE subscription and returns its new id. */
    public int addSubscription(String name, BigDecimal amount, Subscription.BillingCycle billingCycle,
                               LocalDate renewalDate, String email) throws SQLException {
        validate(name, amount, billingCycle, renewalDate, email);

        String sql = "INSERT INTO subscriptions (name, amount, billing_cycle, renewal_date, email) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name.trim());
            ps.setBigDecimal(2, amount);
            ps.setString(3, billingCycle.name());
            ps.setDate(4, Date.valueOf(renewalDate));
            ps.setString(5, email.trim());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // ---------- View ----------

    /** Returns every subscription (active and cancelled), soonest renewal first. */
    public List<Subscription> getAllSubscriptions() throws SQLException {
        String sql = "SELECT * FROM subscriptions ORDER BY renewal_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return readList(rs);
        }
    }

    /** Returns ACTIVE subscriptions renewing from today up to 'days' days from now. */
    public List<Subscription> getUpcomingRenewals(int days) throws SQLException {
        String sql = "SELECT * FROM subscriptions "
                + "WHERE status = 'ACTIVE' AND renewal_date BETWEEN ? AND ? "
                + "ORDER BY renewal_date";

        LocalDate today = LocalDate.now();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(today));
            ps.setDate(2, Date.valueOf(today.plusDays(days)));

            try (ResultSet rs = ps.executeQuery()) {
                return readList(rs);
            }
        }
    }

    // ---------- Cancel ----------

    /**
     * Marks a subscription as CANCELLED (soft delete - the row is kept).
     * Returns true if it was cancelled, false if the id does not exist or is already cancelled.
     */
    public boolean cancelSubscription(int id) throws SQLException {
        String sql = "UPDATE subscriptions SET status = 'CANCELLED' "
                + "WHERE id = ? AND status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rowsChanged = ps.executeUpdate();
            return rowsChanged > 0;
        }
    }

    // ---------- Expenses ----------

    /** Estimated monthly cost of all ACTIVE subscriptions (yearly ones divided by 12). */
    public BigDecimal getMonthlyExpense() throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription s : getActiveSubscriptions()) {
            total = total.add(s.getMonthlyEquivalent());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Estimated yearly cost: monthly plans x 12, plus yearly plans as they are. */
    public BigDecimal getYearlyExpense() throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription s : getActiveSubscriptions()) {
            if (s.getBillingCycle() == Subscription.BillingCycle.MONTHLY) {
                total = total.add(s.getAmount().multiply(BigDecimal.valueOf(12)));
            } else {
                total = total.add(s.getAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Helpers ----------

    private List<Subscription> getActiveSubscriptions() throws SQLException {
        String sql = "SELECT * FROM subscriptions WHERE status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return readList(rs);
        }
    }

    /** Throws IllegalArgumentException with a clear message if any input is invalid. */
    private void validate(String name, BigDecimal amount, Subscription.BillingCycle billingCycle,
                          LocalDate renewalDate, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Name must be at most 100 characters.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount can have at most 2 decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount is too large.");
        }
        if (billingCycle == null) {
            throw new IllegalArgumentException("Billing cycle must be MONTHLY or YEARLY.");
        }
        if (renewalDate == null || renewalDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Renewal date cannot be in the past.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email address is not valid.");
        }
    }

    private List<Subscription> readList(ResultSet rs) throws SQLException {
        List<Subscription> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    /** Converts the current database row into a Subscription object. */
    static Subscription mapRow(ResultSet rs) throws SQLException {
        Date sentFor = rs.getDate("reminder_sent_for"); // may be NULL

        return new Subscription(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getBigDecimal("amount"),
                Subscription.BillingCycle.valueOf(rs.getString("billing_cycle")),
                rs.getDate("renewal_date").toLocalDate(),
                rs.getString("email"),
                Subscription.Status.valueOf(rs.getString("status")),
                sentFor == null ? null : sentFor.toLocalDate());
    }
}
