# 업태/업종 코드·명 세트 동기화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 회원 상세 모달의 업태/업종 코드·명 입력을 자유 편집에서 "편집 시 공통코드 검색으로 강제 동기화"로 바꿔, 코드와 명이 항상 유효한 세트로만 확정되도록 한다.

**Architecture:** `CommonCodeSearchModal`에 `initialQuery`/`autoSelectSingleMatch` prop을 추가해 "검색어 미리 채우기 + 결과 1건이면 자동 선택" 기능을 갖추고, `MemberDetailModal.tsx`의 `BusinessCard`가 코드/명 각각에 "직전 확정 값"을 추적해 blur/Enter 시 값이 바뀐 경우에만 이 모달을 자동으로 연다.

**Tech Stack:** React 18 + TypeScript(strict), Vitest + @testing-library/react

**설계 문서:** `docs/superpowers/specs/2026-07-10-service-item-code-name-sync-design.md`

---

### Task 1: `CommonCodeSearchModal`에 `initialQuery` / `autoSelectSingleMatch` 추가

**Files:**
- Modify: `src/components/modals/CommonCodeSearchModal.tsx`
- Test: `src/components/modals/CommonCodeSearchModal.test.tsx` (신규)

- [ ] **Step 1: 실패하는 테스트 작성**

`src/components/modals/CommonCodeSearchModal.test.tsx` 파일을 새로 만든다.

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import CommonCodeSearchModal from './CommonCodeSearchModal';
import { getCommonCodes } from '../../api/commonCodes';
import type { CommonCodeItem } from '../../types/commonCode';

vi.mock('../../api/commonCodes');
const mockedCodes = vi.mocked(getCommonCodes);

const code = (c: string, name: string): CommonCodeItem => ({
  id: Number(c) || c.length,
  groupCode: 'SERVICE',
  groupName: '업태',
  code: c,
  codeName: name,
  note: null,
  useYn: 'Y',
  createdAt: '',
  updatedAt: '',
});

describe('CommonCodeSearchModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initialQuery가 있으면 검색창에 초기값이 채워진다', () => {
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '소매')]}
        initialQuery="47"
        onSelect={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toHaveValue('47');
    expect(screen.getByText('도매')).toBeInTheDocument();
    expect(screen.queryByText('소매')).not.toBeInTheDocument();
  });

  it('autoSelectSingleMatch=true + 결과 1건 → onSelect 자동 호출', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '소매')]}
        initialQuery="47"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(onSelect).toHaveBeenCalledWith('47', '도매'));
  });

  it('autoSelectSingleMatch=true + 결과 0건 → onSelect 호출 안 됨, 결과 없음 표시', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매')]}
        initialQuery="존재안함"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('검색 결과가 없어요.')).toBeInTheDocument());
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('autoSelectSingleMatch=true + 결과 2건 이상 → onSelect 호출 안 됨, 목록 유지', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '도매업')]}
        initialQuery="도매"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('도매')).toBeInTheDocument());
    expect(screen.getByText('도매업')).toBeInTheDocument();
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('autoSelectSingleMatch 미지정(기본값) + 결과 1건이어도 onSelect 자동 호출 안 됨', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매')]}
        initialQuery="47"
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('도매')).toBeInTheDocument());
    expect(onSelect).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `npx vitest run src/components/modals/CommonCodeSearchModal.test.tsx`
Expected: FAIL — `initialQuery`/`autoSelectSingleMatch` prop이 아직 존재하지 않아 첫 번째 테스트부터 검색창 값이 빈 문자열(`''`)로 나와 실패한다. 나머지 4개 테스트도 `onSelect`가 호출되지 않아 실패한다.

- [ ] **Step 3: 최소 구현**

`src/components/modals/CommonCodeSearchModal.tsx`을 수정한다.

`Props` 인터페이스에 두 필드를 추가한다(기존 필드는 그대로 둔다):

```tsx
interface Props {
  groupCode: CommonCodeGroup;
  onSelect: (code: string, name: string) => void;
  onClose: () => void;
  initialItems?: CommonCodeItem[];
  initialQuery?: string;
  autoSelectSingleMatch?: boolean;
}
```

컴포넌트 시그니처와 `query` 초기값을 변경한다:

```tsx
const CommonCodeSearchModal = ({
  groupCode,
  onSelect,
  onClose,
  initialItems,
  initialQuery,
  autoSelectSingleMatch,
}: Props) => {
  // items가 null이면 아직 로딩 전 — loading은 파생 상태로 처리 (effect 내 동기 setState 제거)
  const [items, setItems] = useState<CommonCodeItem[] | null>(initialItems ?? null);
  const [query, setQuery] = useState(initialQuery ?? '');
  const [error, setError] = useState<string | null>(null);
  const loading = items == null && error == null;
```

기존 `filtered` `useMemo` 바로 아래에, 최초 1회만 동작하는 자동 선택 effect를 추가한다:

```tsx
  // autoSelectSingleMatch: 모달이 열린 최초 필터링 결과가 정확히 1건이면 자동 선택.
  // 이후 사용자가 검색어를 더 타이핑해 결과가 바뀌는 경우에는 재적용하지 않는다(1회성).
  const autoSelectChecked = useRef(false);
  useEffect(() => {
    if (!autoSelectSingleMatch || autoSelectChecked.current || items == null) return;
    autoSelectChecked.current = true;
    if (filtered.length === 1) {
      onSelect(filtered[0].code, filtered[0].codeName);
    }
  }, [items, autoSelectSingleMatch, filtered, onSelect]);
```

`useRef`는 이미 최상단 import에 포함되어 있으므로(리스트 뷰포트 측정용) 추가 import는 필요 없다.

- [ ] **Step 4: 테스트 통과 확인**

Run: `npx vitest run src/components/modals/CommonCodeSearchModal.test.tsx`
Expected: PASS (5개 테스트 전체)

- [ ] **Step 5: 커밋**

```bash
git add src/components/modals/CommonCodeSearchModal.tsx src/components/modals/CommonCodeSearchModal.test.tsx
git commit -m "feat: CommonCodeSearchModal에 초기 검색어·단일결과 자동선택 옵션 추가"
```

---

### Task 2: `BusinessCard` — 편집 시 세트 동기화 로직 연결

