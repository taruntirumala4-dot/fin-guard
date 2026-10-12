-- ============================================================
--  FinGuard – Seed Data (MySQL 5.5+ compatible)
--  Synthetic data only – no real personal information
-- All passwords are BCrypt hashes of 'Password@123'
--  Usage: mysql -u root -p finguard < database/seed.sql
-- ============================================================

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
-- Disable FK checks during bulk insert for performance
SET foreign_key_checks = 0;

-- ─────────────────────────────────────────────────────────────
-- USERS (20 users: 1 admin, 3 analysts, 16 regular users)
-- BCrypt hash for 'Password@123'
-- ─────────────────────────────────────────────────────────────

INSERT INTO users (name, email, password_hash, role) VALUES
('Admin User',       'admin@finguard.io',        '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'ADMIN'),
('Priya Sharma',     'priya.sharma@finguard.io', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'ANALYST'),
('Rahul Verma',      'rahul.verma@finguard.io',  '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'ANALYST'),
('Ananya Singh',     'ananya.singh@finguard.io', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'ANALYST'),
('Vikram Patel',     'vikram.patel@example.com', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Sneha Iyer',       'sneha.iyer@example.com',   '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Arjun Mehta',      'arjun.mehta@example.com',  '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Kavya Reddy',      'kavya.reddy@example.com',  '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Rohan Gupta',      'rohan.gupta@example.com',  '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Meera Nair',       'meera.nair@example.com',   '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Aditya Kumar',     'aditya.kumar@example.com', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Pooja Desai',      'pooja.desai@example.com',  '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Sanjay Joshi',     'sanjay.joshi@example.com', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Divya Krishnan',   'divya.krishnan@example.com','$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER'),
('Kiran Malhotra',   'kiran.malhotra@example.com','$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER'),
('Neha Bhatt',       'neha.bhatt@example.com',   '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.', 'USER'),
('Ravi Chandran',    'ravi.chandran@example.com','$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER'),
('Sunita Agarwal',   'sunita.agarwal@example.com','$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER'),
('Tarun Khanna',     'tarun.khanna@example.com', '$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER'),
('Lakshmi Pillai',   'lakshmi.pillai@example.com','$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.','USER');

-- ─────────────────────────────────────────────────────────────
-- TRANSACTIONS (100+ rows, mix of SAFE / SUSPICIOUS / FRAUD)
-- NOTE: MySQL 5.5 interval syntax: INTERVAL N DAY, INTERVAL N HOUR, INTERVAL N MINUTE
--       Compound intervals are chained: NOW() - INTERVAL 30 DAY - INTERVAL 2 HOUR
-- ─────────────────────────────────────────────────────────────

INSERT INTO transactions
  (transaction_id, user_id, amount, currency, merchant, location, transaction_type, device_id, timestamp, status, fraud_score, fraud_status)
VALUES

-- ── Normal transactions (SAFE) ──────────────────────────────
('TXN-2026-000001', 5, 1500.00,  'INR', 'Amazon India',       'Hyderabad', 'PAYMENT',    'DEV-HP-LAPTOP-001', NOW() - INTERVAL 30 DAY, 'SUCCESS', 5.0,  'SAFE'),
('TXN-2026-000002', 5, 2200.00,  'INR', 'Swiggy',             'Hyderabad', 'PAYMENT',    'DEV-HP-LAPTOP-001', NOW() - INTERVAL 29 DAY, 'SUCCESS', 3.5,  'SAFE'),
('TXN-2026-000003', 5, 800.00,   'INR', 'Zomato',             'Hyderabad', 'PAYMENT',    'DEV-HP-LAPTOP-001', NOW() - INTERVAL 28 DAY, 'SUCCESS', 2.0,  'SAFE'),
('TXN-2026-000004', 6, 5000.00,  'INR', 'Flipkart',           'Mumbai',    'PAYMENT',    'DEV-IPHONE-002',    NOW() - INTERVAL 27 DAY, 'SUCCESS', 8.5,  'SAFE'),
('TXN-2026-000005', 6, 3200.00,  'INR', 'Reliance Digital',   'Mumbai',    'PAYMENT',    'DEV-IPHONE-002',    NOW() - INTERVAL 26 DAY, 'SUCCESS', 6.0,  'SAFE'),
('TXN-2026-000006', 7, 10000.00, 'INR', 'HDFC Bank Transfer', 'Delhi',     'TRANSFER',   'DEV-SAMSUNG-003',   NOW() - INTERVAL 25 DAY, 'SUCCESS', 12.0, 'SAFE'),
('TXN-2026-000007', 7, 1200.00,  'INR', 'Big Bazaar',         'Delhi',     'PAYMENT',    'DEV-SAMSUNG-003',   NOW() - INTERVAL 24 DAY, 'SUCCESS', 4.5,  'SAFE'),
('TXN-2026-000008', 8, 15000.00, 'INR', 'ICICI Bank ATM',     'Bangalore', 'WITHDRAWAL', 'DEV-ATM-CARD-004',  NOW() - INTERVAL 23 DAY, 'SUCCESS', 15.0, 'SAFE'),
('TXN-2026-000009', 8, 4500.00,  'INR', 'Ola Cabs',           'Bangalore', 'PAYMENT',    'DEV-SAMSUNG-003',   NOW() - INTERVAL 22 DAY, 'SUCCESS', 7.0,  'SAFE'),
('TXN-2026-000010', 9, 25000.00, 'INR', 'Hero MotoCorp',      'Pune',      'PAYMENT',    'DEV-HP-LAPTOP-005', NOW() - INTERVAL 21 DAY, 'SUCCESS', 18.0, 'SAFE'),
('TXN-2026-000011', 9, 500.00,   'INR', 'Paytm Recharge',     'Pune',      'PAYMENT',    'DEV-HP-LAPTOP-005', NOW() - INTERVAL 20 DAY, 'SUCCESS', 2.5,  'SAFE'),
('TXN-2026-000012', 10, 8000.00, 'INR', 'D-Mart',             'Chennai',   'PAYMENT',    'DEV-ONEPLUS-006',   NOW() - INTERVAL 19 DAY, 'SUCCESS', 9.0,  'SAFE'),
('TXN-2026-000013', 10, 2000.00, 'INR', 'Airtel Postpaid',    'Chennai',   'PAYMENT',    'DEV-ONEPLUS-006',   NOW() - INTERVAL 18 DAY, 'SUCCESS', 4.0,  'SAFE'),
('TXN-2026-000014', 11, 35000.00,'INR', 'LG Electronics',     'Kolkata',   'PAYMENT',    'DEV-DELL-007',      NOW() - INTERVAL 17 DAY, 'SUCCESS', 20.0, 'SAFE'),
('TXN-2026-000015', 11, 1800.00, 'INR', 'Dominos Pizza',      'Kolkata',   'PAYMENT',    'DEV-DELL-007',      NOW() - INTERVAL 16 DAY, 'SUCCESS', 5.5,  'SAFE'),
('TXN-2026-000016', 12, 6000.00, 'INR', 'MakeMyTrip',         'Jaipur',    'PAYMENT',    'DEV-IPHONE-008',    NOW() - INTERVAL 15 DAY, 'SUCCESS', 11.0, 'SAFE'),
('TXN-2026-000017', 12, 900.00,  'INR', 'Spotify Premium',    'Jaipur',    'PAYMENT',    'DEV-IPHONE-008',    NOW() - INTERVAL 14 DAY, 'SUCCESS', 3.0,  'SAFE'),
('TXN-2026-000018', 13, 45000.00,'INR', 'Apple India Store',  'Ahmedabad', 'PAYMENT',    'DEV-MACBOOK-009',   NOW() - INTERVAL 13 DAY, 'SUCCESS', 22.0, 'SAFE'),
('TXN-2026-000019', 13, 3500.00, 'INR', 'BookMyShow',         'Ahmedabad', 'PAYMENT',    'DEV-MACBOOK-009',   NOW() - INTERVAL 12 DAY, 'SUCCESS', 8.0,  'SAFE'),
('TXN-2026-000020', 14, 12000.00,'INR', 'Nykaa Fashion',      'Surat',     'PAYMENT',    'DEV-REDMI-010',     NOW() - INTERVAL 11 DAY, 'SUCCESS', 14.0, 'SAFE'),
('TXN-2026-000021', 14, 2500.00, 'INR', 'Myntra',             'Surat',     'PAYMENT',    'DEV-REDMI-010',     NOW() - INTERVAL 10 DAY, 'SUCCESS', 6.5,  'SAFE'),
('TXN-2026-000022', 15, 7500.00, 'INR', 'Decathlon',          'Lucknow',   'PAYMENT',    'DEV-PIXEL-011',     NOW() - INTERVAL 9 DAY,  'SUCCESS', 13.0, 'SAFE'),
('TXN-2026-000023', 15, 400.00,  'INR', 'Google Play Store',  'Lucknow',   'PAYMENT',    'DEV-PIXEL-011',     NOW() - INTERVAL 8 DAY,  'SUCCESS', 1.5,  'SAFE'),
('TXN-2026-000024', 16, 20000.00,'INR', 'IDFC Bank Transfer', 'Noida',     'TRANSFER',   'DEV-LENOVO-012',    NOW() - INTERVAL 7 DAY,  'SUCCESS', 16.0, 'SAFE'),
('TXN-2026-000025', 16, 1100.00, 'INR', 'Uber India',         'Noida',     'PAYMENT',    'DEV-LENOVO-012',    NOW() - INTERVAL 6 DAY,  'SUCCESS', 5.0,  'SAFE'),
('TXN-2026-000026', 17, 18000.00,'INR', 'Croma Electronics',  'Chandigarh','PAYMENT',    'DEV-ASUS-013',      NOW() - INTERVAL 5 DAY,  'SUCCESS', 17.0, 'SAFE'),
('TXN-2026-000027', 17, 3000.00, 'INR', 'Tata Cliq',          'Chandigarh','PAYMENT',    'DEV-ASUS-013',      NOW() - INTERVAL 4 DAY,  'SUCCESS', 7.5,  'SAFE'),
('TXN-2026-000028', 18, 5500.00, 'INR', 'Naukri Premium',     'Bhopal',    'PAYMENT',    'DEV-HP-14',         NOW() - INTERVAL 3 DAY,  'SUCCESS', 10.0, 'SAFE'),
('TXN-2026-000029', 18, 700.00,  'INR', 'Netflix India',      'Bhopal',    'PAYMENT',    'DEV-HP-14',         NOW() - INTERVAL 2 DAY,  'SUCCESS', 2.0,  'SAFE'),
('TXN-2026-000030', 19, 9000.00, 'INR', 'Bajaj Finance EMI',  'Indore',    'PAYMENT',    'DEV-VIVO-015',      NOW() - INTERVAL 1 DAY,  'SUCCESS', 12.5, 'SAFE'),

-- ── Suspicious Transactions ─────────────────────────────────
('TXN-2026-000031', 5,  85000.00, 'INR', 'International Forex Exchange', 'Delhi',     'TRANSFER',  'DEV-HP-LAPTOP-001', NOW() - INTERVAL 25 DAY, 'SUCCESS', 45.0, 'SUSPICIOUS'),
('TXN-2026-000032', 6,  75000.00, 'INR', 'Crypto Exchange India',        'Bangalore', 'PAYMENT',   'DEV-IPHONE-002',    NOW() - INTERVAL 22 DAY, 'SUCCESS', 52.0, 'SUSPICIOUS'),
('TXN-2026-000033', 7,  92000.00, 'INR', 'Jewellery World',              'Mumbai',    'PAYMENT',   'DEV-NEW-DEVICE-X',  NOW() - INTERVAL 18 DAY, 'SUCCESS', 58.0, 'SUSPICIOUS'),
('TXN-2026-000034', 8,  60000.00, 'INR', 'Online Casino Royale',         'Goa',       'PAYMENT',   'DEV-ATM-CARD-004',  NOW() - INTERVAL 15 DAY, 'SUCCESS', 48.0, 'SUSPICIOUS'),
('TXN-2026-000035', 9,  55000.00, 'INR', 'Peer to Peer Transfer',        'Hyderabad', 'TRANSFER',  'DEV-HP-LAPTOP-005', NOW() - INTERVAL 12 DAY, 'SUCCESS', 42.0, 'SUSPICIOUS'),
('TXN-2026-000036', 10, 78000.00, 'INR', 'Dark Web Marketplace',         'Unknown',   'PAYMENT',   'DEV-TOR-BROWSER',   NOW() - INTERVAL 10 DAY, 'BLOCKED', 59.0, 'SUSPICIOUS'),
('TXN-2026-000037', 11, 50000.00, 'INR', 'Luxury Watch Store',           'Singapore', 'PAYMENT',   'DEV-DELL-007',      NOW() - INTERVAL 8 DAY,  'SUCCESS', 44.0, 'SUSPICIOUS'),
('TXN-2026-000038', 12, 65000.00, 'INR', 'NFT Platform',                 'Dubai',     'PAYMENT',   'DEV-UNKNOWN-NEW',   NOW() - INTERVAL 6 DAY,  'SUCCESS', 53.0, 'SUSPICIOUS'),
('TXN-2026-000039', 13, 48000.00, 'INR', 'Offshore Investment',          'Cayman',    'TRANSFER',  'DEV-MACBOOK-009',   NOW() - INTERVAL 4 DAY,  'PENDING', 41.0, 'SUSPICIOUS'),
('TXN-2026-000040', 14, 99000.00, 'INR', 'Bulk Electronics Dealer',      'Delhi',     'PAYMENT',   'DEV-NEW-MOBILE-Y',  NOW() - INTERVAL 2 DAY,  'SUCCESS', 57.0, 'SUSPICIOUS'),

-- ── Fraud Transactions ──────────────────────────────────────
('TXN-2026-000041', 5,  250000.00,'INR', 'Shell Corp Transfer',    'Dubai',       'TRANSFER',   'DEV-UNKNOWN-A',     NOW() - INTERVAL 20 DAY, 'BLOCKED', 87.0, 'FRAUD'),
('TXN-2026-000042', 6,  180000.00,'INR', 'Bitcoin ATM',            'Singapore',   'WITHDRAWAL', 'DEV-NEW-DEVICE-B',  NOW() - INTERVAL 18 DAY, 'BLOCKED', 91.0, 'FRAUD'),
('TXN-2026-000043', 7,  320000.00,'INR', 'International Wire',     'Switzerland', 'TRANSFER',   'DEV-SAMSUNG-003',   NOW() - INTERVAL 16 DAY, 'BLOCKED', 95.0, 'FRAUD'),
('TXN-2026-000044', 8,  150000.00,'INR', 'Ponzi Investment',       'Lagos',       'PAYMENT',    'DEV-HACKED-C',      NOW() - INTERVAL 14 DAY, 'BLOCKED', 88.0, 'FRAUD'),
('TXN-2026-000045', 9,  500000.00,'INR', 'Ransomware Payment',     'Unknown',     'PAYMENT',    'DEV-COMPROMISED-D', NOW() - INTERVAL 12 DAY, 'BLOCKED', 98.0, 'FRAUD'),
('TXN-2026-000046', 10, 200000.00,'INR', 'Fake E-Commerce',        'Russia',      'PAYMENT',    'DEV-TOR-BROWSER',   NOW() - INTERVAL 10 DAY, 'BLOCKED', 93.0, 'FRAUD'),
('TXN-2026-000047', 11, 175000.00,'INR', 'Money Mule Transfer',    'UK',          'TRANSFER',   'DEV-NEW-DEVICE-E',  NOW() - INTERVAL 8 DAY,  'BLOCKED', 86.0, 'FRAUD'),
('TXN-2026-000048', 12, 400000.00,'INR', 'Fake Investment Scheme', 'Cayman',      'TRANSFER',   'DEV-UNKNOWN-F',     NOW() - INTERVAL 6 DAY,  'BLOCKED', 96.0, 'FRAUD'),
('TXN-2026-000049', 13, 120000.00,'INR', 'Phishing Site Purchase', 'Unknown',     'PAYMENT',    'DEV-HACKED-G',      NOW() - INTERVAL 4 DAY,  'BLOCKED', 82.0, 'FRAUD'),
('TXN-2026-000050', 14, 280000.00,'INR', 'Account Takeover',       'China',       'WITHDRAWAL', 'DEV-HACKED-H',      NOW() - INTERVAL 2 DAY,  'BLOCKED', 97.0, 'FRAUD'),

-- ── More normal transactions for realistic volume ────────────
('TXN-2026-000051', 5,  1000.00,  'INR', 'Paytm Mall',       'Hyderabad', 'PAYMENT',  'DEV-HP-LAPTOP-001', NOW() - INTERVAL 29 DAY - INTERVAL 2 HOUR,  'SUCCESS', 3.0,  'SAFE'),
('TXN-2026-000052', 6,  4000.00,  'INR', 'IRCTC',            'Mumbai',    'PAYMENT',  'DEV-IPHONE-002',    NOW() - INTERVAL 28 DAY - INTERVAL 3 HOUR,  'SUCCESS', 7.0,  'SAFE'),
('TXN-2026-000053', 7,  6500.00,  'INR', 'BSNL Bill',        'Delhi',     'PAYMENT',  'DEV-SAMSUNG-003',   NOW() - INTERVAL 27 DAY - INTERVAL 4 HOUR,  'SUCCESS', 9.0,  'SAFE'),
('TXN-2026-000054', 8,  2800.00,  'INR', 'HDFC Credit Card', 'Bangalore', 'PAYMENT',  'DEV-ATM-CARD-004',  NOW() - INTERVAL 26 DAY - INTERVAL 5 HOUR,  'SUCCESS', 6.0,  'SAFE'),
('TXN-2026-000055', 9,  19000.00, 'INR', 'Maruti Suzuki',    'Pune',      'PAYMENT',  'DEV-HP-LAPTOP-005', NOW() - INTERVAL 25 DAY - INTERVAL 6 HOUR,  'SUCCESS', 16.0, 'SAFE'),
('TXN-2026-000056', 10, 3200.00,  'INR', 'Jio Postpaid',     'Chennai',   'PAYMENT',  'DEV-ONEPLUS-006',   NOW() - INTERVAL 24 DAY - INTERVAL 7 HOUR,  'SUCCESS', 5.0,  'SAFE'),
('TXN-2026-000057', 11, 750.00,   'INR', 'Hotstar Premium',  'Kolkata',   'PAYMENT',  'DEV-DELL-007',      NOW() - INTERVAL 23 DAY - INTERVAL 8 HOUR,  'SUCCESS', 2.0,  'SAFE'),
('TXN-2026-000058', 12, 8500.00,  'INR', 'Tanishq Jewels',   'Jaipur',    'PAYMENT',  'DEV-IPHONE-008',    NOW() - INTERVAL 22 DAY - INTERVAL 9 HOUR,  'SUCCESS', 14.0, 'SAFE'),
('TXN-2026-000059', 13, 1600.00,  'INR', 'Haldirams',        'Ahmedabad', 'PAYMENT',  'DEV-MACBOOK-009',   NOW() - INTERVAL 21 DAY - INTERVAL 10 HOUR, 'SUCCESS', 4.0,  'SAFE'),
('TXN-2026-000060', 14, 22000.00, 'INR', 'Hyundai Service',  'Surat',     'PAYMENT',  'DEV-REDMI-010',     NOW() - INTERVAL 20 DAY - INTERVAL 11 HOUR, 'SUCCESS', 18.0, 'SAFE'),
('TXN-2026-000061', 15, 3800.00,  'INR', 'CEAT Tyres',       'Lucknow',   'PAYMENT',  'DEV-PIXEL-011',     NOW() - INTERVAL 19 DAY - INTERVAL 12 HOUR, 'SUCCESS', 8.0,  'SAFE'),
('TXN-2026-000062', 16, 5200.00,  'INR', 'LIC Premium',      'Noida',     'PAYMENT',  'DEV-LENOVO-012',    NOW() - INTERVAL 18 DAY - INTERVAL 13 HOUR, 'SUCCESS', 10.0, 'SAFE'),
('TXN-2026-000063', 17, 1300.00,  'INR', 'SBI NEFT Transfer','Chandigarh','TRANSFER', 'DEV-ASUS-013',      NOW() - INTERVAL 17 DAY - INTERVAL 14 HOUR, 'SUCCESS', 5.5,  'SAFE'),
('TXN-2026-000064', 18, 6800.00,  'INR', 'Boat Headphones',  'Bhopal',    'PAYMENT',  'DEV-HP-14',         NOW() - INTERVAL 16 DAY - INTERVAL 15 HOUR, 'SUCCESS', 11.0, 'SAFE'),
('TXN-2026-000065', 19, 4100.00,  'INR', 'Nykaa Beauty',     'Indore',    'PAYMENT',  'DEV-VIVO-015',      NOW() - INTERVAL 15 DAY - INTERVAL 16 HOUR, 'SUCCESS', 7.5,  'SAFE'),
('TXN-2026-000066', 20, 28000.00, 'INR', 'Samsung Galaxy',   'Kochi',     'PAYMENT',  'DEV-SAMSUNG-016',   NOW() - INTERVAL 14 DAY - INTERVAL 17 HOUR, 'SUCCESS', 21.0, 'SAFE'),
('TXN-2026-000067', 5,  2100.00,  'INR', 'Starbucks',        'Hyderabad', 'PAYMENT',  'DEV-HP-LAPTOP-001', NOW() - INTERVAL 13 DAY - INTERVAL 18 HOUR, 'SUCCESS', 4.5,  'SAFE'),
('TXN-2026-000068', 6,  900.00,   'INR', 'McDonald''s',      'Mumbai',    'PAYMENT',  'DEV-IPHONE-002',    NOW() - INTERVAL 12 DAY - INTERVAL 19 HOUR, 'SUCCESS', 2.5,  'SAFE'),
('TXN-2026-000069', 7,  11000.00, 'INR', 'Axis Bank Transfer','Delhi',    'TRANSFER', 'DEV-SAMSUNG-003',   NOW() - INTERVAL 11 DAY - INTERVAL 20 HOUR, 'SUCCESS', 13.5, 'SAFE'),
('TXN-2026-000070', 8,  1700.00,  'INR', 'Apollo Pharmacy',  'Bangalore', 'PAYMENT',  'DEV-ATM-CARD-004',  NOW() - INTERVAL 10 DAY - INTERVAL 21 HOUR, 'SUCCESS', 5.0,  'SAFE'),

-- ── Rapid Transaction Scenario (Scenario 3) ─────────────────
-- User 5 makes 7 transactions within a few minutes (flagged)
('TXN-2026-000071', 5, 5000.00, 'INR', 'Store A', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR,                                  'SUCCESS', 35.0, 'SUSPICIOUS'),
('TXN-2026-000072', 5, 4500.00, 'INR', 'Store B', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 1 MINUTE,              'SUCCESS', 38.0, 'SUSPICIOUS'),
('TXN-2026-000073', 5, 3200.00, 'INR', 'Store C', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 2 MINUTE,              'SUCCESS', 40.0, 'SUSPICIOUS'),
('TXN-2026-000074', 5, 6000.00, 'INR', 'Store D', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 3 MINUTE,              'SUCCESS', 42.0, 'SUSPICIOUS'),
('TXN-2026-000075', 5, 2800.00, 'INR', 'Store E', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 4 MINUTE,              'SUCCESS', 44.0, 'SUSPICIOUS'),
('TXN-2026-000076', 5, 5500.00, 'INR', 'Store F', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 5 MINUTE,              'SUCCESS', 46.0, 'SUSPICIOUS'),
('TXN-2026-000077', 5, 4800.00, 'INR', 'Store G', 'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR - INTERVAL 6 MINUTE,              'BLOCKED', 72.0, 'FRAUD'),

-- ── Location Mismatch Scenario (Scenario 4) ─────────────────
-- User 6: Hyderabad → Delhi within 10 minutes (impossible travel)
('TXN-2026-000078', 6, 8000.00,  'INR', 'Local Market',    'Hyderabad', 'PAYMENT', 'DEV-IPHONE-002', NOW() - INTERVAL 3 DAY - INTERVAL 14 HOUR,                             'SUCCESS', 10.0, 'SAFE'),
('TXN-2026-000079', 6, 45000.00, 'INR', 'Electronics Hub', 'Delhi',     'PAYMENT', 'DEV-IPHONE-002', NOW() - INTERVAL 3 DAY - INTERVAL 13 HOUR - INTERVAL 50 MINUTE,        'BLOCKED', 76.0, 'FRAUD'),

-- ── Device Change Scenario (Scenario 5) ─────────────────────
-- User 7: known device → sudden new device
('TXN-2026-000080', 7, 12000.00, 'INR', 'Reliance Trends', 'Delhi', 'PAYMENT', 'DEV-SAMSUNG-003',     NOW() - INTERVAL 2 DAY - INTERVAL 16 HOUR,                            'SUCCESS', 11.0, 'SAFE'),
('TXN-2026-000081', 7, 95000.00, 'INR', 'Luxury Store',    'Delhi', 'PAYMENT', 'DEV-NEW-UNKNOWN-XYZ', NOW() - INTERVAL 2 DAY - INTERVAL 15 HOUR,                            'BLOCKED', 68.0, 'FRAUD'),

-- ── Multi-Risk Scenario (Scenario 6) ────────────────────────
-- High amount + new device + location mismatch + high frequency
('TXN-2026-000082', 8, 350000.00,'INR', 'Offshore Wire', 'Unknown', 'TRANSFER', 'DEV-COMPROMISED-NEW', NOW() - INTERVAL 1 DAY - INTERVAL 12 HOUR,                           'BLOCKED', 97.5, 'FRAUD'),

-- ── More normal filler ───────────────────────────────────────
('TXN-2026-000083', 9,  1400.00,  'INR', 'Pizza Hut',        'Pune',      'PAYMENT', 'DEV-HP-LAPTOP-005', NOW() - INTERVAL 29 DAY - INTERVAL 1 HOUR,  'SUCCESS', 3.5,  'SAFE'),
('TXN-2026-000084', 10, 3600.00,  'INR', 'HP Printer',       'Chennai',   'PAYMENT', 'DEV-ONEPLUS-006',   NOW() - INTERVAL 28 DAY - INTERVAL 2 HOUR,  'SUCCESS', 7.0,  'SAFE'),
('TXN-2026-000085', 11, 2300.00,  'INR', 'Zerodha',          'Kolkata',   'PAYMENT', 'DEV-DELL-007',      NOW() - INTERVAL 27 DAY - INTERVAL 3 HOUR,  'SUCCESS', 5.5,  'SAFE'),
('TXN-2026-000086', 12, 14000.00, 'INR', 'Skybags Luggage',  'Jaipur',    'PAYMENT', 'DEV-IPHONE-008',    NOW() - INTERVAL 26 DAY - INTERVAL 4 HOUR,  'SUCCESS', 15.0, 'SAFE'),
('TXN-2026-000087', 13, 800.00,   'INR', 'Chai Point',       'Ahmedabad', 'PAYMENT', 'DEV-MACBOOK-009',   NOW() - INTERVAL 25 DAY - INTERVAL 5 HOUR,  'SUCCESS', 2.0,  'SAFE'),
('TXN-2026-000088', 14, 7200.00,  'INR', 'Woodland Shoes',   'Surat',     'PAYMENT', 'DEV-REDMI-010',     NOW() - INTERVAL 24 DAY - INTERVAL 6 HOUR,  'SUCCESS', 12.0, 'SAFE'),
('TXN-2026-000089', 15, 4300.00,  'INR', 'Urban Ladder',     'Lucknow',   'PAYMENT', 'DEV-PIXEL-011',     NOW() - INTERVAL 23 DAY - INTERVAL 7 HOUR,  'SUCCESS', 8.5,  'SAFE'),
('TXN-2026-000090', 16, 16000.00, 'INR', 'Boat Smartwatch',  'Noida',     'PAYMENT', 'DEV-LENOVO-012',    NOW() - INTERVAL 22 DAY - INTERVAL 8 HOUR,  'SUCCESS', 17.0, 'SAFE'),
('TXN-2026-000091', 17, 600.00,   'INR', 'Kindle Purchase',  'Chandigarh','PAYMENT', 'DEV-ASUS-013',      NOW() - INTERVAL 21 DAY - INTERVAL 9 HOUR,  'SUCCESS', 2.5,  'SAFE'),
('TXN-2026-000092', 18, 9500.00,  'INR', 'Prestige Cookware','Bhopal',    'PAYMENT', 'DEV-HP-14',         NOW() - INTERVAL 20 DAY - INTERVAL 10 HOUR, 'SUCCESS', 13.0, 'SAFE'),
('TXN-2026-000093', 19, 2700.00,  'INR', 'Mama Earth',       'Indore',    'PAYMENT', 'DEV-VIVO-015',      NOW() - INTERVAL 19 DAY - INTERVAL 11 HOUR, 'SUCCESS', 6.0,  'SAFE'),
('TXN-2026-000094', 20, 38000.00, 'INR', 'Noise Smartwatch', 'Kochi',     'PAYMENT', 'DEV-SAMSUNG-016',   NOW() - INTERVAL 18 DAY - INTERVAL 12 HOUR, 'SUCCESS', 22.0, 'SAFE'),
('TXN-2026-000095', 5,  1950.00,  'INR', 'Pizza Express',    'Hyderabad', 'PAYMENT', 'DEV-HP-LAPTOP-001', NOW() - INTERVAL 17 DAY - INTERVAL 13 HOUR, 'SUCCESS', 4.0,  'SAFE'),
('TXN-2026-000096', 6,  6200.00,  'INR', 'Ashok Leyland',    'Mumbai',    'PAYMENT', 'DEV-IPHONE-002',    NOW() - INTERVAL 16 DAY - INTERVAL 14 HOUR, 'SUCCESS', 11.5, 'SAFE'),
('TXN-2026-000097', 7,  3100.00,  'INR', 'Indian Oil Corp',  'Delhi',     'PAYMENT', 'DEV-SAMSUNG-003',   NOW() - INTERVAL 15 DAY - INTERVAL 15 HOUR, 'SUCCESS', 7.0,  'SAFE'),
('TXN-2026-000098', 8,  5800.00,  'INR', 'Taj Hotels',       'Bangalore', 'PAYMENT', 'DEV-ATM-CARD-004',  NOW() - INTERVAL 14 DAY - INTERVAL 16 HOUR, 'SUCCESS', 10.5, 'SAFE'),
('TXN-2026-000099', 9,  1250.00,  'INR', 'CCD Coffee',       'Pune',      'PAYMENT', 'DEV-HP-LAPTOP-005', NOW() - INTERVAL 13 DAY - INTERVAL 17 HOUR, 'SUCCESS', 3.0,  'SAFE'),
('TXN-2026-000100', 10, 42000.00, 'INR', 'Godrej Appliances','Chennai',   'PAYMENT', 'DEV-ONEPLUS-006',   NOW() - INTERVAL 12 DAY - INTERVAL 18 HOUR, 'SUCCESS', 23.0, 'SAFE');

-- ─────────────────────────────────────────────────────────────
-- FRAUD ALERTS
-- ─────────────────────────────────────────────────────────────

INSERT INTO fraud_alerts (transaction_id, alert_type, severity, reason, fraud_score, status) VALUES
('TXN-2026-000041', 'HIGH_AMOUNT',       'CRITICAL', 'Transaction amount Rs.250,000 exceeds threshold. Shell company transfer to offshore account.',  87.0, 'INVESTIGATING'),
('TXN-2026-000042', 'DEVICE_CHANGE',     'CRITICAL', 'Transaction from new unrecognized device. Bitcoin ATM withdrawal in Singapore.',                91.0, 'OPEN'),
('TXN-2026-000043', 'HIGH_AMOUNT',       'CRITICAL', 'International wire transfer Rs.320,000 to Switzerland. Multiple risk factors triggered.',       95.0, 'INVESTIGATING'),
('TXN-2026-000044', 'LOCATION_MISMATCH', 'HIGH',     'Transaction in Lagos – unusual geography for account. Ponzi investment pattern detected.',      88.0, 'OPEN'),
('TXN-2026-000045', 'HIGH_AMOUNT',       'CRITICAL', 'Transaction Rs.500,000 to unknown recipient. Ransomware payment pattern.',                      98.0, 'RESOLVED'),
('TXN-2026-000046', 'VELOCITY',          'CRITICAL', 'High velocity transactions from TOR browser IP. Dark web marketplace payment.',                  93.0, 'RESOLVED'),
('TXN-2026-000047', 'DEVICE_CHANGE',     'HIGH',     'Money mule transfer pattern detected. New device used for large international transfer.',        86.0, 'INVESTIGATING'),
('TXN-2026-000048', 'HIGH_AMOUNT',       'CRITICAL', 'Investment scheme payment Rs.400,000 to Cayman Islands account.',                               96.0, 'OPEN'),
('TXN-2026-000049', 'DEVICE_CHANGE',     'HIGH',     'Account accessed from compromised device. Phishing site purchase detected.',                    82.0, 'FALSE_POSITIVE'),
('TXN-2026-000050', 'LOCATION_MISMATCH', 'CRITICAL', 'Account takeover suspected. Transaction from China – account typically used in India.',          97.0, 'INVESTIGATING'),
('TXN-2026-000077', 'RAPID_TRANSACTION', 'HIGH',     'User made 7 transactions within 6 minutes. Velocity rule triggered after 5 transactions.',      72.0, 'OPEN'),
('TXN-2026-000079', 'LOCATION_MISMATCH', 'CRITICAL', 'Impossible travel detected: Hyderabad at 14:00, Delhi at 14:10 (1,500 km in 10 minutes).',     76.0, 'OPEN'),
('TXN-2026-000081', 'DEVICE_CHANGE',     'HIGH',     'Transaction from unrecognized device DEV-NEW-UNKNOWN-XYZ. Known device: DEV-SAMSUNG-003.',      68.0, 'OPEN'),
('TXN-2026-000082', 'HIGH_AMOUNT',       'CRITICAL', 'Multiple risk factors: Rs.350,000 amount + new device + unknown location. Fraud score 97.5.',   97.5, 'INVESTIGATING');

-- Re-enable FK checks
SET foreign_key_checks = 1;
