package com.sfood.automation.common_code.application.usecase;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeJpaRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommonCodeMappingService {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeMappingService.class);

    private final CommonCodeJpaRepository repo;
    private Map<String, String> serviceNameToCode = new HashMap<>();
    private Map<String, String> itemNameToCode = new HashMap<>();

    public CommonCodeMappingService(CommonCodeJpaRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void loadCache() {
        try {
            Map<String, String> svc = new HashMap<>();
            Map<String, String> itm = new HashMap<>();
            repo.findByUseYn("Y").forEach(e -> {
                String key = e.getCodeName() == null ? null : e.getCodeName().trim().toLowerCase();
                if (key == null || key.isEmpty()) return;
                if ("SERVICE".equals(e.getGroupCode())) svc.put(key, e.getCode());
                else if ("ITEM".equals(e.getGroupCode())) itm.put(key, e.getCode());
            });
            this.serviceNameToCode = svc;
            this.itemNameToCode = itm;
        } catch (Exception ex) {
            log.warn("공통코드 캐시 로드 실패 — 코드 맵핑 비활성화: {}", ex.getMessage());
        }
    }

    public Optional<String> findServiceCode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return Optional.ofNullable(serviceNameToCode.get(text.trim().toLowerCase()));
    }

    public Optional<String> findItemCode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return Optional.ofNullable(itemNameToCode.get(text.trim().toLowerCase()));
    }
}
