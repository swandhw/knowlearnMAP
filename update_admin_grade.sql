-- Update admin user grade to PRO
UPDATE users SET grade = 'PRO' WHERE username = 'admin';

-- Verify the change
SELECT username, email, grade, role FROM users WHERE username = 'admin';
