package com.finguard.config;

import com.finguard.model.FraudAlert;
import com.finguard.model.Transaction;
import com.finguard.model.User;
import com.finguard.repository.FraudAlertRepository;
import com.finguard.repository.TransactionRepository;
import com.finguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSeeder – seeds the SQL database on first startup.
 *
 * Ports the original database/seed.sql synthetic data into SQL:
 *   - 20 users (1 admin, 3 analysts, 16 regular users)
 *   - 100+ transactions (SAFE / SUSPICIOUS / FRAUD mix)
 *   - 14 fraud alerts
 *
 * All passwords are BCrypt hashes of 'Password@123'.
 * Runs only when the users table is empty (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String PASSWORD_HASH =
        "$2a$12$28kBM1lPGfJKRS/esBB4D.h1oNUn4krEF7iGps91GUH4dDiVNFy4.";

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    
    private final Map<String, User> seedUsersMap = new HashMap<>();  // internal mapping, not injected

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("SQL database already seeded ({} users found) – skipping", userRepository.count());
            return;
        }

        log.info("Seeding SQL database with FinGuard demo data...");
        seedUsers();
        seedTransactions();
        seedAlerts();
        log.info("SQL database seeding complete: users={} transactions={} alerts={}",
            userRepository.count(), transactionRepository.count(), fraudAlertRepository.count());
    }

    // ─── Users ─────────────────────────────────────────────────

    private void seedUsers() {
        Object[][] users = {
            // seedId, name, email, role
            {"1",  "Admin User",       "admin@finguard.io",        "ADMIN"},
            {"2",  "Priya Sharma",     "priya.sharma@finguard.io", "ANALYST"},
            {"3",  "Rahul Verma",      "rahul.verma@finguard.io",  "ANALYST"},
            {"4",  "Ananya Singh",     "ananya.singh@finguard.io", "ANALYST"},
            {"5",  "Vikram Patel",     "vikram.patel@example.com", "USER"},
            {"6",  "Sneha Iyer",       "sneha.iyer@example.com",   "USER"},
            {"7",  "Arjun Mehta",      "arjun.mehta@example.com",  "USER"},
            {"8",  "Kavya Reddy",      "kavya.reddy@example.com",  "USER"},
            {"9",  "Rohan Gupta",      "rohan.gupta@example.com",  "USER"},
            {"10", "Meera Nair",       "meera.nair@example.com",   "USER"},
            {"11", "Aditya Kumar",     "aditya.kumar@example.com", "USER"},
            {"12", "Pooja Desai",      "pooja.desai@example.com",  "USER"},
            {"13", "Sanjay Joshi",     "sanjay.joshi@example.com", "USER"},
            {"14", "Divya Krishnan",   "divya.krishnan@example.com", "USER"},
            {"15", "Kiran Malhotra",   "kiran.malhotra@example.com", "USER"},
            {"16", "Neha Bhatt",       "neha.bhatt@example.com",   "USER"},
            {"17", "Ravi Chandran",    "ravi.chandran@example.com", "USER"},
            {"18", "Sunita Agarwal",   "sunita.agarwal@example.com", "USER"},
            {"19", "Tarun Khanna",     "tarun.khanna@example.com", "USER"},
            {"20", "Lakshmi Pillai",   "lakshmi.pillai@example.com", "USER"},
            {"21", "Tarun Tirumala",   "taruntirumala4@gmail.com", "ADMIN"},
        };

        for (Object[] u : users) {
            User user = User.builder()
                .name((String) u[1])
                .email((String) u[2])
                .passwordHash(PASSWORD_HASH)
                .role(User.Role.valueOf((String) u[3]))
                .createdAt(Instant.now().minus(Duration.ofDays(90)))
                .build();
            User saved = userRepository.save(user);
            seedUsersMap.put((String) u[0], saved);
        }
    }

    // ─── Transactions ──────────────────────────────────────────

    private void seedTransactions() {
        // (transactionId, seedUserId, amount, currency, merchant, location, type, deviceId, timestamp(Instant), status, fraudScore, fraudStatus)
        seedTx("TXN-2026-000001", "5",  1500.00,  "Amazon India",        "Hyderabad",  "PAYMENT",    "DEV-HP-LAPTOP-001", days(30),         "SUCCESS", 5.0,  "SAFE");
        seedTx("TXN-2026-000002", "5",  2200.00,  "Swiggy",              "Hyderabad",  "PAYMENT",    "DEV-HP-LAPTOP-001", days(29),         "SUCCESS", 3.5,  "SAFE");
        seedTx("TXN-2026-000003", "5",  800.00,   "Zomato",              "Hyderabad",  "PAYMENT",    "DEV-HP-LAPTOP-001", days(28),         "SUCCESS", 2.0,  "SAFE");
        seedTx("TXN-2026-000004", "6",  5000.00,  "Flipkart",            "Mumbai",     "PAYMENT",    "DEV-IPHONE-002",    days(27),         "SUCCESS", 8.5,  "SAFE");
        seedTx("TXN-2026-000005", "6",  3200.00,  "Reliance Digital",    "Mumbai",     "PAYMENT",    "DEV-IPHONE-002",    days(26),         "SUCCESS", 6.0,  "SAFE");
        seedTx("TXN-2026-000006", "7",  10000.00, "HDFC Bank Transfer",  "Delhi",      "TRANSFER",   "DEV-SAMSUNG-003",   days(25),         "SUCCESS", 12.0, "SAFE");
        seedTx("TXN-2026-000007", "7",  1200.00,  "Big Bazaar",          "Delhi",      "PAYMENT",    "DEV-SAMSUNG-003",   days(24),         "SUCCESS", 4.5,  "SAFE");
        seedTx("TXN-2026-000008", "8",  15000.00, "ICICI Bank ATM",      "Bangalore",  "WITHDRAWAL", "DEV-ATM-CARD-004",  days(23),         "SUCCESS", 15.0, "SAFE");
        seedTx("TXN-2026-000009", "8",  4500.00,  "Ola Cabs",            "Bangalore",  "PAYMENT",    "DEV-SAMSUNG-003",   days(22),         "SUCCESS", 7.0,  "SAFE");
        seedTx("TXN-2026-000010", "9",  25000.00, "Hero MotoCorp",       "Pune",       "PAYMENT",    "DEV-HP-LAPTOP-005", days(21),         "SUCCESS", 18.0, "SAFE");
        seedTx("TXN-2026-000011", "9",  500.00,   "Paytm Recharge",      "Pune",       "PAYMENT",    "DEV-HP-LAPTOP-005", days(20),         "SUCCESS", 2.5,  "SAFE");
        seedTx("TXN-2026-000012", "10", 8000.00,  "D-Mart",              "Chennai",    "PAYMENT",    "DEV-ONEPLUS-006",   days(19),         "SUCCESS", 9.0,  "SAFE");
        seedTx("TXN-2026-000013", "10", 2000.00,  "Airtel Postpaid",     "Chennai",    "PAYMENT",    "DEV-ONEPLUS-006",   days(18),         "SUCCESS", 4.0,  "SAFE");
        seedTx("TXN-2026-000014", "11", 35000.00, "LG Electronics",      "Kolkata",    "PAYMENT",    "DEV-DELL-007",      days(17),         "SUCCESS", 20.0, "SAFE");
        seedTx("TXN-2026-000015", "11", 1800.00,  "Dominos Pizza",       "Kolkata",    "PAYMENT",    "DEV-DELL-007",      days(16),         "SUCCESS", 5.5,  "SAFE");
        seedTx("TXN-2026-000016", "12", 6000.00,  "MakeMyTrip",          "Jaipur",     "PAYMENT",    "DEV-IPHONE-008",    days(15),         "SUCCESS", 11.0, "SAFE");
        seedTx("TXN-2026-000017", "12", 900.00,   "Spotify Premium",     "Jaipur",     "PAYMENT",    "DEV-IPHONE-008",    days(14),         "SUCCESS", 3.0,  "SAFE");
        seedTx("TXN-2026-000018", "13", 45000.00, "Apple India Store",   "Ahmedabad",  "PAYMENT",    "DEV-MACBOOK-009",   days(13),         "SUCCESS", 22.0, "SAFE");
        seedTx("TXN-2026-000019", "13", 3500.00,  "BookMyShow",          "Ahmedabad",  "PAYMENT",    "DEV-MACBOOK-009",   days(12),         "SUCCESS", 8.0,  "SAFE");
        seedTx("TXN-2026-000020", "14", 12000.00, "Nykaa Fashion",       "Surat",      "PAYMENT",    "DEV-REDMI-010",     days(11),         "SUCCESS", 14.0, "SAFE");
        seedTx("TXN-2026-000021", "14", 2500.00,  "Myntra",              "Surat",      "PAYMENT",    "DEV-REDMI-010",     days(10),         "SUCCESS", 6.5,  "SAFE");
        seedTx("TXN-2026-000022", "15", 7500.00,  "Decathlon",           "Lucknow",    "PAYMENT",    "DEV-PIXEL-011",     days(9),          "SUCCESS", 13.0, "SAFE");
        seedTx("TXN-2026-000023", "15", 400.00,   "Google Play Store",   "Lucknow",    "PAYMENT",    "DEV-PIXEL-011",     days(8),          "SUCCESS", 1.5,  "SAFE");
        seedTx("TXN-2026-000024", "16", 20000.00, "IDFC Bank Transfer",  "Noida",      "TRANSFER",   "DEV-LENOVO-012",    days(7),          "SUCCESS", 16.0, "SAFE");
        seedTx("TXN-2026-000025", "16", 1100.00,  "Uber India",          "Noida",      "PAYMENT",    "DEV-LENOVO-012",    days(6),          "SUCCESS", 5.0,  "SAFE");
        seedTx("TXN-2026-000026", "17", 18000.00, "Croma Electronics",   "Chandigarh", "PAYMENT",    "DEV-ASUS-013",      days(5),          "SUCCESS", 17.0, "SAFE");
        seedTx("TXN-2026-000027", "17", 3000.00,  "Tata Cliq",           "Chandigarh", "PAYMENT",    "DEV-ASUS-013",      days(4),          "SUCCESS", 7.5,  "SAFE");
        seedTx("TXN-2026-000028", "18", 5500.00,  "Naukri Premium",      "Bhopal",     "PAYMENT",    "DEV-HP-14",         days(3),          "SUCCESS", 10.0, "SAFE");
        seedTx("TXN-2026-000029", "18", 700.00,   "Netflix India",       "Bhopal",     "PAYMENT",    "DEV-HP-14",         days(2),          "SUCCESS", 2.0,  "SAFE");
        seedTx("TXN-2026-000030", "19", 9000.00,  "Bajaj Finance EMI",   "Indore",     "PAYMENT",    "DEV-VIVO-015",      days(1),          "SUCCESS", 12.5, "SAFE");

        // Suspicious
        seedTx("TXN-2026-000031", "5",  85000.00, "International Forex Exchange", "Delhi",     "TRANSFER",  "DEV-HP-LAPTOP-001", days(25), "SUCCESS", 45.0, "SUSPICIOUS");
        seedTx("TXN-2026-000032", "6",  75000.00, "Crypto Exchange India",        "Bangalore", "PAYMENT",   "DEV-IPHONE-002",    days(22), "SUCCESS", 52.0, "SUSPICIOUS");
        seedTx("TXN-2026-000033", "7",  92000.00, "Jewellery World",              "Mumbai",    "PAYMENT",   "DEV-NEW-DEVICE-X",  days(18), "SUCCESS", 58.0, "SUSPICIOUS");
        seedTx("TXN-2026-000034", "8",  60000.00, "Online Casino Royale",         "Goa",       "PAYMENT",   "DEV-ATM-CARD-004",  days(15), "SUCCESS", 48.0, "SUSPICIOUS");
        seedTx("TXN-2026-000035", "9",  55000.00, "Peer to Peer Transfer",        "Hyderabad", "TRANSFER",  "DEV-HP-LAPTOP-005", days(12), "SUCCESS", 42.0, "SUSPICIOUS");
        seedTx("TXN-2026-000036", "10", 78000.00, "Dark Web Marketplace",         "Unknown",   "PAYMENT",   "DEV-TOR-BROWSER",   days(10), "BLOCKED", 59.0, "SUSPICIOUS");
        seedTx("TXN-2026-000037", "11", 50000.00, "Luxury Watch Store",           "Singapore", "PAYMENT",   "DEV-DELL-007",      days(8),  "SUCCESS", 44.0, "SUSPICIOUS");
        seedTx("TXN-2026-000038", "12", 65000.00, "NFT Platform",                 "Dubai",     "PAYMENT",   "DEV-UNKNOWN-NEW",   days(6),  "SUCCESS", 53.0, "SUSPICIOUS");
        seedTx("TXN-2026-000039", "13", 48000.00, "Offshore Investment",          "Cayman",    "TRANSFER",  "DEV-MACBOOK-009",   days(4),  "PENDING", 41.0, "SUSPICIOUS");
        seedTx("TXN-2026-000040", "14", 99000.00, "Bulk Electronics Dealer",      "Delhi",     "PAYMENT",   "DEV-NEW-MOBILE-Y",  days(2),  "SUCCESS", 57.0, "SUSPICIOUS");

        // Fraud
        seedTx("TXN-2026-000041", "5",  250000.00, "Shell Corp Transfer",    "Dubai",       "TRANSFER",   "DEV-UNKNOWN-A",     days(20), "BLOCKED", 87.0, "FRAUD");
        seedTx("TXN-2026-000042", "6",  180000.00, "Bitcoin ATM",            "Singapore",   "WITHDRAWAL", "DEV-NEW-DEVICE-B",  days(18), "BLOCKED", 91.0, "FRAUD");
        seedTx("TXN-2026-000043", "7",  320000.00, "International Wire",     "Switzerland", "TRANSFER",   "DEV-SAMSUNG-003",   days(16), "BLOCKED", 95.0, "FRAUD");
        seedTx("TXN-2026-000044", "8",  150000.00, "Ponzi Investment",       "Lagos",       "PAYMENT",    "DEV-HACKED-C",      days(14), "BLOCKED", 88.0, "FRAUD");
        seedTx("TXN-2026-000045", "9",  500000.00, "Ransomware Payment",     "Unknown",     "PAYMENT",    "DEV-COMPROMISED-D", days(12), "BLOCKED", 98.0, "FRAUD");
        seedTx("TXN-2026-000046", "10", 200000.00, "Fake E-Commerce",        "Russia",      "PAYMENT",    "DEV-TOR-BROWSER",   days(10), "BLOCKED", 93.0, "FRAUD");
        seedTx("TXN-2026-000047", "11", 175000.00, "Money Mule Transfer",    "UK",          "TRANSFER",   "DEV-NEW-DEVICE-E",  days(8),  "BLOCKED", 86.0, "FRAUD");
        seedTx("TXN-2026-000048", "12", 400000.00, "Fake Investment Scheme", "Cayman",      "TRANSFER",   "DEV-UNKNOWN-F",     days(6),  "BLOCKED", 96.0, "FRAUD");
        seedTx("TXN-2026-000049", "13", 120000.00, "Phishing Site Purchase", "Unknown",     "PAYMENT",    "DEV-HACKED-G",      days(4),  "BLOCKED", 82.0, "FRAUD");
        seedTx("TXN-2026-000050", "14", 280000.00, "Account Takeover",       "China",       "WITHDRAWAL", "DEV-HACKED-H",      days(2),  "BLOCKED", 97.0, "FRAUD");

        // More normal filler
        seedTx("TXN-2026-000051", "5",  1000.00,  "Paytm Mall",       "Hyderabad",  "PAYMENT",  "DEV-HP-LAPTOP-001", daysHours(29, 2),  "SUCCESS", 3.0,  "SAFE");
        seedTx("TXN-2026-000052", "6",  4000.00,  "IRCTC",            "Mumbai",     "PAYMENT",  "DEV-IPHONE-002",    daysHours(28, 3),  "SUCCESS", 7.0,  "SAFE");
        seedTx("TXN-2026-000053", "7",  6500.00,  "BSNL Bill",        "Delhi",      "PAYMENT",  "DEV-SAMSUNG-003",   daysHours(27, 4),  "SUCCESS", 9.0,  "SAFE");
        seedTx("TXN-2026-000054", "8",  2800.00,  "HDFC Credit Card", "Bangalore",  "PAYMENT",  "DEV-ATM-CARD-004",  daysHours(26, 5),  "SUCCESS", 6.0,  "SAFE");
        seedTx("TXN-2026-000055", "9",  19000.00, "Maruti Suzuki",    "Pune",       "PAYMENT",  "DEV-HP-LAPTOP-005", daysHours(25, 6),  "SUCCESS", 16.0, "SAFE");
        seedTx("TXN-2026-000056", "10", 3200.00,  "Jio Postpaid",     "Chennai",    "PAYMENT",  "DEV-ONEPLUS-006",   daysHours(24, 7),  "SUCCESS", 5.0,  "SAFE");
        seedTx("TXN-2026-000057", "11", 750.00,   "Hotstar Premium",  "Kolkata",    "PAYMENT",  "DEV-DELL-007",      daysHours(23, 8),  "SUCCESS", 2.0,  "SAFE");
        seedTx("TXN-2026-000058", "12", 8500.00,  "Tanishq Jewels",   "Jaipur",     "PAYMENT",  "DEV-IPHONE-008",    daysHours(22, 9),  "SUCCESS", 14.0, "SAFE");
        seedTx("TXN-2026-000059", "13", 1600.00,  "Haldirams",        "Ahmedabad",  "PAYMENT",  "DEV-MACBOOK-009",   daysHours(21, 10), "SUCCESS", 4.0,  "SAFE");
        seedTx("TXN-2026-000060", "14", 22000.00, "Hyundai Service",  "Surat",      "PAYMENT",  "DEV-REDMI-010",     daysHours(20, 11), "SUCCESS", 18.0, "SAFE");
        seedTx("TXN-2026-000061", "15", 3800.00,  "CEAT Tyres",       "Lucknow",    "PAYMENT",  "DEV-PIXEL-011",     daysHours(19, 12), "SUCCESS", 8.0,  "SAFE");
        seedTx("TXN-2026-000062", "16", 5200.00,  "LIC Premium",      "Noida",      "PAYMENT",  "DEV-LENOVO-012",    daysHours(18, 13), "SUCCESS", 10.0, "SAFE");
        seedTx("TXN-2026-000063", "17", 1300.00,  "SBI NEFT Transfer","Chandigarh", "TRANSFER", "DEV-ASUS-013",      daysHours(17, 14), "SUCCESS", 5.5,  "SAFE");
        seedTx("TXN-2026-000064", "18", 6800.00,  "Boat Headphones",  "Bhopal",     "PAYMENT",  "DEV-HP-14",         daysHours(16, 15), "SUCCESS", 11.0, "SAFE");
        seedTx("TXN-2026-000065", "19", 4100.00,  "Nykaa Beauty",     "Indore",     "PAYMENT",  "DEV-VIVO-015",      daysHours(15, 16), "SUCCESS", 7.5,  "SAFE");
        seedTx("TXN-2026-000066", "20", 28000.00, "Samsung Galaxy",   "Kochi",      "PAYMENT",  "DEV-SAMSUNG-016",   daysHours(14, 17), "SUCCESS", 21.0, "SAFE");
        seedTx("TXN-2026-000067", "5",  2100.00,  "Starbucks",        "Hyderabad",  "PAYMENT",  "DEV-HP-LAPTOP-001", daysHours(13, 18), "SUCCESS", 4.5,  "SAFE");
        seedTx("TXN-2026-000068", "6",  900.00,   "McDonald's",       "Mumbai",     "PAYMENT",  "DEV-IPHONE-002",    daysHours(12, 19), "SUCCESS", 2.5,  "SAFE");
        seedTx("TXN-2026-000069", "7",  11000.00, "Axis Bank Transfer","Delhi",     "TRANSFER", "DEV-SAMSUNG-003",   daysHours(11, 20), "SUCCESS", 13.5, "SAFE");
        seedTx("TXN-2026-000070", "8",  1700.00,  "Apollo Pharmacy",  "Bangalore",  "PAYMENT",  "DEV-ATM-CARD-004",  daysHours(10, 21), "SUCCESS", 5.0,  "SAFE");

        // Rapid transaction scenario
        seedTx("TXN-2026-000071", "5", 5000.00, "Store A", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 0),   "SUCCESS", 35.0, "SUSPICIOUS");
        seedTx("TXN-2026-000072", "5", 4500.00, "Store B", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 1),   "SUCCESS", 38.0, "SUSPICIOUS");
        seedTx("TXN-2026-000073", "5", 3200.00, "Store C", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 2),   "SUCCESS", 40.0, "SUSPICIOUS");
        seedTx("TXN-2026-000074", "5", 6000.00, "Store D", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 3),   "SUCCESS", 42.0, "SUSPICIOUS");
        seedTx("TXN-2026-000075", "5", 2800.00, "Store E", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 4),   "SUCCESS", 44.0, "SUSPICIOUS");
        seedTx("TXN-2026-000076", "5", 5500.00, "Store F", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 5),   "SUCCESS", 46.0, "SUSPICIOUS");
        seedTx("TXN-2026-000077", "5", 4800.00, "Store G", "Hyderabad", "PAYMENT", "DEV-HP-LAPTOP-001", daysMin(5, 10, 6),   "BLOCKED", 72.0, "FRAUD");

        // Location mismatch scenario
        seedTx("TXN-2026-000078", "6", 8000.00,  "Local Market",    "Hyderabad", "PAYMENT", "DEV-IPHONE-002", daysMin(3, 14, 0),  "SUCCESS", 10.0, "SAFE");
        seedTx("TXN-2026-000079", "6", 45000.00, "Electronics Hub", "Delhi",     "PAYMENT", "DEV-IPHONE-002", daysMin(3, 13, 50), "BLOCKED", 76.0, "FRAUD");

        // Device change scenario
        seedTx("TXN-2026-000080", "7", 12000.00, "Reliance Trends", "Delhi", "PAYMENT", "DEV-SAMSUNG-003",     daysHours(2, 16), "SUCCESS", 11.0, "SAFE");
        seedTx("TXN-2026-000081", "7", 95000.00, "Luxury Store",    "Delhi", "PAYMENT", "DEV-NEW-UNKNOWN-XYZ", daysHours(2, 15), "BLOCKED", 68.0, "FRAUD");

        // Multi-risk scenario
        seedTx("TXN-2026-000082", "8", 350000.00, "Offshore Wire", "Unknown", "TRANSFER", "DEV-COMPROMISED-NEW", daysHours(1, 12), "BLOCKED", 97.5, "FRAUD");

        // More normal filler
        seedTx("TXN-2026-000083", "9",  1400.00,  "Pizza Hut",        "Pune",       "PAYMENT", "DEV-HP-LAPTOP-005", daysHours(29, 1),  "SUCCESS", 3.5,  "SAFE");
        seedTx("TXN-2026-000084", "10", 3600.00,  "HP Printer",       "Chennai",    "PAYMENT", "DEV-ONEPLUS-006",   daysHours(28, 2),  "SUCCESS", 7.0,  "SAFE");
        seedTx("TXN-2026-000085", "11", 2300.00,  "Zerodha",          "Kolkata",    "PAYMENT", "DEV-DELL-007",      daysHours(27, 3),  "SUCCESS", 5.5,  "SAFE");
        seedTx("TXN-2026-000086", "12", 14000.00, "Skybags Luggage",  "Jaipur",     "PAYMENT", "DEV-IPHONE-008",    daysHours(26, 4),  "SUCCESS", 15.0, "SAFE");
        seedTx("TXN-2026-000087", "13", 800.00,   "Chai Point",       "Ahmedabad",  "PAYMENT", "DEV-MACBOOK-009",   daysHours(25, 5),  "SUCCESS", 2.0,  "SAFE");
        seedTx("TXN-2026-000088", "14", 7200.00,  "Woodland Shoes",   "Surat",      "PAYMENT", "DEV-REDMI-010",     daysHours(24, 6),  "SUCCESS", 12.0, "SAFE");
        seedTx("TXN-2026-000089", "15", 4300.00,  "Urban Ladder",     "Lucknow",    "PAYMENT", "DEV-PIXEL-011",     daysHours(23, 7),  "SUCCESS", 8.5,  "SAFE");
        seedTx("TXN-2026-000090", "16", 16000.00, "Boat Smartwatch",  "Noida",      "PAYMENT", "DEV-LENOVO-012",    daysHours(22, 8),  "SUCCESS", 17.0, "SAFE");
        seedTx("TXN-2026-000091", "17", 600.00,   "Kindle Purchase",  "Chandigarh", "PAYMENT", "DEV-ASUS-013",      daysHours(21, 9),  "SUCCESS", 2.5,  "SAFE");
        seedTx("TXN-2026-000092", "18", 9500.00,  "Prestige Cookware","Bhopal",     "PAYMENT", "DEV-HP-14",         daysHours(20, 10), "SUCCESS", 13.0, "SAFE");
        seedTx("TXN-2026-000093", "19", 2700.00,  "Mama Earth",       "Indore",     "PAYMENT", "DEV-VIVO-015",      daysHours(19, 11), "SUCCESS", 6.0,  "SAFE");
        seedTx("TXN-2026-000094", "20", 38000.00, "Noise Smartwatch", "Kochi",      "PAYMENT", "DEV-SAMSUNG-016",   daysHours(18, 12), "SUCCESS", 22.0, "SAFE");
        seedTx("TXN-2026-000095", "5",  1950.00,  "Pizza Express",    "Hyderabad",  "PAYMENT", "DEV-HP-LAPTOP-001", daysHours(17, 13), "SUCCESS", 4.0,  "SAFE");
        seedTx("TXN-2026-000096", "6",  6200.00,  "Ashok Leyland",    "Mumbai",     "PAYMENT", "DEV-IPHONE-002",    daysHours(16, 14), "SUCCESS", 11.5, "SAFE");
        seedTx("TXN-2026-000097", "7",  3100.00,  "Indian Oil Corp",  "Delhi",      "PAYMENT", "DEV-SAMSUNG-003",   daysHours(15, 15), "SUCCESS", 7.0,  "SAFE");
        seedTx("TXN-2026-000098", "8",  5800.00,  "Taj Hotels",       "Bangalore",  "PAYMENT", "DEV-ATM-CARD-004",  daysHours(14, 16), "SUCCESS", 10.5, "SAFE");
        seedTx("TXN-2026-000099", "9",  1250.00,  "CCD Coffee",       "Pune",       "PAYMENT", "DEV-HP-LAPTOP-005", daysHours(13, 17), "SUCCESS", 3.0,  "SAFE");
        seedTx("TXN-2026-000100", "10", 42000.00, "Godrej Appliances","Chennai",    "PAYMENT", "DEV-ONEPLUS-006",   daysHours(12, 18), "SUCCESS", 23.0, "SAFE");
    }

    // ─── Fraud Alerts ──────────────────────────────────────────

    private void seedAlerts() {
        Object[][] alerts = {
            {"TXN-2026-000041", "HIGH_AMOUNT",       "CRITICAL", "Transaction amount Rs.250,000 exceeds threshold. Shell company transfer to offshore account.",  87.0,  "INVESTIGATING"},
            {"TXN-2026-000042", "DEVICE_CHANGE",     "CRITICAL", "Transaction from new unrecognized device. Bitcoin ATM withdrawal in Singapore.",                91.0,  "OPEN"},
            {"TXN-2026-000043", "HIGH_AMOUNT",       "CRITICAL", "International wire transfer Rs.320,000 to Switzerland. Multiple risk factors triggered.",       95.0,  "INVESTIGATING"},
            {"TXN-2026-000044", "LOCATION_MISMATCH", "HIGH",     "Transaction in Lagos – unusual geography for account. Ponzi investment pattern detected.",      88.0,  "OPEN"},
            {"TXN-2026-000045", "HIGH_AMOUNT",       "CRITICAL", "Transaction Rs.500,000 to unknown recipient. Ransomware payment pattern.",                      98.0,  "RESOLVED"},
            {"TXN-2026-000046", "VELOCITY",          "CRITICAL", "High velocity transactions from TOR browser IP. Dark web marketplace payment.",                  93.0,  "RESOLVED"},
            {"TXN-2026-000047", "DEVICE_CHANGE",     "HIGH",     "Money mule transfer pattern detected. New device used for large international transfer.",        86.0,  "INVESTIGATING"},
            {"TXN-2026-000048", "HIGH_AMOUNT",       "CRITICAL", "Investment scheme payment Rs.400,000 to Cayman Islands account.",                               96.0,  "OPEN"},
            {"TXN-2026-000049", "DEVICE_CHANGE",     "HIGH",     "Account accessed from compromised device. Phishing site purchase detected.",                    82.0,  "FALSE_POSITIVE"},
            {"TXN-2026-000050", "LOCATION_MISMATCH", "CRITICAL", "Account takeover suspected. Transaction from China – account typically used in India.",          97.0,  "INVESTIGATING"},
            {"TXN-2026-000077", "RAPID_TRANSACTION", "HIGH",     "User made 7 transactions within 6 minutes. Velocity rule triggered after 5 transactions.",      72.0,  "OPEN"},
            {"TXN-2026-000079", "LOCATION_MISMATCH", "CRITICAL", "Impossible travel detected: Hyderabad at 14:00, Delhi at 14:10 (1,500 km in 10 minutes).",     76.0,  "OPEN"},
            {"TXN-2026-000081", "DEVICE_CHANGE",     "HIGH",     "Transaction from unrecognized device DEV-NEW-UNKNOWN-XYZ. Known device: DEV-SAMSUNG-003.",      68.0,  "OPEN"},
            {"TXN-2026-000082", "HIGH_AMOUNT",       "CRITICAL", "Multiple risk factors: Rs.350,000 amount + new device + unknown location. Fraud score 97.5.",   97.5,  "INVESTIGATING"},
        };

        for (Object[] a : alerts) {
            FraudAlert alert = FraudAlert.builder()
                .transactionId((String) a[0])
                .alertType((String) a[1])
                .severity(FraudAlert.Severity.valueOf((String) a[2]))
                .reason((String) a[3])
                .fraudScore(BigDecimal.valueOf((Double) a[4]))
                .status(FraudAlert.AlertStatus.valueOf((String) a[5]))
                .createdAt(Instant.now().minus(Duration.ofDays(20)))
                .build();
            fraudAlertRepository.save(alert);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────

    private void seedTx(String id, String seedUserId, double amount, String merchant,
                        String location, String type, String deviceId, Instant ts,
                        String status, double score, String fraudStatus) {
        User user = seedUsersMap.get(seedUserId);
        Transaction tx = Transaction.builder()
            .transactionId(id)
            .userId(user.getId().toString())
            .userName(user.getName())
            .amount(BigDecimal.valueOf(amount))
            .currency("INR")
            .merchant(merchant)
            .location(location)
            .transactionType(Transaction.TransactionType.valueOf(type))
            .deviceId(deviceId)
            .timestamp(ts)
            .status(Transaction.TransactionStatus.valueOf(status))
            .fraudScore(BigDecimal.valueOf(score))
            .fraudStatus(Transaction.FraudStatus.valueOf(fraudStatus))
            .createdAt(ts)
            .build();
        transactionRepository.save(tx);
    }

    private static Instant days(int days) {
        return Instant.now().minus(Duration.ofDays(days));
    }

    private static Instant daysHours(int days, int hours) {
        return Instant.now().minus(Duration.ofDays(days)).minus(Duration.ofHours(hours));
    }

    private static Instant daysMin(int days, int hours, int minutes) {
        return Instant.now().minus(Duration.ofDays(days))
            .minus(Duration.ofHours(hours))
            .minus(Duration.ofMinutes(minutes));
    }
}
