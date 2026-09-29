# 회원 이력 조회 API 중복 호출 제거 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 회원 상세 모달에서 "수정이력"을 열 때 `GET /api/members/{memNo}/business/logs`·`.../partner/logs`가 2번 호출되는 버그를 고친다.

**Architecture:** `MemberHistoryModal`의 자체 조회 로직(자체 fetch effect, 자체 error state)을 완전히 제거하고, `MemberDetailModal`을 이력 데이터의 유일한 조회 주체로 만든다. `MemberHistoryModal`은 부모가 준 데이터·에러를 표시만 하는 순수 컴포넌트가 된다.

**Tech Stack:** React 18 + TypeScript(strict), Vitest + @testing-library/react

**설계 문서:** `docs/superpowers/specs/2026-07-10-member-history-log-duplicate-fetch-design.md`

---

### Task 1: `MemberDetailModal`이 이력 에러 상태를 소유하도록 변경

**Files:**
- Modify: `src/components/members/MemberDetailModal.tsx:56-99` (state·preload effect), `:250-261` (`MemberHistoryModal` 렌더)
- Test: `src/components/members/MemberDetailModal.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/components/members/MemberDetailModal.test.tsx`의 `describe('MemberDetailModal', ...)` 블록 맨 끝(파일 끝 `});` 바로 앞)에 아래 테스트 2개를 추가한다. 이 파일은 이미 `getMemberBusinessLogs`/`getMemberPartnerLogs`를 사용하지 않으므로, 파일 상단 import 목록에 다음을 추가한다:

```tsx
import { getMemberBusinessLogs, getMemberPartnerLogs } from '../../api/members';
```

기존 `vi.mock('../../api/commonCodes')` 아래에 다음 mock을 추가한다:

```tsx
vi.mock('../../api/members', () => ({
  getMemberBusinessLogs: vi.fn(),
  getMemberPartnerLogs: vi.fn(),
}));

const mockedBizLogs = vi.mocked(getMemberBusinessLogs);
const mockedPartnerLogs = vi.mocked(getMemberPartnerLogs);
```

`beforeEach(() => { ... })` 블록 안(`vi.clearAllMocks();` 다음 줄)에 두 mock의 기본 동작을 추가한다:

```tsx
    mockedBizLogs.mockResolvedValue([]);
    mockedPartnerLogs.mockResolvedValue([]);
```

파일 끝에 아래 테스트 2개를 추가한다:

```tsx
  it('이력 미리 로드 응답이 늦어도 수정이력 버튼을 눌렀을 때 API가 추가로 호출되지 않는다', async () => {
    mockedBizLogs.mockImplementation(() => new Promise(() => {})); // 응답이 영영 안 오는 상태를 흉내
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const bizHeader = screen.getByText('사업자 정보').parentElement!;
    fireEvent.click(within(bizHeader).getByText('수정이력'));

    await waitFor(() => expect(screen.getByText('사업자 정보 수정 이력')).toBeInTheDocument());
    expect(mockedBizLogs).toHaveBeenCalledTimes(1);
  });

  it('이력 미리 로드 실패 시 이력 모달에 에러 메시지가 표시된다', async () => {
    mockedBizLogs.mockRejectedValue(new Error('network error'));
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const bizHeader = screen.getByText('사업자 정보').parentElement!;
    fireEvent.click(within(bizHeader).getByText('수정이력'));

    await waitFor(() => expect(screen.getByText('이력을 불러오지 못했어요.')).toBeInTheDocument());
  });
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `npx vitest run src/components/members/MemberDetailModal.test.tsx`
Expected: FAIL — 첫 번째 테스트는 현재 `MemberHistoryModal`이 `businessLogs` prop과 무관하게 자체적으로 `getMemberBusinessLogs`를 호출하므로 실패(호출 횟수가 1이 아니거나, promise가 안 끝나 "불러오는 중..."에 머물러 제목 텍스트 대기가 타임아웃)한다. 두 번째 테스트는 `MemberDetailModal`이 아직 에러 상태를 갖지 않아 "이력을 불러오지 못했어요." 문구가 뜨지 않아 실패한다.

- [ ] **Step 3: 최소 구현**

`src/components/members/MemberDetailModal.tsx`를 수정한다.

기존:

```tsx
  const [businessLogs, setBusinessLogs] = useState<MemberBusinessLogItem[] | undefined>(undefined);
  const [partnerLogs, setPartnerLogs] = useState<MemberPartnerLogItem[] | undefined>(undefined);
