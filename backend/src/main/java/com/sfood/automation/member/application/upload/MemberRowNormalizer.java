package com.sfood.automation.member.application.upload;

import com.sfood.automation.common.security.HashUtil;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 엑셀 원본 셀 값(LIST 43컬럼 / TEMPLATE 51컬럼)을 DB 저장 가능한 형태로 정규화한다.
 * 컬럼 인덱스는 LIST 기준 0-base이며, TEMPLATE은 동일 컬럼이 +8 오프셋에 위치한다.
 * 같은 날짜 컬럼에 엑셀 시리얼 숫자와 문자열이 행마다 혼재하므로(실측) 모든 날짜는 타입 분기한다.
 */
@Component
public class MemberRowNormalizer {

    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TEMPLATE_OFFSET = 8;

    private final HashUtil hashUtil;

    public MemberRowNormalizer(HashUtil hashUtil) {
        this.hashUtil = hashUtil;
    }

    public Long extractMemNo(List<Object> row, MemberUploadType type) {
        return normalizeLong(cell(row, 0, type));
    }

    public void applyMemberFields(GodoMemberEntity member, List<Object> row, MemberUploadType type) {
        member.setEntryDt(normalizeDateTime(cell(row, 1, type)));
        member.setAppDt(normalizeDateTime(cell(row, 2, type)));
        member.setAppFl(normalizeApproval(cell(row, 3, type)));
        member.setEntryPath(normalizeString(cell(row, 4, type), "pc"));
        member.setLastLoginDt(normalizeDateTime(cell(row, 5, type)));
        member.setMemberFl(normalizeString(cell(row, 6, type), "personal"));
        member.setGroupSno(normalizeInt(cell(row, 7, type), 1));
        member.setMemId(normalizeString(cell(row, 8, type), null));
        member.setNickName(normalizeString(cell(row, 9, type), null));

        String memName = normalizeString(cell(row, 10, type), null);
        member.setMemName(memName);
        member.setMemNameHash(hashUtil.hash(memName));

        String email = normalizeString(cell(row, 11, type), null);
        member.setEmail(email);
        member.setEmailHash(hashUtil.hash(email));

        String cellPhone = normalizeString(cell(row, 12, type), null);
        member.setCellPhone(cellPhone);
        member.setCellPhoneHash(cellPhone == null ? null : hashUtil.hash(cellPhone.replaceAll("[^0-9]", "")));

        member.setPhone(normalizeString(cell(row, 13, type), null));
        member.setAddress(normalizeString(cell(row, 14, type), null));
        member.setMaillingFl(normalizeFlag(cell(row, 15, type), "y"));
        member.setSmsFl(normalizeFlag(cell(row, 16, type), "y"));
        member.setSaleAmount(normalizeAmount(cell(row, 17, type)));
        member.setMileage(normalizeAmount(cell(row, 18, type)));
        member.setDeposit(normalizeAmount(cell(row, 19, type)));
        member.setLoginCount(normalizeInt(cell(row, 20, type), 0));
        member.setFax(normalizeString(cell(row, 27, type), null));
        member.setRecommId(normalizeString(cell(row, 28, type), null));
        member.setSexFl(normalizeSex(cell(row, 29, type)));
        member.setBirthDt(normalizeDate(cell(row, 30, type)));
        member.setMarriFl(normalizeNullableFlag(cell(row, 31, type)));
        member.setMarriDate(normalizeDate(cell(row, 32, type)));
        member.setJob(normalizeString(cell(row, 33, type), null));
        member.setInterest(normalizeString(cell(row, 34, type), null));
        member.setExpirationFl(normalizeInt(cell(row, 35, type), 1));
        member.setMemo(normalizeString(cell(row, 36, type), null));
        member.setEx1(normalizeString(cell(row, 37, type), null));
        member.setEx2(normalizeString(cell(row, 38, type), null));
        member.setEx3(normalizeString(cell(row, 39, type), null));
        member.setEx4(normalizeString(cell(row, 40, type), null));
        member.setEx5(normalizeString(cell(row, 41, type), null));
        member.setEx6(normalizeString(cell(row, 42, type), null));
    }

    public void applyBusinessFields(GodoMemberBusinessEntity business, List<Object> row, MemberUploadType type) {
        business.setCompany(normalizeString(cell(row, 21, type), null));
        business.setBusinessNo(normalizeString(cell(row, 22, type), null));
        business.setCeoName(normalizeString(cell(row, 23, type), null));
        business.setServiceName(normalizeString(cell(row, 24, type), null));
        business.setItemName(normalizeString(cell(row, 25, type), null));
        business.setComAddress(normalizeString(cell(row, 26, type), null));
        if (type == MemberUploadType.TEMPLATE) {
            business.setBizLicenseFile(normalizeString(templateCell(row, 7), null));
        }
    }

