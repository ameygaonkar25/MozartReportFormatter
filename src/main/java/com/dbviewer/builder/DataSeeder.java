package com.dbviewer.builder;

import com.dbviewer.model.ExecutionRecord;
import com.dbviewer.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile("dev")   // Only runs with H2 dev profile — disabled for Oracle/production
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExecutionRecordRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding database with sample data...");

        // Correct Total Txn values: 10000, 100000, 500000, 1000000
        List<ExecutionRecord> records = Arrays.asList(
                // ── 02-Apr-26 ──────────────────────────────────────────────────
                build(1,  10000,   "02-Apr-26", 20.0),
                build(1,  100000,  "02-Apr-26", 200.0),
                build(1,  500000,  "02-Apr-26", 1000.0),
                build(1,  1000000, "02-Apr-26", 2000.0),
                build(2,  10000,   "02-Apr-26", 10.0),
                build(2,  100000,  "02-Apr-26", 100.0),
                build(2,  500000,  "02-Apr-26", 500.0),
                build(2,  1000000, "02-Apr-26", 1000.0),
                build(5,  10000,   "02-Apr-26", 4.0),
                build(5,  100000,  "02-Apr-26", 40.0),
                build(5,  500000,  "02-Apr-26", 200.0),
                build(5,  1000000, "02-Apr-26", 400.0),
                build(10, 10000,   "02-Apr-26", 2.0),
                build(10, 100000,  "02-Apr-26", 20.0),
                build(10, 500000,  "02-Apr-26", 100.0),
                build(10, 1000000, "02-Apr-26", 200.0),
                // ── 04-Apr-26 ──────────────────────────────────────────────────
                build(1,  10000,   "04-Apr-26", 20.0),
                build(1,  100000,  "04-Apr-26", 200.0),
                build(1,  500000,  "04-Apr-26", 1000.0),
                build(1,  1000000, "04-Apr-26", 2000.0),
                build(2,  10000,   "04-Apr-26", 10.0),
                build(2,  100000,  "04-Apr-26", 100.0),
                build(2,  500000,  "04-Apr-26", 500.0),
                build(2,  1000000, "04-Apr-26", 1000.0),
                build(5,  10000,   "04-Apr-26", 4.0),
                build(5,  100000,  "04-Apr-26", 40.0),
                build(5,  500000,  "04-Apr-26", 200.0),
                build(5,  1000000, "04-Apr-26", 400.0),
                build(10, 10000,   "04-Apr-26", 2.0),
                build(10, 100000,  "04-Apr-26", 20.0),
                build(10, 500000,  "04-Apr-26", 100.0),
                build(10, 1000000, "04-Apr-26", 200.0),
                // ── 12-Apr-26 ──────────────────────────────────────────────────
                build(1,  10000,   "12-Apr-26", 19.0),
                build(1,  100000,  "12-Apr-26", 199.0),
                build(1,  500000,  "12-Apr-26", 999.0),
                build(1,  1000000, "12-Apr-26", 1999.0),
                build(2,  10000,   "12-Apr-26", 9.0),
                build(2,  100000,  "12-Apr-26", 99.0),
                build(2,  500000,  "12-Apr-26", 499.0),
                build(2,  1000000, "12-Apr-26", 999.0),
                build(5,  10000,   "12-Apr-26", 3.0),
                build(5,  100000,  "12-Apr-26", 39.0),
                build(5,  500000,  "12-Apr-26", 199.0),
                build(5,  1000000, "12-Apr-26", 399.0),
                build(10, 10000,   "12-Apr-26", 1.0),
                build(10, 100000,  "12-Apr-26", 19.0),
                build(10, 500000,  "12-Apr-26", 99.0),
                build(10, 1000000, "12-Apr-26", 199.0)
        );

        repository.saveAll(records);
        log.info("Seeded {} records successfully.", records.size());
    }

    private ExecutionRecord build(int nodes, int txn, String date, double avg) {
        return ExecutionRecord.builder()
                .nodes(nodes)
                .totalTxn(txn)
                .executionDate(date)
                .avgTime(avg)
                .build();
    }
}