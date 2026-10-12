-- ============================================================
--  FinGuard – Fix broken seeded passwords
--  The original seed.sql contained a BCrypt hash that does NOT
--  match 'Password@123', so all demo accounts failed to log in.
--
--  Usage:  mysql -u <user> -p finguard < database/fix-passwords.sql
-- ============================================================

-- BCrypt hash of 'Password@123' (cost 12, verified)
UPDATE users
SET password_hash = '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.'
WHERE role IN ('ADMIN', 'ANALYST', 'USER');