**Files:**
- Modify: `src/components/members/MemberDetailModal.tsx:298-455` (`BusinessCard`), `:608-637` (`CodePickerRow`)
- Test: `src/components/members/MemberDetailModal.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`src/components/members/MemberDetailModal.test.tsx`의 `describe('MemberDetailModal', ...)` 블록 맨 끝(마지막 `it('× 버튼 클릭 ...)` 앞 또는 뒤 — 기존 테스트 순서를 건드리지 않도록 파일 끝 `});` 바로 앞에 추가)에 아래 테스트들을 추가한다.

```tsx
  it('업태 코드를 수정하면 명이 즉시 비워진다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });

    expect(nameInput).toHaveValue('');
  });

  it('업태 명을 수정하면 코드가 즉시 비워진다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(nameInput, { target: { value: '소매' } });

    expect(codeInput).toHaveValue('');
  });

  it('코드·명이 그대로인데 blur만 하면 검색 모달이 열리지 않는다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    fireEvent.blur(codeInput);

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('코드를 비우고 blur하면 검색 모달이 열리지 않는다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    fireEvent.change(codeInput, { target: { value: '' } });
    fireEvent.blur(codeInput);

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('업태 코드 변경 후 blur, 검색 결과 1건 → 코드+명 자동 채움, 모달이 닫힌다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '99', '음식점업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(nameInput).toHaveValue('음식점업'));
    expect(codeInput).toHaveValue('99');
    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('업태 코드 입력 후 Enter → 검색 결과 1건이면 자동으로 채워진다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '99', '음식점업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });
    fireEvent.keyDown(codeInput, { key: 'Enter' });

    await waitFor(() => expect(nameInput).toHaveValue('음식점업'));
  });

  it('업태 코드 변경 후 blur, 검색 결과 2건 이상 → 모달이 열린 채로 유지된다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '48', '도매업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '4' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toBeInTheDocument());
    expect(nameInput).toHaveValue('');
  });

  it('자동으로 열린 검색모달을 선택 없이 닫으면 입력값이 그대로 남는다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '48', '도매업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');

    fireEvent.change(codeInput, { target: { value: '4' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(screen.getByText('업태 검색')).toBeInTheDocument());
    const searchModalHead = screen.getByText('업태 검색').parentElement!;
    fireEvent.click(within(searchModalHead).getByLabelText('닫기'));

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
    expect(codeInput).toHaveValue('4');
  });

  it('업종 코드 변경 후 blur, 검색 결과 1건 → 코드+명 자동 채움(업태와 동일 로직 적용 확인)', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매')]
          : [code('ITEM', '01', '식품'), code('ITEM', '02', '음료')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const itemRow = screen.getByText('업종').parentElement!;
    const codeInput = within(itemRow).getByPlaceholderText('코드');
    const nameInput = within(itemRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '02' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(nameInput).toHaveValue('음료'));
  });

  it('수동 검색(찾기) 버튼 클릭 → 결과가 1건이어도 자동으로 닫히지 않는다(기존 수동 탐색 동작 보존)', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE' ? [code('SERVICE', '47', '도매')] : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    fireEvent.click(within(serviceRow).getByTitle('검색'));

    await waitFor(() => expect(screen.getByText('업태 검색')).toBeInTheDocument());
    expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toHaveValue('');
    // 결과가 1건이어도(도매 1건) 자동 선택되지 않아 모달이 계속 열려 있어야 한다
    expect(screen.getByText('업태 검색')).toBeInTheDocument();
  });
```

**주의**: `code(...)` 헬퍼와 `mockedCodes`, `setHook`, `business`, `partner`, `baseMember`는 이 파일 상단에 이미 정의되어 있으므로 재정의하지 않는다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `npx vitest run src/components/members/MemberDetailModal.test.tsx`
Expected: FAIL — 새로 추가한 10개 테스트 전부 실패(코드 변경 시 명 자동 비움 로직·blur/Enter 커밋 핸들러·`CommonCodeSearchModal`으로의 `initialQuery`/`autoSelectSingleMatch` 전달이 아직 없음). "수동 검색 버튼" 테스트는 기존 동작이라 이미 통과할 수 있음 — 통과해도 문제 없음(회귀 방지용 가드).

- [ ] **Step 3: 최소 구현**

`src/components/members/MemberDetailModal.tsx`의 `BusinessCard` 컴포넌트를 수정한다.

기존:

```tsx
  const [serviceCode, setServiceCode] = useState(business?.serviceCode ?? '');
  const [serviceName, setServiceName] = useState(business?.serviceName ?? '');
  const [itemCode, setItemCode] = useState(business?.itemCode ?? '');
  const [itemName, setItemName] = useState(business?.itemName ?? '');
  const [pickerOpen, setPickerOpen] = useState<CommonCodeGroup | null>(null);
```

다음으로 교체:

```tsx
  const [serviceCode, setServiceCode] = useState(business?.serviceCode ?? '');
  const [serviceName, setServiceName] = useState(business?.serviceName ?? '');
  const [itemCode, setItemCode] = useState(business?.itemCode ?? '');
  const [itemName, setItemName] = useState(business?.itemName ?? '');

  // 업태/업종 각각 "직전 확정 값"(초기 로드·검색 선택·저장 완료 시점) — blur/Enter 시 실제 변경 여부 판단용
  const [confirmedService, setConfirmedService] = useState({
    code: business?.serviceCode ?? '',
    name: business?.serviceName ?? '',
  });
  const [confirmedItem, setConfirmedItem] = useState({
    code: business?.itemCode ?? '',
    name: business?.itemName ?? '',
  });

  const [pickerOpen, setPickerOpen] = useState<CommonCodeGroup | null>(null);
  const [pickerQuery, setPickerQuery] = useState('');
  const [pickerAutoSelect, setPickerAutoSelect] = useState(false);
```

기존 `handleSave` 안의 성공 분기:

```tsx
    if (ok) {
      onToast('사업자 정보를 저장했어요.', 'success');
      onDirtyChange?.(false);
      onSaved?.();
    }
```

다음으로 교체:

```tsx
    if (ok) {
      onToast('사업자 정보를 저장했어요.', 'success');
      onDirtyChange?.(false);
      onSaved?.();
      setConfirmedService({ code: serviceCode.trim(), name: serviceName.trim() });
      setConfirmedItem({ code: itemCode.trim(), name: itemName.trim() });
    }
```

`handleSave` 함수 바로 아래(return문 이전)에 업태/업종 공용 커밋 헬퍼를 추가한다:

```tsx
  // 코드/명 blur·Enter 커밋 — 직전 확정 값과 실제로 다를 때만 검색 모달을 연다
  const commitCodeName = (
    group: CommonCodeGroup,
    codeVal: string,
    nameVal: string,
    confirmed: { code: string; name: string },
  ) => {
    if (pickerOpen === group) return;
    if (codeVal === confirmed.code && nameVal === confirmed.name) return;
    const query = codeVal || nameVal;
    if (!query) return;
    setPickerQuery(query);
    setPickerAutoSelect(true);
    setPickerOpen(group);
  };
  const commitService = () => commitCodeName('SERVICE', serviceCode.trim(), serviceName.trim(), confirmedService);
  const commitItem = () => commitCodeName('ITEM', itemCode.trim(), itemName.trim(), confirmedItem);
```

`CodePickerRow` 사용 부분(업태·업종) 교체:

```tsx
        <CodePickerRow
          label="업태"
          code={serviceCode}
          name={serviceName}
          onCodeChange={(v) => { setServiceCode(v); setServiceName(''); markDirty(); }}
          onNameChange={(v) => { setServiceName(v); setServiceCode(''); markDirty(); }}
          onCommit={commitService}
          onPick={() => { setPickerQuery(''); setPickerAutoSelect(false); setPickerOpen('SERVICE'); }}
          invalid={serviceInvalid}
        />
        <CodePickerRow
          label="업종"
          code={itemCode}
          name={itemName}
          onCodeChange={(v) => { setItemCode(v); setItemName(''); markDirty(); }}
          onNameChange={(v) => { setItemName(v); setItemCode(''); markDirty(); }}
          onCommit={commitItem}
          onPick={() => { setPickerQuery(''); setPickerAutoSelect(false); setPickerOpen('ITEM'); }}
          invalid={itemInvalid}
        />
```

`CommonCodeSearchModal` 렌더 부분 교체:

```tsx
        {pickerOpen && (
          <CommonCodeSearchModal
            groupCode={pickerOpen}
            initialItems={codesLoaded ? (pickerOpen === 'SERVICE' ? serviceCodes : itemCodes) : undefined}
            initialQuery={pickerQuery}
            autoSelectSingleMatch={pickerAutoSelect}
            onSelect={(code, name) => {
              if (pickerOpen === 'SERVICE') {
                setServiceCode(code);
                setServiceName(name);
                setConfirmedService({ code, name });
              } else {
                setItemCode(code);
                setItemName(name);
                setConfirmedItem({ code, name });
              }
              setPickerOpen(null);
              setPickerQuery('');
              setPickerAutoSelect(false);
            }}
            onClose={() => {
              setPickerOpen(null);
              setPickerQuery('');
              setPickerAutoSelect(false);
            }}
          />
        )}
```

파일 최상단 import에 `KeyboardEvent` 타입을 추가한다(전역 DOM `KeyboardEvent`와 이름이 겹치므로 별칭 사용 — `CommonCodeSearchModal.tsx`와 동일한 패턴). 기존:

```tsx
import { useEffect, useState } from 'react';
```

다음으로 교체:

```tsx
import { useEffect, useState } from 'react';
import type { KeyboardEvent as ReactKeyboardEvent } from 'react';
```

마지막으로 `CodePickerRowProps`와 `CodePickerRow`를 수정한다. 기존:

```tsx
interface CodePickerRowProps {
  label: string;
  code: string;
  name: string;
  onCodeChange: (v: string) => void;
  onNameChange: (v: string) => void;
  onPick: () => void;
  className?: string;
  invalid?: boolean;
}

const CodePickerRow = ({ label, code, name, onCodeChange, onNameChange, onPick, className, invalid }: CodePickerRowProps) => (
  <div className={[styles.infoRow, className].filter(Boolean).join(' ')}>
    <span className={[styles.infoLabel, invalid ? styles.labelInvalid : ''].filter(Boolean).join(' ')}>{label}</span>
    <div className={[styles.pickerGroup, invalid ? styles.pickerInvalid : ''].filter(Boolean).join(' ')}>
      <input
        className={[styles.infoInput, styles.pickerCode].join(' ')}
        value={code}
        onChange={(e) => onCodeChange(e.target.value)}
        placeholder="코드"
      />
      <input
        className={[styles.infoInput, styles.pickerName].join(' ')}
        value={name}
        onChange={(e) => onNameChange(e.target.value)}
        placeholder="명칭"
      />
      <button
        type="button"
        className={styles.pickerBtn}
        onClick={onPick}
        title={invalid ? '유효하지 않은 코드·명입니다. 검색해서 수정하세요.' : '검색'}
      >
```

다음으로 교체:

```tsx
interface CodePickerRowProps {
  label: string;
  code: string;
  name: string;
  onCodeChange: (v: string) => void;
  onNameChange: (v: string) => void;
  onCommit: () => void;
  onPick: () => void;
  className?: string;
  invalid?: boolean;
}

const CodePickerRow = ({ label, code, name, onCodeChange, onNameChange, onCommit, onPick, className, invalid }: CodePickerRowProps) => {
  const handleKeyDown = (e: ReactKeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      onCommit();
    }
  };
  return (
    <div className={[styles.infoRow, className].filter(Boolean).join(' ')}>
      <span className={[styles.infoLabel, invalid ? styles.labelInvalid : ''].filter(Boolean).join(' ')}>{label}</span>
      <div className={[styles.pickerGroup, invalid ? styles.pickerInvalid : ''].filter(Boolean).join(' ')}>
        <input
          className={[styles.infoInput, styles.pickerCode].join(' ')}
          value={code}
          onChange={(e) => onCodeChange(e.target.value)}
          onBlur={onCommit}
          onKeyDown={handleKeyDown}
          placeholder="코드"
        />
        <input
          className={[styles.infoInput, styles.pickerName].join(' ')}
          value={name}
          onChange={(e) => onNameChange(e.target.value)}
          onBlur={onCommit}
          onKeyDown={handleKeyDown}
          placeholder="명칭"
        />
        <button
          type="button"
          className={styles.pickerBtn}
          onClick={onPick}
          title={invalid ? '유효하지 않은 코드·명입니다. 검색해서 수정하세요.' : '검색'}
        >
          <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" strokeWidth="1.6">
            <circle cx="5.5" cy="5.5" r="4" />
            <line x1="8.7" y1="8.7" x2="11.5" y2="11.5" />
          </svg>
        </button>
      </div>
    </div>
  );
};
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `npx vitest run src/components/members/MemberDetailModal.test.tsx`
Expected: PASS (기존 9개 + 신규 10개 = 19개 전체)

- [ ] **Step 5: 커밋**

```bash
git add src/components/members/MemberDetailModal.tsx src/components/members/MemberDetailModal.test.tsx
git commit -m "feat: 업태/업종 코드·명 편집 시 공통코드 검색으로 세트 동기화"
```

---

### Task 3: 전체 검증 및 PR

**Files:** 없음 (검증만 수행)

- [ ] **Step 1: 전체 테스트 실행**

Run: `npx vitest run`
Expected: 모든 테스트 파일 PASS (Task 1·2에서 추가한 테스트 포함)

- [ ] **Step 2: 빌드 확인**

Run: `npm run build`
Expected: `tsc -b && vite build` 성공, 타입 오류 없음

- [ ] **Step 3: 린트 확인**

Run: `npm run lint`
Expected: 경고 0

- [ ] **Step 4: 브랜치 푸시 + PR 생성**

```bash
git push -u origin feature/business-code-name-sync
gh pr create --base develop --title "feat: 업태/업종 코드·명 세트 동기화" --body "$(cat <<'EOF'
## Summary
- 업태(SERVICE)/업종(ITEM) 코드·명을 자유 입력 대신 공통코드 검색으로만 확정되도록 변경
  - 코드 변경 시 명 자동 비움, 명 변경 시 코드 자동 비움
  - blur/Enter 시 직전 확정 값과 다를 때만 공통코드 검색 모달을 자동으로 열고 검색어를 채움
  - 검색 결과 1건이면 자동 선택 후 모달 닫힘, 0건/2건 이상이면 모달 유지
  - 기존 수동 "찾기" 버튼과 저장 시 유효성 하이라이트는 그대로 유지
- 설계 문서: `docs/superpowers/specs/2026-07-10-service-item-code-name-sync-design.md`

## Test plan
- [x] `CommonCodeSearchModal.test.tsx` 신규 5개 테스트 (initialQuery, autoSelectSingleMatch 0/1/2+건, 기본값 회귀 가드)
- [x] `MemberDetailModal.test.tsx` 신규 10개 테스트 (자동 비움, blur/Enter 트리거, 1건/2건 이상, 선택 없이 닫기, 업종 동일 적용, 수동 버튼 회귀 가드)
- [x] `npx vitest run` 전체 통과
- [x] `npm run build` 통과
- [x] `npm run lint` 통과 (경고 0)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Self-Review 체크리스트 (계획 작성자용, 참고)

- **Spec 커버리지**: 설계 문서의 "핵심 동작" 1~5번, "기존 요소와의 관계" 4개 항목 모두 Task 2의 테스트로 커버됨. "테스트 계획" 절의 두 파일(모달·상세) 모두 Task 1·2에 반영됨.
- **범위 밖 항목**: 코드 필드 숫자 전용 제한, 파트너 코드 등 다른 공통코드 필드 적용은 설계 문서의 "범위 밖"과 동일하게 이 계획에도 포함하지 않음.
