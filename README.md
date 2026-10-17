# Subscription Reminder Manager

A Java console application that stores recurring subscriptions (Netflix, Spotify, gym, domains, etc.)
in MySQL and automatically sends an email reminder one day before each subscription renews.

**Tech stack:** Java 17+, MySQL, JDBC, Maven, Jakarta Mail (Gmail SMTP), `ScheduledExecutorService`

## Quick start

```bash
# 1. Create the database and table
mysql -u root < database/schema.sql

# 2. (Optional) add sample data - edit the email address first
mysql -u root < database/sample_data.sql

# 3. Build and run
mvn -q compile exec:java
```

## Project structure

```
database/schema.sql                 MySQL database + table
database/sample_data.sql            Optional test rows
src/main/java/com/subscription/
    Subscription.java               Model (one subscription)
    DBConnection.java               Opens the MySQL connection
    SubscriptionService.java        Add / view / cancel / expenses
    EmailService.java               Sends reminder emails via Gmail
    ReminderService.java            Finds tomorrow's renewals, sends reminders
    ReminderScheduler.java          Runs the reminder check daily
    Main.java                       Console menu
```

Full setup instructions, configuration and interview notes will be added as the project is completed.
