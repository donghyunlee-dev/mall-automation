package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.common.security.HashUtil;
import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FetchNewMembersUseCaseTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final HashUtil hashUtil = mock(HashUtil.class);
    private final CommonCodeMappingService commonCodeMappingService = mock(CommonCodeMappingService.class);
    private final FetchNewMembersUseCase useCase =
        new FetchNewMembersUseCase(memberRepository, hashUtil, commonCodeMappingService);

    @Test
    void should_return_zero_message_when_empty_list() {
        String result = useCase.execute(List.of());

        assertThat(result).isEqualTo("신규 0건, 업데이트 0건");
        verify(memberRepository, never()).bulkInsertMembers(anyList());
        verify(memberRepository, never()).bulkUpdateMembers(anyList());
    }

    @Test
    void should_insert_new_member_and_business_for_b2b_group() {
        // groupSno=9 → B2B → hasBusiness()=true
        CrawledMember b2bMember = new CrawledMember(
            1001L, "testuser01", 9, "normal", "y", "2024-01-01", "2024-01-01",
            "pc", "2024-06-01", "홍길동", "길동", "test@example.com",
            "01012345678", "0212345678", "서울시 강남구", "m", "19900101",
            "n", null, "y", "y", "IT", "cooking", null,
            100000.0, 5000.0, 0.0, 10, null, 1, null, null, null, null,
            "테스트사", "123-45-67890", "홍대표", "식품", "가공식품", "서울시 서초구"
        );

        when(memberRepository.findExistingMemNos(List.of(1001L))).thenReturn(List.of());
        when(memberRepository.findExistingBusinessNos(List.of(1001L))).thenReturn(List.of());
        when(hashUtil.hash(any())).thenAnswer(inv -> "HASH_" + inv.getArgument(0));
        when(commonCodeMappingService.findServiceCode(any())).thenReturn(Optional.empty());
        when(commonCodeMappingService.findItemCode(any())).thenReturn(Optional.empty());

        String result = useCase.execute(List.of(b2bMember));

        assertThat(result).isEqualTo("신규 1건, 업데이트 0건");
        verify(memberRepository).bulkInsertMembers(anyList());
        verify(memberRepository, never()).bulkUpdateMembers(anyList());
        verify(memberRepository).bulkInsertBusinesses(anyList());
        verify(memberRepository, never()).bulkUpdateBusinesses(anyList(), any(MemberUploadType.class));
    }

    @Test
    void should_update_existing_member() {
        // groupSno=1 → GENERAL → hasBusiness()=false
        CrawledMember generalMember = new CrawledMember(
            2001L, "generaluser", 1, "normal", "y", "2023-05-01", "2023-05-01",
            "pc", "2024-06-01", "김일반", "일반이", "general@example.com",
            "01098765432", null, "부산시 해운대구", "f", "19850315",
            "y", "20100601", "y", "y", null, null, null,
            50000.0, 2000.0, 0.0, 5, null, 1, null, null, null, null,
            null, null, null, null, null, null
        );

        when(memberRepository.findExistingMemNos(List.of(2001L))).thenReturn(List.of(2001L));
        when(hashUtil.hash(any())).thenAnswer(inv -> "HASH_" + inv.getArgument(0));

        String result = useCase.execute(List.of(generalMember));

        assertThat(result).isEqualTo("신규 0건, 업데이트 1건");
        verify(memberRepository, never()).bulkInsertMembers(anyList());
        verify(memberRepository).bulkUpdateMembers(anyList());
        verify(memberRepository, never()).bulkInsertBusinesses(anyList());
    }
}
