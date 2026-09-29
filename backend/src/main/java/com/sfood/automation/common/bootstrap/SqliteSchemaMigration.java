package com.sfood.automation.common.bootstrap;

import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("sqliteSchemaMigration")
@Profile("!prod")
@DependsOn("dataSourceScriptDatabaseInitializer")
public class SqliteSchemaMigration implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    public SqliteSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        ensureGoodsSyncBlockedColumn();
        ensureGoodsRemainValidDaysColumn();
        ensureCommonStatusConfigRemainValidDaysColumn();
        synchronizeGoodsRemainValidDaysFromConfig();
        ensureGoodsStatusLogRemainValidDaysColumn();
        ensureGoodsStatusLogNullableValidPercent();
        ensureMemberPartnerMemberStatusColumn();
        ensureMemberPartnerRegisteredFlRemoved();
        ensureGodoMemberBusinessDuplicateMemNoColumn();
        ensureGodoMemberBusinessCodeColumns();      // 추가
        ensureGodoMemberBusinessLogTable();
        ensureMemberPartnerLogTable();
        ensureMemberPartnerLogRegisteredFlRemoved();
        ensureCommonCodeTable();
        ensureGodoMemberBusinessServiceItemColumnsRenamed();
    }

    private void ensureGoodsSyncBlockedColumn() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(goods)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (columns.contains("godo_sync_blocked_yn")) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE goods ADD COLUMN godo_sync_blocked_yn TEXT NOT NULL DEFAULT 'N'");
    }

    private void ensureGoodsRemainValidDaysColumn() {
        ensureColumn("goods", "remain_valid_days", "ALTER TABLE goods ADD COLUMN remain_valid_days INTEGER NOT NULL DEFAULT 0");
    }

    private void ensureCommonStatusConfigRemainValidDaysColumn() {
        ensureColumn(
            "common_status_config",
            "remain_valid_days",
            "ALTER TABLE common_status_config ADD COLUMN remain_valid_days INTEGER NOT NULL DEFAULT 150"
        );
    }

    private void synchronizeGoodsRemainValidDaysFromConfig() {
        jdbcTemplate.update(
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

    private void ensureGoodsStatusLogRemainValidDaysColumn() {
        ensureColumn(
            "goods_status_log",
            "remain_valid_days",
            "ALTER TABLE goods_status_log ADD COLUMN remain_valid_days INTEGER"
        );
    }

    private void ensureGoodsStatusLogNullableValidPercent() {
        List<ColumnInfo> columns = jdbcTemplate.query(
            "PRAGMA table_info(goods_status_log)",
            (resultSet, rowNum) -> new ColumnInfo(
                resultSet.getString("name"),
                resultSet.getInt("notnull") == 1
            )
        );
        boolean validPercentNotNull = columns.stream()
            .anyMatch(column -> "valid_percent".equals(column.name()) && column.notNull());
        if (!validPercentNotNull) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE goods_status_log RENAME TO goods_status_log_old");
        jdbcTemplate.execute(
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
        jdbcTemplate.execute(
            "INSERT INTO goods_status_log ("
                + "id, godo_goods_code, erp_goods_code, wms_available_qty, criteria_qty, safety_rate, "
                + "valid_percent, remain_valid_days, before_status, after_status, changed_at, created_at"
                + ") "
                + "SELECT id, godo_goods_code, erp_goods_code, wms_available_qty, criteria_qty, safety_rate, "
                + "valid_percent, remain_valid_days, before_status, after_status, changed_at, created_at "
                + "FROM goods_status_log_old"
        );
        jdbcTemplate.execute("DROP TABLE goods_status_log_old");
        jdbcTemplate.execute("CREATE INDEX idx_goods_status_log_godo_goods_code ON goods_status_log(godo_goods_code)");
        jdbcTemplate.execute("CREATE INDEX idx_goods_status_log_changed_at ON goods_status_log(changed_at)");
        jdbcTemplate.execute("CREATE INDEX idx_goods_status_log_after_status ON goods_status_log(after_status)");
    }

    private void ensureMemberPartnerMemberStatusColumn() {
        ensureColumn("member_partner", "member_status",
            "ALTER TABLE member_partner ADD COLUMN member_status TEXT NOT NULL DEFAULT 'NORMAL'");
    }

    /** Req 12: 거래처 등록 여부·상태를 member_status 하나로 통합 — partner_registered_fl 삭제 전 값 이관 */
    private void ensureMemberPartnerRegisteredFlRemoved() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(member_partner)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (!columns.contains("partner_registered_fl")) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE member_partner SET member_status = " +
            "CASE WHEN partner_registered_fl = 'y' THEN 'NORMAL' ELSE 'UNREGISTERED' END");
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_member_partner_registered_fl");
        jdbcTemplate.execute("ALTER TABLE member_partner DROP COLUMN partner_registered_fl");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_member_partner_member_status ON member_partner(member_status)");
    }

    private void ensureMemberPartnerLogRegisteredFlRemoved() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(member_partner_log)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (!columns.contains("partner_registered_fl")) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE member_partner_log DROP COLUMN partner_registered_fl");
    }

    private void ensureGodoMemberBusinessDuplicateMemNoColumn() {
        ensureColumn("godo_member_business", "duplicate_mem_no",
            "ALTER TABLE godo_member_business ADD COLUMN duplicate_mem_no TEXT");
    }

    private void ensureGodoMemberBusinessCodeColumns() {
        ensureColumn("godo_member_business", "service_code",
            "ALTER TABLE godo_member_business ADD COLUMN service_code TEXT");
        ensureColumn("godo_member_business", "item_code",
            "ALTER TABLE godo_member_business ADD COLUMN item_code TEXT");
        ensureColumn("godo_member_business_log", "service_code",
            "ALTER TABLE godo_member_business_log ADD COLUMN service_code TEXT");
        ensureColumn("godo_member_business_log", "item_code",
            "ALTER TABLE godo_member_business_log ADD COLUMN item_code TEXT");
    }

    private void ensureGodoMemberBusinessLogTable() {
        ensureTable("godo_member_business_log",
            "CREATE TABLE godo_member_business_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "mem_no BIGINT NOT NULL," +
            "company TEXT, business_no TEXT, ceo_name TEXT, service TEXT, item TEXT," +
            "com_address TEXT, biz_license_file TEXT," +
            "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no ON godo_member_business_log(mem_no)");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at ON godo_member_business_log(logged_at)");
    }

    private void ensureMemberPartnerLogTable() {
        ensureTable("member_partner_log",
            "CREATE TABLE member_partner_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "mem_no BIGINT NOT NULL," +
            "partner_code TEXT, partner_name TEXT," +
            "tax_invoice_fl TEXT, member_status TEXT," +
            "logged_at TEXT NOT NULL, logged_by TEXT, created_at TEXT NOT NULL)");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_member_partner_log_mem_no ON member_partner_log(mem_no)");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_member_partner_log_logged_at ON member_partner_log(logged_at)");
    }

    private void ensureCommonCodeTable() {
        ensureTable("common_code",
            "CREATE TABLE common_code (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "group_code TEXT NOT NULL, group_name TEXT NOT NULL," +
            "code TEXT NOT NULL, code_name TEXT NOT NULL," +
            "note TEXT, use_yn TEXT NOT NULL DEFAULT 'Y'," +
            "created_at TEXT NOT NULL, updated_at TEXT NOT NULL," +
            "UNIQUE (group_code, code))");
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_common_code_group_code ON common_code(group_code)");
    }

    private void ensureTable(String tableName, String createSql) {
        List<String> tables = jdbcTemplate.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'",
            (rs, i) -> rs.getString("name"));
        if (!tables.isEmpty()) return;
        jdbcTemplate.execute(createSql);
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(" + tableName + ")",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (columns.contains(columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
    }

    private void ensureGodoMemberBusinessServiceItemColumnsRenamed() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(godo_member_business)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (!columns.contains("service")) {
            return;  // Already renamed
        }
        // Rename service → service_name
        jdbcTemplate.execute("ALTER TABLE godo_member_business RENAME COLUMN service TO service_name");
        // Rename item → item_name
        jdbcTemplate.execute("ALTER TABLE godo_member_business RENAME COLUMN item TO item_name");

        // Also update log table
        List<String> logColumns = jdbcTemplate.query(
            "PRAGMA table_info(godo_member_business_log)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );
        if (logColumns.contains("service")) {
            jdbcTemplate.execute("ALTER TABLE godo_member_business_log RENAME COLUMN service TO service_name");
            jdbcTemplate.execute("ALTER TABLE godo_member_business_log RENAME COLUMN item TO item_name");
        }
    }

    private record ColumnInfo(String name, boolean notNull) {
    }
}
