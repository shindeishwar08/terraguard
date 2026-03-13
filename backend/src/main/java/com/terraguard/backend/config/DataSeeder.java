package com.terraguard.backend.config;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // Guard Clause: Don't seed if data exists
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cities", Integer.class);
        if (count != null && count > 0) {
            log.info("[SEEDER] Cities table already seeded ({} rows). Skipping.", count);
            return;
        }

        log.info("[SEEDER] Seeding cities table from worldcities.csv...");
        
        List<Object[]> batch = new ArrayList<>();
        String sql = "INSERT INTO cities (name, country, population, geometry) " +
                     "VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))";

        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new ClassPathResource("data/worldcities.csv").getInputStream()))) {
            
            reader.readNext(); // Skip CSV Header
            String[] line;
            
            while ((line = reader.readNext()) != null) {
                try {
                    String name = line[1]; // Using city_ascii to avoid encoding issues
                    double lat = Double.parseDouble(line[2]);
                    double lon = Double.parseDouble(line[3]);
                    String country = line[4];
                    String popStr = line[9];
                    
                    // Handle scientific notation or empty population strings
                    Integer population = (popStr == null || popStr.isBlank()) ? 
                                         null : (int) Double.parseDouble(popStr);

                    // IMPORTANT: ST_MakePoint(X, Y) -> ST_MakePoint(Longitude, Latitude)
                    batch.add(new Object[]{name, country, population, lon, lat});

                    // Periodic flush to keep memory usage low on 2GB RAM instance
                    if (batch.size() >= 1000) {
                        jdbcTemplate.batchUpdate(sql, batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    log.debug("[SEEDER] Skipping malformed row: {}", (Object) line);
                }
            }
            
            // Final flush for remaining rows
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
            }
        }

        log.info("[SEEDER] City seeding complete.");
    }
}