    public String extractCompany(List<Object> row, MemberUploadType type) {
        return normalizeString(cell(row, 21, type), null);
    }

    /** TEMPLATE 전용 — 인덱스 1~7의 에쓰푸드몰 거래처·관리 컬럼 (인덱스 4 거래처명 중복은 제외) */
    public void applyPartnerFields(MemberPartnerEntity partner, List<Object> row) {
        boolean registered = "y".equals(normalizePartnerRegistered(templateCell(row, 1)));
        Long partnerCode = normalizeLong(templateCell(row, 2));
        partner.setPartnerCode(partnerCode == null ? null : String.valueOf(partnerCode));
        partner.setPartnerName(normalizeString(templateCell(row, 3), null));
        partner.setTaxInvoiceFl(normalizePartnerRegistered(templateCell(row, 5)));
        partner.setMemberStatus(registered
            ? MemberPartnerStatus.NORMAL.name()
            : MemberPartnerStatus.UNREGISTERED.name());
    }

    private Object cell(List<Object> row, int listIndex, MemberUploadType type) {
        int index = type == MemberUploadType.TEMPLATE ? listIndex + TEMPLATE_OFFSET : listIndex;
        return index < row.size() ? row.get(index) : null;
    }

    private Object templateCell(List<Object> row, int templateIndex) {
        return templateIndex < row.size() ? row.get(templateIndex) : null;
    }

    String normalizeDate(Object cellValue) {
        if (cellValue instanceof Number serial) {
            return EXCEL_EPOCH.plusDays((long) serial.doubleValue()).toString();
        }
        if (cellValue instanceof String s && !s.isBlank()) {
            String trimmed = s.trim();
            // MySQL 제로 날짜("0000-00-00") = 값 없음 (실데이터 실측)
            if (trimmed.startsWith("0000-00-00")) {
                return null;
            }
            return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
        }
        return null;
    }

    String normalizeDateTime(Object cellValue) {
        if (cellValue instanceof Number serial) {
            double value = serial.doubleValue();
            long days = (long) value;
            long seconds = Math.round((value - days) * 86400);
            return EXCEL_EPOCH.plusDays(days).atStartOfDay().plusSeconds(seconds).format(DATETIME_FORMAT);
        }
        if (cellValue instanceof String s && !s.isBlank()) {
            String trimmed = s.trim();
            if (trimmed.startsWith("0000-00-00")) {
                return null;
            }
            return trimmed;
        }
        return null;
    }

    double normalizeAmount(Object cellValue) {
        if (cellValue == null) {
            return 0;
        }
        if (cellValue instanceof Number number) {
            return number.doubleValue();
        }
        String digits = cellValue.toString().replaceAll("[^0-9.-]", "");
        return digits.isBlank() ? 0 : Double.parseDouble(digits);
    }

    Long normalizeLong(Object cellValue) {
        if (cellValue instanceof Number number) {
            return number.longValue();
        }
        if (cellValue instanceof String s && !s.isBlank()) {
            try {
                return (long) Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    int normalizeInt(Object cellValue, int defaultValue) {
        Long value = normalizeLong(cellValue);
        return value == null ? defaultValue : value.intValue();
    }

    String normalizeString(Object cellValue, String defaultValue) {
        if (cellValue == null) {
            return defaultValue;
        }
        if (cellValue instanceof Number number) {
            double value = number.doubleValue();
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                return String.valueOf(number.longValue());
            }
            return cellValue.toString();
        }
        String trimmed = cellValue.toString().trim();
        return trimmed.isBlank() ? defaultValue : trimmed;
    }

    String normalizeApproval(Object cellValue) {
        if (cellValue == null) {
            return "n";
        }
        String value = cellValue.toString().trim();
        if ("승인".equals(value) || "y".equalsIgnoreCase(value)) {
            return "y";
        }
        return "n";
    }

    String normalizeFlag(Object cellValue, String defaultValue) {
        if (cellValue == null || cellValue.toString().isBlank()) {
            return defaultValue;
        }
        return "y".equalsIgnoreCase(cellValue.toString().trim()) ? "y" : "n";
    }

    String normalizeNullableFlag(Object cellValue) {
        if (cellValue == null || cellValue.toString().isBlank()) {
            return null;
        }
        return "y".equalsIgnoreCase(cellValue.toString().trim()) ? "y" : "n";
    }

    String normalizeSex(Object cellValue) {
        if (cellValue == null) {
            return null;
        }
        String value = cellValue.toString().trim().toLowerCase();
        return ("m".equals(value) || "w".equals(value)) ? value : null;
    }

    String normalizePartnerRegistered(Object cellValue) {
        if (cellValue == null) {
            return "n";
        }
        String value = cellValue.toString().trim();
        if ("O".equals(value) || "y".equalsIgnoreCase(value)) {
            return "y";
        }
        return "n";
    }
}
