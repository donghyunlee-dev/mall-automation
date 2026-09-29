package com.sfood.automation.batch.infrastructure.job;

import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.application.usecase.FetchNewMembersUseCase;
import com.sfood.automation.member.domain.port.SfoodAdminMemberPort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MemberSyncBatchJob {

    private static final Logger log = LoggerFactory.getLogger(MemberSyncBatchJob.class);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final SfoodAdminMemberPort memberPort;
    private final FetchNewMembersUseCase fetchNewMembersUseCase;

    public MemberSyncBatchJob(
        SfoodAdminMemberPort memberPort,
        FetchNewMembersUseCase fetchNewMembersUseCase
    ) {
        this.memberPort = memberPort;
        this.fetchNewMembersUseCase = fetchNewMembersUseCase;
    }

    public String run() {
        return run(LocalDate.now(KOREA_ZONE));
    }

    public String run(LocalDate date) {
        log.info("MemberSyncBatchJob started: date={}", date);

        List<Long> memNos = memberPort.fetchMemberNos(date);
        log.info("Fetched {} member nos", memNos.size());

        List<CrawledMember> members = memNos.stream()
            .map(memberPort::fetchMemberDetail)
            .toList();

        String result = fetchNewMembersUseCase.execute(members);
        log.info("MemberSyncBatchJob completed: {}", result);
        return result;
    }
}