```

다음으로 교체:

```tsx
  const [businessLogs, setBusinessLogs] = useState<MemberBusinessLogItem[] | undefined>(undefined);
  const [partnerLogs, setPartnerLogs] = useState<MemberPartnerLogItem[] | undefined>(undefined);
  const [businessLogsError, setBusinessLogsError] = useState<string | null>(null);
  const [partnerLogsError, setPartnerLogsError] = useState<string | null>(null);
```

기존:

```tsx
  // member 갱신(저장·이동) 시 이전 회원 이력이 잠깐 보이지 않도록 초기화 — 렌더 중 상태 조정
  const [prevMember, setPrevMember] = useState(member);
  if (prevMember !== member) {
    setPrevMember(member);
    setBusinessLogs(undefined);
    setPartnerLogs(undefined);
  }

  // 이력 미리 로드 — 이력 버튼 클릭 시 로딩 없이 즉시 표시 (member 갱신 시 재요청)
  useEffect(() => {
    if (!member) return;
    let alive = true;
    const no = member.memNo;
    getMemberBusinessLogs(no).then((d) => { if (alive) setBusinessLogs(d); }).catch(() => undefined);
    getMemberPartnerLogs(no).then((d) => { if (alive) setPartnerLogs(d); }).catch(() => undefined);
    return () => { alive = false; };
  }, [member]);
```

다음으로 교체:

```tsx
  // member 갱신(저장·이동) 시 이전 회원 이력이 잠깐 보이지 않도록 초기화 — 렌더 중 상태 조정
  const [prevMember, setPrevMember] = useState(member);
  if (prevMember !== member) {
    setPrevMember(member);
    setBusinessLogs(undefined);
    setPartnerLogs(undefined);
    setBusinessLogsError(null);
    setPartnerLogsError(null);
  }

  // 이력 미리 로드 — 이력 버튼 클릭 시 로딩 없이 즉시 표시 (member 갱신 시 재요청)
  // MemberHistoryModal은 자체 조회를 하지 않으므로, 이 요청이 유일한 이력 조회 경로다.
  useEffect(() => {
    if (!member) return;
    let alive = true;
    const no = member.memNo;
    getMemberBusinessLogs(no)
      .then((d) => { if (alive) { setBusinessLogs(d); setBusinessLogsError(null); } })
      .catch(() => { if (alive) setBusinessLogsError('이력을 불러오지 못했어요.'); });
    getMemberPartnerLogs(no)
      .then((d) => { if (alive) { setPartnerLogs(d); setPartnerLogsError(null); } })
      .catch(() => { if (alive) setPartnerLogsError('이력을 불러오지 못했어요.'); });
    return () => { alive = false; };
  }, [member]);
```

기존:

```tsx
      {member && (
        <MemberHistoryModal
          kind={historyKind}
          memNo={member.memNo}
          onClose={() => setHistoryKind(null)}
          businessLogs={businessLogs}
          partnerLogs={partnerLogs}
          currentBusiness={member.business}
          currentPartner={member.partner}
        />
      )}
```

다음으로 교체:

```tsx
      {member && (
        <MemberHistoryModal
          kind={historyKind}
          onClose={() => setHistoryKind(null)}
          businessLogs={businessLogs}
          partnerLogs={partnerLogs}
          businessLogsError={businessLogsError}
          partnerLogsError={partnerLogsError}
          currentBusiness={member.business}
          currentPartner={member.partner}
        />
      )}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `npx vitest run src/components/members/MemberDetailModal.test.tsx`
Expected: 여전히 FAIL — `MemberHistoryModal`이 아직 새 prop(`businessLogsError`/`partnerLogsError`)을 받지 않고 자체 조회를 계속하기 때문이다(Task 2에서 마저 고친다). TypeScript 빌드가 깨지지 않는지만 `npx tsc --noEmit`로 확인한다.

