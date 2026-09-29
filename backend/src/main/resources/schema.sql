CREATE TABLE IF NOT EXISTS email_verification (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    verification_key TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempt_count INTEGER NOT NULL DEFAULT 5,
    requested_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    verified_at TEXT,
    last_sent_at TEXT NOT NULL,
    request_ip TEXT,
    user_agent TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_email_verification_verification_key
    ON email_verification(verification_key);
CREATE INDEX IF NOT EXISTS idx_email_verification_email_status
    ON email_verification(email, status);
CREATE INDEX IF NOT EXISTS idx_email_verification_expires_at
    ON email_verification(expires_at);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    email_notification_yn TEXT NOT NULL DEFAULT 'N',
    teams_notification_yn TEXT NOT NULL DEFAULT 'N',
    last_login_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email
    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_email_notification_yn
    ON users(email_notification_yn);

CREATE TABLE IF NOT EXISTS goods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code TEXT NOT NULL UNIQUE,
    erp_goods_code TEXT NOT NULL,
    goods_name TEXT NOT NULL,
    only_mall_yn TEXT NOT NULL DEFAULT 'N',
    sold_out_yn TEXT NOT NULL DEFAULT 'N',
    available_qty INTEGER NOT NULL DEFAULT 0,
    safety_rate INTEGER NOT NULL DEFAULT 0,
    remain_valid_days INTEGER NOT NULL DEFAULT 0,
    godo_sync_yn TEXT NOT NULL DEFAULT 'N',
    erp_sync_yn TEXT NOT NULL DEFAULT 'N',
    godo_sync_blocked_yn TEXT NOT NULL DEFAULT 'N',
    description TEXT,
    message TEXT,
    status TEXT NOT NULL DEFAULT 'UNVERIFIED',
    created_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_id TEXT,
    updated_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_goods_godo_goods_code ON goods(godo_goods_code);
CREATE INDEX IF NOT EXISTS idx_goods_erp_goods_code ON goods(erp_goods_code);
CREATE INDEX IF NOT EXISTS idx_goods_status ON goods(status);
CREATE INDEX IF NOT EXISTS idx_goods_sold_out_yn ON goods(sold_out_yn);
CREATE INDEX IF NOT EXISTS idx_goods_erp_sync_yn ON goods(erp_sync_yn);

CREATE TABLE IF NOT EXISTS goods_status_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code TEXT NOT NULL,
    erp_goods_code TEXT NOT NULL,
    wms_available_qty INTEGER NOT NULL,
    criteria_qty INTEGER NOT NULL,
    safety_rate INTEGER NOT NULL,
    valid_percent INTEGER,
    remain_valid_days INTEGER,
    before_status TEXT NOT NULL,
    after_status TEXT NOT NULL,
    changed_at TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_goods_status_log_godo_goods_code
    ON goods_status_log(godo_goods_code);
CREATE INDEX IF NOT EXISTS idx_goods_status_log_changed_at
    ON goods_status_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_goods_status_log_after_status
    ON goods_status_log(after_status);

CREATE TABLE IF NOT EXISTS api_transaction_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    godo_goods_code TEXT NOT NULL,
    api_type TEXT NOT NULL,
    request_url TEXT NOT NULL,
    request_body TEXT,
    response_body TEXT,
    result_code TEXT,
    result_message TEXT,
    is_success INTEGER NOT NULL,
    requested_at TEXT NOT NULL,
    responded_at TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_transaction_log_godo_goods_code
    ON api_transaction_log(godo_goods_code);
CREATE INDEX IF NOT EXISTS idx_api_transaction_log_api_type
    ON api_transaction_log(api_type);
CREATE INDEX IF NOT EXISTS idx_api_transaction_log_requested_at
    ON api_transaction_log(requested_at);

CREATE TABLE IF NOT EXISTS common_status_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    scope TEXT NOT NULL UNIQUE,
    out_of_stock_qty INTEGER NOT NULL,
    on_sale_qty INTEGER NOT NULL,
    safety_rate INTEGER NOT NULL,
    remain_valid_days INTEGER NOT NULL,
    description TEXT,
    updated_id TEXT,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_common_status_config_scope
    ON common_status_config(scope);

CREATE TABLE IF NOT EXISTS batch_schedule_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name TEXT NOT NULL UNIQUE,
    enabled INTEGER NOT NULL,
    schedule_type TEXT NOT NULL,
    fixed_delay_seconds INTEGER NOT NULL,
    initial_delay_seconds INTEGER NOT NULL,
    description TEXT,
    last_modified_by TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_schedule_config_job_name
    ON batch_schedule_config(job_name);

CREATE TABLE IF NOT EXISTS batch_execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name TEXT NOT NULL,
    started_at TEXT NOT NULL,
    ended_at TEXT,
    status TEXT NOT NULL,
    message TEXT,
    triggered_by TEXT,
    duration_ms INTEGER,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_batch_execution_log_job_name
    ON batch_execution_log(job_name);
CREATE INDEX IF NOT EXISTS idx_batch_execution_log_status
    ON batch_execution_log(status);
CREATE INDEX IF NOT EXISTS idx_batch_execution_log_started_at
    ON batch_execution_log(started_at);

CREATE TABLE IF NOT EXISTS batch_schedule_change_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_name TEXT NOT NULL,
    before_fixed_delay_sec INTEGER,
    after_fixed_delay_sec INTEGER,
    before_enabled INTEGER,
    after_enabled INTEGER,
    changed_by TEXT,
    changed_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_batch_schedule_change_log_job_name
    ON batch_schedule_change_log(job_name);
CREATE INDEX IF NOT EXISTS idx_batch_schedule_change_log_changed_at
    ON batch_schedule_change_log(changed_at);

CREATE TABLE IF NOT EXISTS email_send_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    send_type TEXT NOT NULL,
    recipient TEXT NOT NULL,
    subject TEXT NOT NULL,
    is_success INTEGER NOT NULL,
    error_message TEXT,
    sent_at TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_send_log_recipient
    ON email_send_log(recipient);
CREATE INDEX IF NOT EXISTS idx_email_send_log_sent_at
    ON email_send_log(sent_at);

CREATE TABLE IF NOT EXISTS godo_member (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL UNIQUE,
    mem_id          TEXT    NOT NULL UNIQUE,
    group_sno       INTEGER NOT NULL DEFAULT 1,
    member_fl       TEXT    NOT NULL DEFAULT 'personal',
    app_fl          TEXT    NOT NULL DEFAULT 'n',
    app_dt          TEXT,
    mem_name        TEXT,
    mem_name_hash   TEXT,
    nick_name       TEXT,
    sex_fl          TEXT,
    birth_dt        TEXT,
    marri_fl        TEXT,
    marri_date      TEXT,
    job             TEXT,
    interest        TEXT,
    email           TEXT,
    email_hash      TEXT,
    cell_phone      TEXT,
    cell_phone_hash TEXT,
    phone           TEXT,
    fax             TEXT,
    mailling_fl     TEXT    NOT NULL DEFAULT 'y',
    sms_fl          TEXT    NOT NULL DEFAULT 'y',
    address         TEXT,
    entry_dt        TEXT,
    entry_path      TEXT    NOT NULL DEFAULT 'pc',
    last_login_dt   TEXT,
    login_count     INTEGER NOT NULL DEFAULT 0,
    sale_amount     DOUBLE  NOT NULL DEFAULT 0,
    mileage         DOUBLE  NOT NULL DEFAULT 0,
    deposit         DOUBLE  NOT NULL DEFAULT 0,
    recomm_id       TEXT,
    ex1             TEXT,
    ex2             TEXT,
    ex3             TEXT,
    ex4             TEXT,
    ex5             TEXT,
    ex6             TEXT,
    expiration_fl   INTEGER NOT NULL DEFAULT 1,
    memo            TEXT,
    deleted_fl      TEXT NOT NULL DEFAULT 'n',
    synced_at       TEXT NOT NULL,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_mem_no ON godo_member(mem_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_mem_id ON godo_member(mem_id);
CREATE INDEX IF NOT EXISTS idx_godo_member_group_sno       ON godo_member(group_sno);
CREATE INDEX IF NOT EXISTS idx_godo_member_app_fl          ON godo_member(app_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_member_fl       ON godo_member(member_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_entry_dt        ON godo_member(entry_dt);
CREATE INDEX IF NOT EXISTS idx_godo_member_deleted_fl      ON godo_member(deleted_fl);
CREATE INDEX IF NOT EXISTS idx_godo_member_mem_name_hash   ON godo_member(mem_name_hash);
CREATE INDEX IF NOT EXISTS idx_godo_member_email_hash      ON godo_member(email_hash);
CREATE INDEX IF NOT EXISTS idx_godo_member_cell_phone_hash ON godo_member(cell_phone_hash);

CREATE TABLE IF NOT EXISTS godo_member_business (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no          BIGINT  NOT NULL UNIQUE,
    company         TEXT,
    business_no     TEXT,
    ceo_name        TEXT,
    service_name    TEXT,
    item_name       TEXT,
    com_address     TEXT,
    biz_license_file TEXT,
    duplicate_mem_no TEXT,
    service_code    TEXT,
    item_code       TEXT,
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_member_business_mem_no
    ON godo_member_business(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_business_no
    ON godo_member_business(business_no);

CREATE TABLE IF NOT EXISTS member_partner (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL UNIQUE,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT NOT NULL DEFAULT 'n',
    member_status         TEXT NOT NULL DEFAULT 'NORMAL',
    created_at            TEXT NOT NULL,
    updated_at            TEXT NOT NULL,
    FOREIGN KEY (mem_no) REFERENCES godo_member(mem_no)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_member_partner_mem_no
    ON member_partner(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_partner_code
    ON member_partner(partner_code);
CREATE INDEX IF NOT EXISTS idx_member_partner_member_status
    ON member_partner(member_status);

CREATE TABLE IF NOT EXISTS godo_member_business_log (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no           BIGINT  NOT NULL,
    company          TEXT,
    business_no      TEXT,
    ceo_name         TEXT,
    service_name     TEXT,
    item_name        TEXT,
    com_address      TEXT,
    biz_license_file TEXT,
    service_code     TEXT,
    item_code        TEXT,
    logged_at        TEXT NOT NULL,
    logged_by        TEXT,
    created_at       TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_mem_no
    ON godo_member_business_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_godo_member_business_log_logged_at
    ON godo_member_business_log(logged_at);

CREATE TABLE IF NOT EXISTS member_partner_log (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    mem_no                BIGINT  NOT NULL,
    partner_code          TEXT,
    partner_name          TEXT,
    tax_invoice_fl        TEXT,
    member_status         TEXT,
    logged_at             TEXT NOT NULL,
    logged_by             TEXT,
    created_at            TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_mem_no
    ON member_partner_log(mem_no);
CREATE INDEX IF NOT EXISTS idx_member_partner_log_logged_at
    ON member_partner_log(logged_at);

CREATE TABLE IF NOT EXISTS common_code (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    group_code  TEXT NOT NULL,
    group_name  TEXT NOT NULL,
    code        TEXT NOT NULL,
    code_name   TEXT NOT NULL,
    note        TEXT,
    use_yn      TEXT NOT NULL DEFAULT 'Y',
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL,
    UNIQUE (group_code, code)
);

-- 대시보드 통합: 고도몰 주문 데이터 (참조: docs/dashboard-integration/sfood-mall-dashboard-current-state.md 9.3)
CREATE TABLE IF NOT EXISTS godo_order (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no                BIGINT  NOT NULL UNIQUE,
    member_id               TEXT    NOT NULL,
    member_grade            TEXT    NOT NULL,
    order_status            TEXT    NOT NULL,
    ordered_at              TEXT    NOT NULL,
    total_goods_price       NUMERIC NOT NULL,
    total_delivery_charge   NUMERIC NOT NULL,
    use_deposit             NUMERIC NOT NULL,
    use_mileage             NUMERIC NOT NULL,
    total_goods_dc_price    NUMERIC NOT NULL,
    total_member_dc_price   NUMERIC NOT NULL,
    total_coupon_dc_price   NUMERIC NOT NULL,
    payment_final_amount    NUMERIC NOT NULL,
    created_at              TEXT    NOT NULL,
    updated_at              TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_godo_order_ordered_at
    ON godo_order(ordered_at);
CREATE INDEX IF NOT EXISTS idx_godo_order_member_grade
    ON godo_order(member_grade);

CREATE TABLE IF NOT EXISTS godo_order_item (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    item_type               TEXT    NOT NULL,
    godo_line_no            BIGINT  NOT NULL,
    order_no                BIGINT  NOT NULL,
    product_name            TEXT    NOT NULL,
    product_qty             INTEGER NOT NULL,
    product_unit_price      NUMERIC NOT NULL,
    product_line_revenue    NUMERIC NOT NULL,
    delivered_at            TEXT,
    oms_shipped_at          TEXT,
    created_at              TEXT    NOT NULL,
    updated_at              TEXT    NOT NULL,
    FOREIGN KEY (order_no) REFERENCES godo_order(order_no)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_godo_order_item_type_line_no
    ON godo_order_item(item_type, godo_line_no);
CREATE INDEX IF NOT EXISTS idx_godo_order_item_order_no
    ON godo_order_item(order_no);
CREATE INDEX IF NOT EXISTS idx_godo_order_item_delivered_at
    ON godo_order_item(delivered_at);
CREATE INDEX IF NOT EXISTS idx_godo_order_item_oms_shipped_at
    ON godo_order_item(oms_shipped_at);
CREATE INDEX IF NOT EXISTS idx_godo_order_item_product_name
    ON godo_order_item(product_name);
CREATE INDEX IF NOT EXISTS idx_common_code_group_code
    ON common_code(group_code);
