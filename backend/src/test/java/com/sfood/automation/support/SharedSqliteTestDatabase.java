package com.sfood.automation.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class SharedSqliteTestDatabase {

    public static final Path DATABASE_PATH = Path.of("data/sfood-mall-automation_test.db");

    private SharedSqliteTestDatabase() {
    }

    public static void register(DynamicPropertyRegistry registry) throws Exception {
        Files.createDirectories(DATABASE_PATH.getParent());
        ensureSchemaCompatibility();
        registry.add("DB_PATH", () -> DATABASE_PATH.toString());
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "no-reply@s-food.com");
        registry.add("GODO_BASE_URL", () -> "https://example.test/godo");
        registry.add("GODO_PARTNER_KEY", () -> "partner-key");
        registry.add("GODO_KEY", () -> "godo-key");
        registry.add("WMS_LOGIN_URL", () -> "https://example.test/wms/login");
        registry.add("WMS_STOCK_URL", () -> "https://example.test/wms/stock");
        registry.add("WMS_USER_ID", () -> "tester");
        registry.add("WMS_PASSWORD", () -> "password");
        registry.add("WMS_CENTER_CODE", () -> "OA110");
        registry.add("WMS_WAREHOUSE_CODE", () -> "SLP001");
        registry.add("SLACK_WEBHOOK_URL", () -> "https://hooks.slack.test/services/test");
        registry.add("OTP_SERVER_SECRET", () -> "test-secret");
    }

    private static void ensureSchemaCompatibility() throws Exception {
        if (Files.notExists(DATABASE_PATH)) {
            return;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
             Statement statement = connection.createStatement()) {
            if (hasLegacyUniqueErpGoodsCodeIndex(statement)) {
                connection.close();
                Files.deleteIfExists(DATABASE_PATH);
                return;
            }
            if (!tableExists(statement, "goods")) {
                return;
            }
            ensureColumn(statement, "goods", "godo_sync_blocked_yn", "ALTER TABLE goods ADD COLUMN godo_sync_blocked_yn TEXT NOT NULL DEFAULT 'N'");
            ensureColumn(statement, "goods", "remain_valid_days", "ALTER TABLE goods ADD COLUMN remain_valid_days INTEGER NOT NULL DEFAULT 0");
            ensureColumn(
                statement,
                "common_status_config",
                "remain_valid_days",
                "ALTER TABLE common_status_config ADD COLUMN remain_valid_days INTEGER NOT NULL DEFAULT 150"
            );
            synchronizeGoodsRemainValidDaysFromConfig(statement);
            ensureColumn(statement, "goods_status_log", "remain_valid_days", "ALTER TABLE goods_status_log ADD COLUMN remain_valid_days INTEGER");
            ensureGoodsStatusLogNullableValidPercent(statement);
            ensureColumn(statement, "member_partner", "member_status",
                "ALTER TABLE member_partner ADD COLUMN member_status TEXT NOT NULL DEFAULT 'NORMAL'");
            ensureMemberPartnerRegisteredFlRemoved(statement);
            ensureColumn(statement, "godo_member_business", "duplicate_mem_no",
                "ALTER TABLE godo_member_business ADD COLUMN duplicate_mem_no TEXT");
            ensureTable(statement, "godo_member_business_log",
                "CREATE TABLE godo_member_business_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mem_no BIGINT NOT NULL," +
                "company TEXT, business_no TEXT, ceo_name TEXT, service TEXT, item TEXT," +
                "com_address TEXT, biz_license_file TEXT," +
                "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
            ensureTable(statement, "member_partner_log",
                "CREATE TABLE member_partner_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mem_no BIGINT NOT NULL," +
                "partner_code TEXT, partner_name TEXT," +
                "tax_invoice_fl TEXT, member_status TEXT," +
                "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
            ensureMemberPartnerLogRegisteredFlRemoved(statement);
            ensureColumn(statement, "godo_member_business", "service_code",
                "ALTER TABLE godo_member_business ADD COLUMN service_code TEXT");
            ensureColumn(statement, "godo_member_business", "item_code",
                "ALTER TABLE godo_member_business ADD COLUMN item_code TEXT");
            ensureColumn(statement, "godo_member_business_log", "service_code",
                "ALTER TABLE godo_member_business_log ADD COLUMN service_code TEXT");
            ensureColumn(statement, "godo_member_business_log", "item_code",
                "ALTER TABLE godo_member_business_log ADD COLUMN item_code TEXT");
            ensureTable(statement, "common_code",
                "CREATE TABLE common_code (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "group_code TEXT NOT NULL, group_name TEXT NOT NULL," +
                "code TEXT NOT NULL, code_name TEXT NOT NULL," +
                "note TEXT, use_yn TEXT NOT NULL DEFAULT 'Y'," +
                "created_at TEXT NOT NULL, updated_at TEXT NOT NULL," +
                "UNIQUE (group_code, code))");
        }
    }

    private static void ensureColumn(Statement statement, String tableName, String columnName, String alterSql) throws Exception {
        if (!tableExists(statement, tableName) || columnExists(statement, tableName, columnName)) {
            return;
        }
        statement.execute(alterSql);
    }

    private static void ensureMemberPartnerRegisteredFlRemoved(Statement statement) throws Exception {
        if (!tableExists(statement, "member_partner") || !columnExists(statement, "member_partner", "partner_registered_fl")) {
            return;
        }
        statement.execute(
            "UPDATE member_partner SET member_status = " +
            "CASE WHEN partner_registered_fl = 'y' THEN 'NORMAL' ELSE 'UNREGISTERED' END");
        statement.execute("DROP INDEX IF EXISTS idx_member_partner_registered_fl");
        statement.execute("ALTER TABLE member_partner DROP COLUMN partner_registered_fl");
        statement.execute(
            "CREATE INDEX IF NOT EXISTS idx_member_partner_member_status ON member_partner(member_status)");
    }

    private static void ensureMemberPartnerLogRegisteredFlRemoved(Statement statement) throws Exception {
        if (!tableExists(statement, "member_partner_log") || !columnExists(statement, "member_partner_log", "partner_registered_fl")) {
            return;
        }
        statement.execute("ALTER TABLE member_partner_log DROP COLUMN partner_registered_fl");
    }

    private static void ensureGoodsStatusLogNullableValidPercent(Statement statement) throws Exception {
        if (!tableExists(statement, "goods_status_log") || !columnIsNotNull(statement, "goods_status_log", "valid_percent")) {
            return;
        }
        statement.execute("ALTER TABLE goods_status_log RENAME TO goods_status_log_old");
        statement.execute(
            "CREATE TABLE goods_status_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "godo_goods_code TEXT NOT NULL, "
                + "erp_goods_code TEXT NOT NULL, "
                + "wms_available_qty INTEGER NOT NULL, "
                + "criteria_qty INTEGER NOT NULL, "
                + "safety_rate INTEGER NOT NULL, "
                + "valid_percent INTEGER, "
                + "remain_valid_days INTEGER, "
                + "before_status TEXT NOT NULL, "
                + "after_status TEXT NOT NULL, "
                + "changed_at TEXT NOT NULL, "
                + "created_at TEXT NOT NULL)"
        );
        statement.execute(
            "INSERT INTO goods_status_log ("
                + "id, godo_goods_code, erp_goods_code, wms_available_qty, criteria_qty, safety_rate, "
                + "valid_percent, remain_valid_days, before_status, after_status, changed_at, created_at"
                + ") "
                + "SELECT id, godo_goods_code, erp_goods_code, wms_available_qty, criteria_qty, safety_rate, "
                + "valid_percent, remain_valid_days, before_status, after_status, changed_at, created_at "
                + "FROM goods_status_log_old"
        );
        statement.execute("DROP TABLE goods_status_log_old");
        statement.execute("CREATE INDEX idx_goods_status_log_godo_goods_code ON goods_status_log(godo_goods_code)");
        statement.execute("CREATE INDEX idx_goods_status_log_changed_at ON goods_status_log(changed_at)");
        statement.execute("CREATE INDEX idx_goods_status_log_after_status ON goods_status_log(after_status)");
    }

    private static void synchronizeGoodsRemainValidDaysFromConfig(Statement statement) throws Exception {
        if (!tableExists(statement, "goods") || !tableExists(statement, "common_status_config")) {
            return;
        }
        statement.execute(
            """
            UPDATE goods
               SET remain_valid_days = (
                   CASE
                       WHEN only_mall_yn = 'Y'
                           THEN COALESCE((SELECT remain_valid_days FROM common_status_config WHERE scope = 'EXCLUSIVE'), 150)
                       ELSE COALESCE((SELECT remain_valid_days FROM common_status_config WHERE scope = 'GENERAL'), 150)
                   END
               )
             WHERE COALESCE(remain_valid_days, 0) = 0
            """
        );
    }

    private static boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tableName + "'"
        )) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private static void ensureTable(Statement statement, String tableName, String createSql) throws Exception {
        if (tableExists(statement, tableName)) return;
        statement.execute(createSql);
    }

    private static boolean columnExists(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean columnIsNotNull(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return resultSet.getInt("notnull") == 1;
                }
            }
            return false;
        }
    }

    private static boolean hasLegacyUniqueErpGoodsCodeIndex(Statement statement) throws Exception {
        if (!tableExists(statement, "goods")) {
            return false;
        }
        try (ResultSet indexes = statement.executeQuery("PRAGMA index_list(goods)")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") != 1) {
                    continue;
                }
                String indexName = indexes.getString("name");
                try (ResultSet indexInfo = statement.executeQuery("PRAGMA index_info(" + indexName + ")")) {
                    while (indexInfo.next()) {
                        if ("erp_goods_code".equals(indexInfo.getString("name"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