Run: `npx tsc --noEmit`
Expected: `MemberHistoryModal`이 아직 `memNo`를 필수 prop으로 요구하고 `businessLogsError`/`partnerLogsError`를 모른다면 타입 오류가 날 수 있다 — Task 2에서 `MemberHistoryModal`의 Props를 함께 고치므로, 이 시점에는 타입 오류가 나도 정상이다. 커밋은 Task 2 완료 후 두 파일을 함께 묶어서 한다.

- [ ] **Step 5: Task 2로 이동 (아직 커밋하지 않음)**

이 Task는 `MemberHistoryModal.tsx`가 새 prop을 지원해야 완전해지므로, Task 2까지 마친 뒤 두 파일을 한 커밋으로 묶는다.

---

### Task 2: `MemberHistoryModal`의 자체 조회 로직 제거

**Files:**
- Modify: `src/components/members/MemberHistoryModal.tsx` (전체)

- [ ] **Step 1: 구현**

`src/components/members/MemberHistoryModal.tsx`의 import 구문 기존:

```tsx
import { useEffect, useMemo, useState } from 'react';
import modalStyles from '../modals/Modal.module.scss';
import styles from './MemberHistoryModal.module.scss';
import { getMemberBusinessLogs, getMemberPartnerLogs } from '../../api/members';
import type {
  MemberBusinessLogItem,
  MemberPartnerLogItem,
  MemberHistoryKind,
  MemberBusinessDetail,
  MemberPartnerDetail,
} from '../../types/member';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  kind: MemberHistoryKind | null;
  memNo: number;
  onClose: () => void;
  // 상세 모달에서 미리 로드한 이력 — 있으면 즉시 표시(로딩 화면 없이)
  businessLogs?: MemberBusinessLogItem[];
  partnerLogs?: MemberPartnerLogItem[];
  // 현재 레코드 값 — 백엔드가 변경 전(before) 값을 로깅하므로,
  // 가장 최근 변경의 결과 값은 현재 레코드에서 가져온다
  currentBusiness?: MemberBusinessDetail | null;
  currentPartner?: MemberPartnerDetail | null;
}
```

다음으로 교체:

```tsx
import { useEffect, useMemo } from 'react';
import modalStyles from '../modals/Modal.module.scss';
import styles from './MemberHistoryModal.module.scss';
import type {
  MemberBusinessLogItem,
  MemberPartnerLogItem,
  MemberHistoryKind,
  MemberBusinessDetail,
  MemberPartnerDetail,
} from '../../types/member';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  kind: MemberHistoryKind | null;
  onClose: () => void;
  // 상세 모달(MemberDetailModal)이 유일한 조회 주체다 — 이 컴포넌트는 표시만 한다.
  businessLogs?: MemberBusinessLogItem[];
  partnerLogs?: MemberPartnerLogItem[];
  businessLogsError?: string | null;
  partnerLogsError?: string | null;
  // 현재 레코드 값 — 백엔드가 변경 전(before) 값을 로깅하므로,
  // 가장 최근 변경의 결과 값은 현재 레코드에서 가져온다
  currentBusiness?: MemberBusinessDetail | null;
  currentPartner?: MemberPartnerDetail | null;
}
```

컴포넌트 본문 기존:

```tsx
const MemberHistoryModal = ({
  kind,
  memNo,
  onClose,
  businessLogs,
  partnerLogs,
  currentBusiness,
  currentPartner,
}: Props) => {
  // 미리 로드된 이력이 없을 때만 자체 조회 — 결과는 캐시에 비동기로만 기록
  const [fetchedBizLogs, setFetchedBizLogs] = useState<MemberBusinessLogItem[] | null>(null);
  const [fetchedPartnerLogs, setFetchedPartnerLogs] = useState<MemberPartnerLogItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 회원이 바뀌면 자체 조회 캐시 무효화, 모달을 다시 열면 이전 오류 초기화 — 렌더 중 상태 조정
  const [prevMemNo, setPrevMemNo] = useState(memNo);
  if (prevMemNo !== memNo) {
    setPrevMemNo(memNo);
    setFetchedBizLogs(null);
    setFetchedPartnerLogs(null);
    setError(null);
  }
  const [prevKind, setPrevKind] = useState(kind);
  if (prevKind !== kind) {
    setPrevKind(kind);
    setError(null);
  }

  const effectiveBizLogs = businessLogs ?? fetchedBizLogs;
  const effectivePartnerLogs = partnerLogs ?? fetchedPartnerLogs;

  useLockBodyScroll(kind != null);

  // ESC 닫기 — 캡처 단계에서 전파를 막아 뒤의 상세 모달까지 함께 닫히지 않도록 처리
  useEffect(() => {
    if (kind == null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.stopPropagation();
      onClose();
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [kind, onClose]);

  useEffect(() => {
    if (kind == null) return;
    let alive = true;
    if (kind === 'BUSINESS') {
      if (effectiveBizLogs) return;
      getMemberBusinessLogs(memNo)
        .then((d) => { if (alive) setFetchedBizLogs(d); })
        .catch(() => { if (alive) setError('이력을 불러오지 못했어요.'); });
    } else {
      if (effectivePartnerLogs) return;
      getMemberPartnerLogs(memNo)
        .then((d) => { if (alive) setFetchedPartnerLogs(d); })
        .catch(() => { if (alive) setError('이력을 불러오지 못했어요.'); });
    }
    return () => { alive = false; };
  }, [kind, memNo, effectiveBizLogs, effectivePartnerLogs]);

  const rows = useMemo<DisplayRow[]>(() => {
    if (kind === 'BUSINESS') {
      if (!effectiveBizLogs) return [];
      return toRows(effectiveBizLogs, bizValues, currentBusiness ? bizValues(currentBusiness) : null);
    }
    if (kind === 'PARTNER') {
      if (!effectivePartnerLogs) return [];
      return toRows(effectivePartnerLogs, partnerValues, currentPartner ? partnerValues(currentPartner) : null);
    }
    return [];
  }, [kind, effectiveBizLogs, effectivePartnerLogs, currentBusiness, currentPartner]);

  const activeLogs = kind === 'BUSINESS' ? effectiveBizLogs : effectivePartnerLogs;
  const loading = kind != null && activeLogs == null && error == null;
```

다음으로 교체:

```tsx
const MemberHistoryModal = ({
  kind,
  onClose,
  businessLogs,
  partnerLogs,
  businessLogsError,
  partnerLogsError,
  currentBusiness,
  currentPartner,
}: Props) => {
  useLockBodyScroll(kind != null);

  // ESC 닫기 — 캡처 단계에서 전파를 막아 뒤의 상세 모달까지 함께 닫히지 않도록 처리
  useEffect(() => {
    if (kind == null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.stopPropagation();
      onClose();
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [kind, onClose]);

  const rows = useMemo<DisplayRow[]>(() => {
    if (kind === 'BUSINESS') {
      if (!businessLogs) return [];
      return toRows(businessLogs, bizValues, currentBusiness ? bizValues(currentBusiness) : null);
    }
    if (kind === 'PARTNER') {
      if (!partnerLogs) return [];
      return toRows(partnerLogs, partnerValues, currentPartner ? partnerValues(currentPartner) : null);
    }
    return [];
  }, [kind, businessLogs, partnerLogs, currentBusiness, currentPartner]);

  const activeLogs = kind === 'BUSINESS' ? businessLogs : partnerLogs;
  const error = kind === 'BUSINESS' ? businessLogsError : partnerLogsError;
  const loading = kind != null && activeLogs == null && !error;
```

**주의**: 이 아래(반환하는 JSX 부분)에서 `error` 변수를 그대로 참조하는 부분(`{loading ? '불러오는 중...' : error ? error : '수정 이력이 없습니다.'}`)은 이미 위에서 재정의한 `error`(kind에 따라 선택된 에러 문자열)를 그대로 쓰므로 수정할 필요가 없다.

- [ ] **Step 2: 테스트 실행**

Run: `npx vitest run src/components/members/MemberDetailModal.test.tsx`
Expected: PASS — Task 1에서 추가한 2개 테스트 포함, 기존 테스트 전부 통과.

