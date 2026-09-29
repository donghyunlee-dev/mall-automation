package com.sfood.automation.member.infrastructure.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sfood.automation.common.security.AesGcmEncryptor;
import com.sfood.automation.common.security.HashUtil;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.domain.port.MemberListPage;
import com.sfood.automation.member.domain.port.MemberListRow;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.domain.port.MemberSummaryCounts;
import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

    // SQLite 바인딩 변수 한도(999) 이내로 IN 절을 분할한다
    private static final int IN_CLAUSE_BATCH_SIZE = 500;

    private static final QGodoMemberEntity MEMBER = QGodoMemberEntity.godoMemberEntity;
    private static final QGodoMemberBusinessEntity BUSINESS = QGodoMemberBusinessEntity.godoMemberBusinessEntity;
    private static final QMemberPartnerEntity PARTNER = QMemberPartnerEntity.memberPartnerEntity;
    private static final List<Integer> BUSINESS_GROUP_SNOS = List.of(2, 3, 5, 6);

    // @formatter:off
    private static final String INSERT_MEMBER_SQL =
        "INSERT INTO godo_member (mem_no,mem_id,group_sno,member_fl,app_fl,app_dt," +
        "mem_name,mem_name_hash,nick_name,sex_fl,birth_dt,marri_fl,marri_date," +
        "job,interest,email,email_hash,cell_phone,cell_phone_hash,phone,fax," +
        "mailling_fl,sms_fl,address,entry_dt,entry_path,last_login_dt," +
        "login_count,sale_amount,mileage,deposit,recomm_id," +
        "ex1,ex2,ex3,ex4,ex5,ex6,expiration_fl,memo," +
        "deleted_fl,synced_at,created_at,updated_at) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_MEMBER_SQL =
        "UPDATE godo_member SET mem_id=?,group_sno=?,member_fl=?,app_fl=?,app_dt=?," +
        "mem_name=?,mem_name_hash=?,nick_name=?,sex_fl=?,birth_dt=?,marri_fl=?,marri_date=?," +
        "job=?,interest=?,email=?,email_hash=?,cell_phone=?,cell_phone_hash=?,phone=?,fax=?," +
        "mailling_fl=?,sms_fl=?,address=?,entry_dt=?,entry_path=?,last_login_dt=?," +
        "login_count=?,sale_amount=?,mileage=?,deposit=?,recomm_id=?," +
        "ex1=?,ex2=?,ex3=?,ex4=?,ex5=?,ex6=?,expiration_fl=?,memo=?," +
        "deleted_fl=?,synced_at=?,updated_at=? WHERE mem_no=?";

    private static final String INSERT_BIZ_SQL =
        "INSERT INTO godo_member_business (mem_no,company,business_no,ceo_name,service_name,service_code,item_name,item_code,com_address,biz_license_file,created_at,updated_at) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_BIZ_LIST_SQL =
        "UPDATE godo_member_business SET company=?,business_no=?,ceo_name=?,service_name=?,service_code=COALESCE(?,service_code),item_name=?,item_code=COALESCE(?,item_code),com_address=?,updated_at=? WHERE mem_no=?";

    private static final String UPDATE_BIZ_TEMPLATE_SQL =
        "UPDATE godo_member_business SET company=?,business_no=?,ceo_name=?,service_name=?,service_code=COALESCE(?,service_code),item_name=?,item_code=COALESCE(?,item_code),com_address=?,biz_license_file=?,updated_at=? WHERE mem_no=?";

    private static final String INSERT_PARTNER_SQL =
        "INSERT INTO member_partner (mem_no,partner_code,partner_name,tax_invoice_fl,member_status,created_at,updated_at) " +
        "VALUES (?,?,?,?,?,?,?)";

    private static final String UPDATE_PARTNER_SQL =
        "UPDATE member_partner SET partner_code=?,partner_name=?,tax_invoice_fl=?,member_status=?,updated_at=? WHERE mem_no=?";
    // @formatter:on

    private final GodoMemberJpaRepository godoMemberJpaRepository;
    private final GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    private final MemberPartnerJpaRepository memberPartnerJpaRepository;
    private final GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
    private final MemberPartnerLogJpaRepository partnerLogJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final HashUtil hashUtil;
    private final JdbcTemplate jdbcTemplate;
    private final AesGcmEncryptor encryptor;

    public MemberRepositoryImpl(
        GodoMemberJpaRepository godoMemberJpaRepository,
        GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository,
        MemberPartnerJpaRepository memberPartnerJpaRepository,
        GodoMemberBusinessLogJpaRepository businessLogJpaRepository,
        MemberPartnerLogJpaRepository partnerLogJpaRepository,
        EntityManager entityManager,
        HashUtil hashUtil,
        JdbcTemplate jdbcTemplate,
        AesGcmEncryptor encryptor
    ) {
        this.godoMemberJpaRepository = godoMemberJpaRepository;
        this.godoMemberBusinessJpaRepository = godoMemberBusinessJpaRepository;
        this.memberPartnerJpaRepository = memberPartnerJpaRepository;
        this.businessLogJpaRepository = businessLogJpaRepository;
        this.partnerLogJpaRepository = partnerLogJpaRepository;
        this.queryFactory = new JPAQueryFactory(entityManager);
        this.hashUtil = hashUtil;
        this.jdbcTemplate = jdbcTemplate;
        this.encryptor = encryptor;
    }

    @Override
    public MemberListPage findPage(MemberSearchCondition condition) {
        BooleanBuilder where = buildWhere(condition);

        List<Tuple> tuples = queryFactory
            .select(MEMBER, BUSINESS, PARTNER.partnerCode, PARTNER.partnerName, PARTNER.memberStatus)
            .from(MEMBER)
            .leftJoin(BUSINESS).on(BUSINESS.memNo.eq(MEMBER.memNo))
            .leftJoin(PARTNER).on(PARTNER.memNo.eq(MEMBER.memNo))
            .where(where)
            .orderBy(orderSpecifier(condition))
            .offset((long) condition.page() * condition.size())
            .limit(condition.size())
            .fetch();

        Long total = queryFactory.select(MEMBER.count())
            .from(MEMBER)
            .leftJoin(PARTNER).on(PARTNER.memNo.eq(MEMBER.memNo))
            .where(where)
            .fetchOne();

        List<MemberListRow> rows = tuples.stream()
            .map(tuple -> new MemberListRow(
                tuple.get(MEMBER),
                tuple.get(BUSINESS),
                tuple.get(PARTNER.partnerCode),
                tuple.get(PARTNER.partnerName),
                tuple.get(PARTNER.memberStatus)))
            .toList();
        return new MemberListPage(rows, total == null ? 0 : total);
    }

    @Override
    public boolean existsActivePartnerCode(String partnerCode, Long excludeMemNo) {
        Long count = queryFactory.select(PARTNER.count())
            .from(PARTNER)
            .join(MEMBER).on(MEMBER.memNo.eq(PARTNER.memNo))
            .where(PARTNER.partnerCode.eq(partnerCode)
                .and(PARTNER.memNo.ne(excludeMemNo))
                .and(MEMBER.deletedFl.eq("n")))
            .fetchOne();
        return count != null && count > 0;
    }

    private BooleanBuilder buildWhere(MemberSearchCondition condition) {
        BooleanBuilder where = new BooleanBuilder(MEMBER.deletedFl.eq("n"));

        if (condition.keyword() != null) {
            switch (condition.searchType()) {
                case MEM_ID -> where.and(MEMBER.memId.contains(condition.keyword()));
                case NAME -> where.and(MEMBER.memNameHash.eq(hashUtil.hash(condition.keyword())));
                case EMAIL -> where.and(MEMBER.emailHash.eq(hashUtil.hash(condition.keyword())));
                case CELL_PHONE -> where.and(MEMBER.cellPhoneHash.eq(
                    hashUtil.hash(condition.keyword().replaceAll("[^0-9]", ""))));
                case PARTNER_CODE -> where.and(PARTNER.partnerCode.contains(condition.keyword()));
                case PARTNER_NAME -> where.and(PARTNER.partnerName.contains(condition.keyword()));
            }
        }
        switch (condition.group()) {
            case GENERAL -> where.and(MEMBER.groupSno.eq(1));
            case BUSINESS -> where.and(MEMBER.groupSno.in(BUSINESS_GROUP_SNOS));
            case SKILLED -> where.and(MEMBER.groupSno.eq(8));
            case B2B -> where.and(MEMBER.groupSno.eq(9));
            case ALL -> { }
        }
        if (condition.appFl() != null) {
            where.and(MEMBER.appFl.eq(condition.appFl()));
        }
        if (MemberPartnerStatus.UNREGISTERED.name().equals(condition.memberStatus())) {
            where.and(PARTNER.memberStatus.eq(MemberPartnerStatus.UNREGISTERED.name())
                .or(PARTNER.memberStatus.isNull()));
        } else if (condition.memberStatus() != null) {
            where.and(PARTNER.memberStatus.eq(condition.memberStatus()));
        }
        if (condition.entryDtFrom() != null) {
            where.and(MEMBER.entryDt.goe(condition.entryDtFrom().toString()));
        }
        if (condition.entryDtTo() != null) {
            where.and(MEMBER.entryDt.lt(condition.entryDtTo().plusDays(1).toString()));
        }
        return where;
    }

    private OrderSpecifier<?> orderSpecifier(MemberSearchCondition condition) {
        ComparableExpressionBase<?> column = switch (condition.sortKey()) {
            case "mem_id" -> MEMBER.memId;
            case "group_sno" -> MEMBER.groupSno;
            case "app_fl" -> MEMBER.appFl;
            case "entry_dt" -> MEMBER.entryDt;
            case "last_login_dt" -> MEMBER.lastLoginDt;
            case "sale_amount" -> MEMBER.saleAmount;
            default -> MEMBER.memNo;
        };
        return "asc".equals(condition.sortDir()) ? column.asc() : column.desc();
    }

    @Override
    public MemberSummaryCounts findSummary() {
        BooleanBuilder active = new BooleanBuilder(MEMBER.deletedFl.eq("n"));
        long total = count(active);
        long business = count(new BooleanBuilder(active).and(MEMBER.groupSno.in(BUSINESS_GROUP_SNOS)));
        long b2b = count(new BooleanBuilder(active).and(MEMBER.groupSno.eq(9)));
        long pending = count(new BooleanBuilder(active).and(MEMBER.appFl.eq("n")));
        String lastSyncedAt = queryFactory.select(MEMBER.syncedAt.max()).from(MEMBER).where(active).fetchOne();
        return new MemberSummaryCounts(total, business, b2b, pending, lastSyncedAt);
    }

    private long count(BooleanBuilder where) {
        Long count = queryFactory.select(MEMBER.count()).from(MEMBER).where(where).fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public Optional<GodoMemberEntity> findByMemNo(Long memNo) {
        return godoMemberJpaRepository.findByMemNo(memNo);
    }

    @Override
    public void save(GodoMemberEntity member) {
        godoMemberJpaRepository.save(member);
    }

    @Override
    public void saveAll(List<GodoMemberEntity> members) {
        godoMemberJpaRepository.saveAll(members);
    }

    @Override
    public List<Long> findAllActiveMemNos() {
        return godoMemberJpaRepository.findAllActiveMemNos();
    }

    @Override
    public int softDeleteByMemNos(Collection<Long> memNos, String updatedAt) {
        List<Long> targets = new ArrayList<>(memNos);
        int deleted = 0;
        for (int from = 0; from < targets.size(); from += IN_CLAUSE_BATCH_SIZE) {
            List<Long> batch = targets.subList(from, Math.min(from + IN_CLAUSE_BATCH_SIZE, targets.size()));
            deleted += godoMemberJpaRepository.softDeleteByMemNos(batch, updatedAt);
        }
        return deleted;
    }

    @Override
    public Optional<GodoMemberBusinessEntity> findBusinessByMemNo(Long memNo) {
        return godoMemberBusinessJpaRepository.findByMemNo(memNo);
    }

    @Override
    public void saveBusiness(GodoMemberBusinessEntity business) {
        godoMemberBusinessJpaRepository.save(business);
    }

    @Override
    public Optional<MemberPartnerEntity> findPartnerByMemNo(Long memNo) {
        return memberPartnerJpaRepository.findByMemNo(memNo);
    }

    @Override
    public void savePartner(MemberPartnerEntity partner) {
        memberPartnerJpaRepository.save(partner);
    }

    @Override
    public List<Long> findExistingMemNos(List<Long> memNos) {
        return batchIn(memNos, godoMemberJpaRepository::findMemNosByMemNoIn);
    }

    @Override
    public List<Long> findExistingBusinessNos(List<Long> memNos) {
        return batchIn(memNos, godoMemberBusinessJpaRepository::findMemNosByMemNoIn);
    }

    @Override
    public List<Long> findExistingPartnerNos(List<Long> memNos) {
        return batchIn(memNos, memberPartnerJpaRepository::findMemNosByMemNoIn);
    }

    @Override
    public void bulkInsertMembers(List<GodoMemberEntity> members) {
        if (members.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_MEMBER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GodoMemberEntity m = members.get(i);
                int p = 1;
                ps.setObject(p++, m.getMemNo());
                ps.setObject(p++, m.getMemId());
                ps.setObject(p++, m.getGroupSno());
                ps.setObject(p++, m.getMemberFl());
                ps.setObject(p++, m.getAppFl());
                ps.setObject(p++, m.getAppDt());
                ps.setObject(p++, enc(m.getMemName()));
                ps.setObject(p++, m.getMemNameHash());
                ps.setObject(p++, m.getNickName());
                ps.setObject(p++, m.getSexFl());
                ps.setObject(p++, enc(m.getBirthDt()));
                ps.setObject(p++, m.getMarriFl());
                ps.setObject(p++, m.getMarriDate());
                ps.setObject(p++, m.getJob());
                ps.setObject(p++, m.getInterest());
                ps.setObject(p++, enc(m.getEmail()));
                ps.setObject(p++, m.getEmailHash());
                ps.setObject(p++, enc(m.getCellPhone()));
                ps.setObject(p++, m.getCellPhoneHash());
                ps.setObject(p++, enc(m.getPhone()));
                ps.setObject(p++, m.getFax());
                ps.setObject(p++, m.getMaillingFl());
                ps.setObject(p++, m.getSmsFl());
                ps.setObject(p++, enc(m.getAddress()));
                ps.setObject(p++, m.getEntryDt());
                ps.setObject(p++, m.getEntryPath());
                ps.setObject(p++, m.getLastLoginDt());
                ps.setObject(p++, m.getLoginCount());
                ps.setObject(p++, m.getSaleAmount());
                ps.setObject(p++, m.getMileage());
                ps.setObject(p++, m.getDeposit());
                ps.setObject(p++, m.getRecommId());
                ps.setObject(p++, m.getEx1());
                ps.setObject(p++, m.getEx2());
                ps.setObject(p++, m.getEx3());
                ps.setObject(p++, m.getEx4());
                ps.setObject(p++, m.getEx5());
                ps.setObject(p++, m.getEx6());
                ps.setObject(p++, m.getExpirationFl());
                ps.setObject(p++, m.getMemo());
                ps.setObject(p++, m.getDeletedFl());
                ps.setObject(p++, m.getSyncedAt());
                ps.setObject(p++, m.getCreatedAt());
                ps.setObject(p, m.getUpdatedAt());
            }

            @Override
            public int getBatchSize() { return members.size(); }
        });
    }

    @Override
    public void bulkUpdateMembers(List<GodoMemberEntity> members) {
        if (members.isEmpty()) return;
        jdbcTemplate.batchUpdate(UPDATE_MEMBER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GodoMemberEntity m = members.get(i);
                int p = 1;
                ps.setObject(p++, m.getMemId());
                ps.setObject(p++, m.getGroupSno());
                ps.setObject(p++, m.getMemberFl());
                ps.setObject(p++, m.getAppFl());
                ps.setObject(p++, m.getAppDt());
                ps.setObject(p++, enc(m.getMemName()));
                ps.setObject(p++, m.getMemNameHash());
                ps.setObject(p++, m.getNickName());
                ps.setObject(p++, m.getSexFl());
                ps.setObject(p++, enc(m.getBirthDt()));
                ps.setObject(p++, m.getMarriFl());
                ps.setObject(p++, m.getMarriDate());
                ps.setObject(p++, m.getJob());
                ps.setObject(p++, m.getInterest());
                ps.setObject(p++, enc(m.getEmail()));
                ps.setObject(p++, m.getEmailHash());
                ps.setObject(p++, enc(m.getCellPhone()));
                ps.setObject(p++, m.getCellPhoneHash());
                ps.setObject(p++, enc(m.getPhone()));
                ps.setObject(p++, m.getFax());
                ps.setObject(p++, m.getMaillingFl());
                ps.setObject(p++, m.getSmsFl());
                ps.setObject(p++, enc(m.getAddress()));
                ps.setObject(p++, m.getEntryDt());
                ps.setObject(p++, m.getEntryPath());
                ps.setObject(p++, m.getLastLoginDt());
                ps.setObject(p++, m.getLoginCount());
                ps.setObject(p++, m.getSaleAmount());
                ps.setObject(p++, m.getMileage());
                ps.setObject(p++, m.getDeposit());
                ps.setObject(p++, m.getRecommId());
                ps.setObject(p++, m.getEx1());
                ps.setObject(p++, m.getEx2());
                ps.setObject(p++, m.getEx3());
                ps.setObject(p++, m.getEx4());
                ps.setObject(p++, m.getEx5());
                ps.setObject(p++, m.getEx6());
                ps.setObject(p++, m.getExpirationFl());
                ps.setObject(p++, m.getMemo());
                ps.setObject(p++, m.getDeletedFl());
                ps.setObject(p++, m.getSyncedAt());
                ps.setObject(p++, m.getUpdatedAt());
                ps.setObject(p, m.getMemNo());
            }

            @Override
            public int getBatchSize() { return members.size(); }
        });
    }

    @Override
    public void bulkInsertBusinesses(List<GodoMemberBusinessEntity> businesses) {
        if (businesses.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_BIZ_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GodoMemberBusinessEntity b = businesses.get(i);
                ps.setObject(1, b.getMemNo());
                ps.setObject(2, b.getCompany());
                ps.setObject(3, b.getBusinessNo());
                ps.setObject(4, enc(b.getCeoName()));
                ps.setObject(5, b.getServiceName());
                ps.setObject(6, b.getServiceCode());
                ps.setObject(7, b.getItemName());
                ps.setObject(8, b.getItemCode());
                ps.setObject(9, enc(b.getComAddress()));
                ps.setObject(10, b.getBizLicenseFile());
                ps.setObject(11, b.getCreatedAt());
                ps.setObject(12, b.getUpdatedAt());
            }

            @Override
            public int getBatchSize() { return businesses.size(); }
        });
    }

    @Override
    public void bulkUpdateBusinesses(List<GodoMemberBusinessEntity> businesses, MemberUploadType type) {
        if (businesses.isEmpty()) return;
        String sql = type == MemberUploadType.TEMPLATE ? UPDATE_BIZ_TEMPLATE_SQL : UPDATE_BIZ_LIST_SQL;
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                GodoMemberBusinessEntity b = businesses.get(i);
                int p = 1;
                ps.setObject(p++, b.getCompany());
                ps.setObject(p++, b.getBusinessNo());
                ps.setObject(p++, enc(b.getCeoName()));
                ps.setObject(p++, b.getServiceName());
                ps.setObject(p++, b.getServiceCode());
                ps.setObject(p++, b.getItemName());
                ps.setObject(p++, b.getItemCode());
                ps.setObject(p++, enc(b.getComAddress()));
                if (type == MemberUploadType.TEMPLATE) {
                    ps.setObject(p++, b.getBizLicenseFile());
                }
                ps.setObject(p++, b.getUpdatedAt());
                ps.setObject(p, b.getMemNo());
            }

            @Override
            public int getBatchSize() { return businesses.size(); }
        });
    }

    @Override
    public void bulkInsertPartners(List<MemberPartnerEntity> partners) {
        if (partners.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_PARTNER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MemberPartnerEntity p = partners.get(i);
                ps.setObject(1, p.getMemNo());
                ps.setObject(2, p.getPartnerCode());
                ps.setObject(3, p.getPartnerName());
                ps.setObject(4, p.getTaxInvoiceFl());
                ps.setObject(5, p.getMemberStatus());
                ps.setObject(6, p.getCreatedAt());
                ps.setObject(7, p.getUpdatedAt());
            }

            @Override
            public int getBatchSize() { return partners.size(); }
        });
    }

    @Override
    public void bulkUpdatePartners(List<MemberPartnerEntity> partners) {
        if (partners.isEmpty()) return;
        jdbcTemplate.batchUpdate(UPDATE_PARTNER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MemberPartnerEntity p = partners.get(i);
                ps.setObject(1, p.getPartnerCode());
                ps.setObject(2, p.getPartnerName());
                ps.setObject(3, p.getTaxInvoiceFl());
                ps.setObject(4, p.getMemberStatus());
                ps.setObject(5, p.getUpdatedAt());
                ps.setObject(6, p.getMemNo());
            }

            @Override
            public int getBatchSize() { return partners.size(); }
        });
    }

    @Override
    public int findMaxPartnerCodeNumeric() {
        List<String> codes = jdbcTemplate.queryForList(
            "SELECT partner_code FROM member_partner WHERE partner_code IS NOT NULL AND partner_code != ''",
            String.class);
        return codes.stream()
            .filter(c -> c.matches("\\d{1,6}"))
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0);
    }

    @Override
    public void saveBusinessLog(GodoMemberBusinessLogEntity log) {
        businessLogJpaRepository.save(log);
    }

    @Override
    public void savePartnerLog(MemberPartnerLogEntity log) {
        partnerLogJpaRepository.save(log);
    }

    @Override
    public List<GodoMemberBusinessLogEntity> findBusinessLogsByMemNo(Long memNo) {
        return businessLogJpaRepository.findByMemNoOrderByLoggedAtDesc(memNo);
    }

    @Override
    public List<MemberPartnerLogEntity> findPartnerLogsByMemNo(Long memNo) {
        return partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(memNo);
    }

    @Override
    public List<Long> findOtherMemNosByBusinessNo(String businessNo, Long excludeMemNo) {
        if (businessNo == null || businessNo.isBlank()) return List.of();
        return jdbcTemplate.queryForList(
            "SELECT mem_no FROM godo_member_business WHERE business_no = ? AND mem_no != ?",
            Long.class, businessNo, excludeMemNo);
    }

    @Override
    public void bulkUpdateDuplicateMemNos(List<Long> memNos, List<String> duplicateMemNos) {
        if (memNos.isEmpty()) return;
        if (memNos.size() != duplicateMemNos.size()) {
            throw new IllegalArgumentException("memNos and duplicateMemNos must have the same size");
        }
        jdbcTemplate.batchUpdate(
            "UPDATE godo_member_business SET duplicate_mem_no = ? WHERE mem_no = ?",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setObject(1, duplicateMemNos.get(i));
                    ps.setObject(2, memNos.get(i));
                }
                @Override
                public int getBatchSize() { return memNos.size(); }
            });
    }

    private String enc(String value) {
        return value == null ? null : encryptor.encrypt(value);
    }

    private List<Long> batchIn(List<Long> memNos, java.util.function.Function<List<Long>, List<Long>> query) {
        List<Long> result = new ArrayList<>();
        for (int from = 0; from < memNos.size(); from += IN_CLAUSE_BATCH_SIZE) {
            List<Long> batch = memNos.subList(from, Math.min(from + IN_CLAUSE_BATCH_SIZE, memNos.size()));
            result.addAll(query.apply(batch));
        }
        return result;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteMember(Long memNo) {
        Optional<MemberPartnerEntity> partner = memberPartnerJpaRepository.findByMemNo(memNo);
        boolean isRegistered = partner.isPresent()
            && !MemberPartnerStatus.UNREGISTERED.name().equals(partner.get().getMemberStatus());

        if (isRegistered) {
            String now = java.time.Instant.now().toString();
            godoMemberJpaRepository.softDeleteByMemNos(List.of(memNo), now);
        } else {
            memberPartnerJpaRepository.findByMemNo(memNo)
                .ifPresent(p -> memberPartnerJpaRepository.deleteById(p.getMemNo()));
            godoMemberBusinessJpaRepository.findByMemNo(memNo)
                .ifPresent(b -> godoMemberBusinessJpaRepository.deleteById(b.getMemNo()));
            godoMemberJpaRepository.hardDeleteByMemNo(memNo);
        }
    }
}
