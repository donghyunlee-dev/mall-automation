package com.sfood.automation.batch.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.application.usecase.FetchNewMembersUseCase;
import com.sfood.automation.member.domain.port.SfoodAdminMemberPort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MemberSyncBatchJobTest {

    private final SfoodAdminMemberPort memberPort = mock(SfoodAdminMemberPort.class);
    private final FetchNewMembersUseCase useCase = mock(FetchNewMembersUseCase.class);
    private final MemberSyncBatchJob job = new MemberSyncBatchJob(memberPort, useCase);

    @Test
    void should_use_korea_date_for_default_run() {
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        LocalDate beforeRun = LocalDate.now(koreaZone);
        when(memberPort.fetchMemberNos(any())).thenReturn(List.of());
        when(useCase.execute(List.of())).thenReturn("신규 0건, 업데이트 0건");

        job.run();

        LocalDate afterRun = LocalDate.now(koreaZone);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(memberPort).fetchMemberNos(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isBetween(beforeRun, afterRun);
    }

    @Test
    void should_return_result_message() {
        when(memberPort.fetchMemberNos(any())).thenReturn(List.of(900101L));
        when(memberPort.fetchMemberDetail(900101L)).thenReturn(mock(CrawledMember.class));
        when(useCase.execute(anyList())).thenReturn("신규 1건, 업데이트 0건");

        String result = job.run();

        assertThat(result).isEqualTo("신규 1건, 업데이트 0건");
    }

    @Test
    void should_return_zero_when_no_new_members() {
        when(memberPort.fetchMemberNos(any())).thenReturn(List.of());
        when(useCase.execute(List.of())).thenReturn("신규 0건, 업데이트 0건");

        String result = job.run();

        assertThat(result).isEqualTo("신규 0건, 업데이트 0건");
    }

}
