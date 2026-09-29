package com.sfood.automation.member.application;

import org.springframework.stereotype.Component;

@Component
public class MemberMasker {

    public String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0)
            + "*".repeat(name.length() - 2)
            + name.charAt(name.length() - 1);
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 1) {
            return "*" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    public String maskCellPhone(String cellPhone) {
        if (cellPhone == null || cellPhone.isBlank()) {
            return cellPhone;
        }
        String[] parts = cellPhone.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + "*".repeat(parts[1].length()) + "-" + parts[2];
        }
        String digits = cellPhone.trim();
        if (digits.length() > 7) {
            return digits.substring(0, 3)
                + "*".repeat(digits.length() - 7)
                + digits.substring(digits.length() - 4);
        }
        return "*".repeat(digits.length());
    }
}
