package com.sfood.automation.common.bootstrap;

import com.sfood.automation.common.config.DatabaseProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class SqliteBootstrapRunner implements ApplicationRunner {

    private final DatabaseProperties databaseProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path databasePath = Path.of(databaseProperties.path()).toAbsolutePath();
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(databasePath)) {
            Files.createFile(databasePath);
        }

        jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");
    }
}
