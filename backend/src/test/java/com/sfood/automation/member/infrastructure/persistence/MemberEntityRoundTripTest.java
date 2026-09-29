package com.sfood.automation.member.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.common.security.AesGcmEncryptor;
import com.sfood.automation.common.security.EncryptedFieldConverter;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AesGcmEncryptor.class, EncryptedFieldConverter.class})
class MemberEntityRoundTripTest {

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @Autowired
    private GodoMemberBusinessLogJpaRepository businessLogJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @Test
    void should_store_ciphertext_and_decrypt_on_read() {
        GodoMemberEntity member = newMember(900001L, "roundtrip-user1");
        member.setMemName("홍길동");
        member.setEmail("hong@example.com");
        member.setCellPhone("010-1234-5678");
        member.setPhone("02-123-4567");
        member.setBirthDt("1990-01-01");
        member.setAddress("서울시 강남구 테헤란로 1");
        godoMemberJpaRepository.save(member);
        entityManager.flush();

        String storedMemName = (String) entityManager.getEntityManager()
            .createNativeQuery("SELECT mem_name FROM godo_member WHERE mem_no = 900001")
            .getSingleResult();
        assertThat(storedMemName).isNotEqualTo("홍길동");
        assertThat(storedMemName).matches("^[A-Za-z0-9_=-]+$");

        entityManager.clear();
        GodoMemberEntity reloaded = godoMemberJpaRepository.findByMemNo(900001L).orElseThrow();
        assertThat(reloaded.getMemName()).isEqualTo("홍길동");
        assertThat(reloaded.getEmail()).isEqualTo("hong@example.com");
        assertThat(reloaded.getCellPhone()).isEqualTo("010-1234-5678");
        assertThat(reloaded.getPhone()).isEqualTo("02-123-4567");
        assertThat(reloaded.getBirthDt()).isEqualTo("1990-01-01");
        assertThat(reloaded.getAddress()).isEqualTo("서울시 강남구 테헤란로 1");
    }

    @Test
    void should_encrypt_business_pii_and_keep_business_no_plaintext() {
        GodoMemberEntity member = newMember(900002L, "roundtrip-user2");
        godoMemberJpaRepository.save(member);

        GodoMemberBusinessEntity business = new GodoMemberBusinessEntity();
        business.setMemNo(900002L);
        business.setCompany("(주)테스트");
        business.setBusinessNo("123-45-67890");
        business.setCeoName("김대표");
        business.setComAddress("서울시 송파구 올림픽로 2");
        business.setCreatedAt("2026-06-12T00:00:00Z");
        business.setUpdatedAt("2026-06-12T00:00:00Z");
        godoMemberBusinessJpaRepository.save(business);
        entityManager.flush();

        Object[] stored = (Object[]) entityManager.getEntityManager()
            .createNativeQuery("SELECT business_no, ceo_name FROM godo_member_business WHERE mem_no = 900002")
            .getSingleResult();
        assertThat(stored[0]).isEqualTo("123-45-67890");
        assertThat((String) stored[1]).isNotEqualTo("김대표");

        entityManager.clear();
        GodoMemberBusinessEntity reloaded = godoMemberBusinessJpaRepository.findByMemNo(900002L).orElseThrow();
        assertThat(reloaded.getCeoName()).isEqualTo("김대표");
        assertThat(reloaded.getComAddress()).isEqualTo("서울시 송파구 올림픽로 2");
    }

    @Test
    void should_save_and_find_member_partner() {
        GodoMemberEntity member = newMember(900003L, "roundtrip-user3");
        godoMemberJpaRepository.save(member);

        MemberPartnerEntity partner = new MemberPartnerEntity();
        partner.setMemNo(900003L);
        partner.setPartnerCode("990954");
        partner.setPartnerName("(에쓰푸드몰)테스트");
        partner.setTaxInvoiceFl("y");
        partner.setMemberStatus("NORMAL");
        partner.setCreatedAt("2026-06-12T00:00:00Z");
        partner.setUpdatedAt("2026-06-12T00:00:00Z");
        memberPartnerJpaRepository.save(partner);
        entityManager.flush();
        entityManager.clear();

        MemberPartnerEntity reloaded = memberPartnerJpaRepository.findByMemNo(900003L).orElseThrow();
        assertThat(reloaded.getMemberStatus()).isEqualTo("NORMAL");
        assertThat(reloaded.getPartnerCode()).isEqualTo("990954");
        assertThat(reloaded.getPartnerName()).isEqualTo("(에쓰푸드몰)테스트");
        assertThat(reloaded.getTaxInvoiceFl()).isEqualTo("y");
    }

    @Test
    void should_encrypt_business_log_pii_and_decrypt_on_read() {
        GodoMemberEntity member = newMember(900004L, "roundtrip-user4");
        godoMemberJpaRepository.save(member);

        GodoMemberBusinessLogEntity log = new GodoMemberBusinessLogEntity();
        log.setMemNo(900004L);
        log.setCompany("(주)테스트");
        log.setBusinessNo("123-45-67890");
        log.setCeoName("김대표");
        log.setComAddress("서울시 송파구 올림픽로 2");
        log.setLoggedAt("2026-06-12T00:00:00Z");
        log.setCreatedAt("2026-06-12T00:00:00Z");
        businessLogJpaRepository.save(log);
        entityManager.flush();

        Object[] stored = (Object[]) entityManager.getEntityManager()
            .createNativeQuery("SELECT business_no, ceo_name, com_address FROM godo_member_business_log WHERE mem_no = 900004")
            .getSingleResult();
        assertThat(stored[0]).isEqualTo("123-45-67890");
        assertThat((String) stored[1]).isNotEqualTo("김대표");
        assertThat((String) stored[2]).isNotEqualTo("서울시 송파구 올림픽로 2");

        entityManager.clear();
        GodoMemberBusinessLogEntity reloaded =
            businessLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900004L).get(0);
        assertThat(reloaded.getCeoName()).isEqualTo("김대표");
        assertThat(reloaded.getComAddress()).isEqualTo("서울시 송파구 올림픽로 2");
    }

    private GodoMemberEntity newMember(Long memNo, String memId) {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(memNo);
        member.setMemId(memId);
        member.setSyncedAt("2026-06-12T00:00:00Z");
        member.setCreatedAt("2026-06-12T00:00:00Z");
        member.setUpdatedAt("2026-06-12T00:00:00Z");
        return member;
    }
}