- [ ] **Step 3: 타입·린트·빌드 확인**

Run: `npx tsc --noEmit`
Expected: 오류 없음 (특히 `MemberHistoryModal`에서 `memNo`/`getMemberBusinessLogs`/`getMemberPartnerLogs`/`useState` 미사용으로 인한 오류가 없어야 한다 — 전부 제거했으므로 문제 없어야 함)

Run: `npm run lint`
Expected: 경고 0

- [ ] **Step 4: 커밋 (Task 1 변경분과 함께)**

```bash
git add src/components/members/MemberDetailModal.tsx src/components/members/MemberHistoryModal.tsx src/components/members/MemberDetailModal.test.tsx
git commit -m "fix: 회원 이력 조회를 상세 모달 단일 경로로 통합 — 수정이력 조회 시 API 중복 호출 제거"
```

---

### Task 3: 전체 검증 및 PR

**Files:** 없음 (검증만 수행)

- [ ] **Step 1: 전체 테스트 실행**

Run: `npx vitest run`
Expected: 모든 테스트 파일 PASS

- [ ] **Step 2: 빌드 확인**

Run: `npm run build`
Expected: `tsc -b && vite build` 성공, 타입 오류 없음

- [ ] **Step 3: 린트 확인**

Run: `npm run lint`
Expected: 경고 0

- [ ] **Step 4: 브랜치 푸시 + PR 생성**

```bash
git push -u origin fix/member-history-log-duplicate-fetch
gh pr create --base develop --title "fix: 회원 수정이력 조회 시 log API 중복 호출 제거" --body "$(cat <<'EOF'
## Summary
- 회원 상세 모달에서 "수정이력"을 열 때 `GET /api/members/{memNo}/business/logs`·`.../partner/logs`가 2번 호출되던 버그 수정
  - 원인: `MemberDetailModal`의 이력 미리 로드(preload)와 `MemberHistoryModal` 자체의 "미리 로드 안 됐으면 재조회" 로직이 서로의 진행 상태를 모른 채 독립 동작 — preload 응답이 늦으면 이력 버튼 클릭 시 중복 호출됨(재현 테스트로 확인)
  - 수정: `MemberHistoryModal`의 자체 조회 로직을 완전히 제거하고, `MemberDetailModal`을 이력 데이터의 유일한 조회 주체로 통합. 에러 상태도 부모가 소유해 `businessLogsError`/`partnerLogsError`로 전달
- 설계 문서: `docs/superpowers/specs/2026-07-10-member-history-log-duplicate-fetch-design.md`

## Test plan
- [x] `MemberDetailModal.test.tsx` 신규 2개 테스트 (preload 지연 시에도 API 1번만 호출, preload 실패 시 에러 메시지 표시)
- [x] `npx vitest run` 전체 통과
- [x] `npm run build` 통과
- [x] `npm run lint` 통과 (경고 0)

## Note
- `MemberHistoryModal`을 부모(`MemberDetailModal`) 없이 단독으로 재사용할 수 있던 기존의 "폴백 자체 조회" 능력은 이번 변경으로 제거됨(의도된 설계 결정 — 설계 문서 "범위 밖" 참조)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Self-Review 체크리스트 (계획 작성자용, 참고)

- **Spec 커버리지**: 설계 문서의 "핵심 동작" 1~3번 모두 Task 1·2의 테스트로 커버됨(정상 케이스는 기존 테스트가, 지연 케이스와 실패 케이스는 신규 테스트가 검증). "구현 범위"의 두 파일(`MemberDetailModal.tsx`, `MemberHistoryModal.tsx`) 모두 Task 1·2에 반영됨.
- **타입 일관성**: `businessLogsError`/`partnerLogsError`는 `MemberDetailModal.tsx`(state 선언·전달)와 `MemberHistoryModal.tsx`(Props 선언·사용) 양쪽에서 동일한 이름·타입(`string | null | undefined`)으로 일치함.
- **범위 밖 항목**: 재시도 버튼, `MemberHistoryModal` 단독 재사용성 유지는 설계 문서와 동일하게 이 계획에도 포함하지 않음.
