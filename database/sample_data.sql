-- Optional test data. Run this only once, or you will get duplicate rows.
-- Change the email address to your own before running.

USE subscription_manager;

INSERT INTO subscriptions (name, amount, billing_cycle, renewal_date, email)
VALUES
    ('Spotify', 119.00,  'MONTHLY', CURDATE() + INTERVAL 1 DAY,   'your.email@gmail.com'),
    ('Netflix', 649.00,  'MONTHLY', CURDATE() + INTERVAL 5 DAY,   'your.email@gmail.com'),
    ('Domain',  899.00,  'YEARLY',  CURDATE() + INTERVAL 60 DAY,  'your.email@gmail.com'),
    ('Gym',     1500.00, 'MONTHLY', CURDATE() + INTERVAL 1 DAY,   'your.email@gmail.com');

-- Cancel the gym so we can later check that cancelled subscriptions get no reminder.
UPDATE subscriptions SET status = 'CANCELLED' WHERE name = 'Gym';
