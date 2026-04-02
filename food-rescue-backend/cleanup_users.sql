-- Delete all non-admin users from food_rescue database
DELETE FROM users WHERE role != 'ADMIN';

-- Verify only admin remains
SELECT id, name, email, role, latitude, longitude FROM users;
