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
  // 현재 레코드 값 — 백엔드가 변경 전(before) 값을 로깅하므로,
  // 가장 최근 변경의 결과 값은 현재 레코드에서 가져온다
  currentBusiness?: MemberBusinessDetail | null;
  currentPartner?: MemberPartnerDetail | null;
}

const TITLE: Record<MemberHistoryKind, string> = {
  BUSINESS: '사업자 정보 수정 이력',
  PARTNER: '거래처 정보 수정 이력',
};

type FieldConfig = { key: string; label: string; format?: (v: string | null) => string };

function formatComAddress(v: string | null): string {
  if (!v?.trim()) return '—';
  const m = v.match(/^(\d{4,6})\s+(.+)$/);
  return m ? `(${m[1]}) ${m[2]}` : v;
}

const BIZ_FIELDS: FieldConfig[] = [
  { key: 'company', label: '상호' },
  { key: 'businessNo', label: '사업자번호' },
  { key: 'ceoName', label: '대표자명' },
  { key: 'comAddress', label: '사업장 주소', format: formatComAddress },
  { key: 'serviceName', label: '업태' },
  { key: 'itemName', label: '업종' },
];

const MEMBER_STATUS_LABEL: Record<string, string> = {
  NORMAL: '등록',
  INACTIVE: '불용',
  UNREGISTERED: '미등록',
};

const PARTNER_FIELDS: FieldConfig[] = [
  { key: 'partnerCode', label: '거래처 코드' },
  { key: 'partnerName', label: '거래처 명' },
  { key: 'memberStatus', label: '거래처 상태', format: (v) => (v ? (MEMBER_STATUS_LABEL[v] ?? v) : '—') },
];

type BizValueShape = Pick<
  MemberBusinessDetail,
  'company' | 'businessNo' | 'ceoName' | 'serviceCode' | 'serviceName' | 'itemCode' | 'itemName' | 'comAddress'
>;

// 업태·업종은 "(코드) 이름" 형태로 표시
function codeName(code: string | null, name: string | null): string | null {
  if (!name?.trim()) return null;
  return code?.trim() ? `(${code}) ${name}` : name;
}

function bizValues(e: BizValueShape): Record<string, string | null> {
  return {
    company: e.company,
    businessNo: e.businessNo,
    ceoName: e.ceoName,
    comAddress: e.comAddress,
    serviceName: codeName(e.serviceCode, e.serviceName),
    itemName: codeName(e.itemCode, e.itemName),
  };
}

type PartnerValueShape = {
  partnerCode: string | null;
  partnerName: string | null;
  memberStatus: string | null;
};

function partnerValues(e: PartnerValueShape): Record<string, string | null> {
  return {
    partnerCode: e.partnerCode,
    partnerName: e.partnerName,
    memberStatus: e.memberStatus,
  };
}

interface DisplayRow {
  loggedAt: string;
  values: Record<string, string | null>;
  changedKeys: Set<string>;
}

// 백엔드는 변경 '직전' 값을 loggedAt(변경 시각)과 함께 기록한다.
// 따라서 loggedAt[i]가 만들어낸 결과 값 = 다음 로그의 before 값(마지막 변경은 현재 레코드).
// 각 행이 "변경 시각 + 그 변경으로 바뀐 값"이 되도록 재정렬한다.
function toRows<T extends { loggedAt: string }>(
  logs: T[],
  getValues: (e: T) => Record<string, string | null>,
  current: Record<string, string | null> | null,
): DisplayRow[] {
  // loggedAt이 없는(null/undefined) 레코드가 섞여 있어도 정렬이 실패하지 않도록 방어
  const sorted = [...logs].sort((a, b) => (a.loggedAt ?? '').localeCompare(b.loggedAt ?? ''));
  if (sorted.length === 0) return [];

  // 초깃값: 가장 오래된 변경 직전 값. 이를 만든 변경 시각은 남아있지 않으므로
  // 변경 일시 없이('') 기준 행으로 맨 위에 표시한다.
  const rows: DisplayRow[] = [
    { loggedAt: '', values: getValues(sorted[0]), changedKeys: new Set() },
  ];

  sorted.forEach((entry, i) => {
    const before = getValues(entry);
    const after = i < sorted.length - 1 ? getValues(sorted[i + 1]) : (current ?? before);
    const changedKeys = new Set(
      Object.keys(after).filter((k) => (after[k] ?? null) !== (before[k] ?? null)),
    );
    rows.push({ loggedAt: entry.loggedAt, values: after, changedKeys });
  });

  return rows;
}

const MemberHistoryModal = ({
  kind,
  memNo,
  onClose,
  currentBusiness,
  currentPartner,
}: Props) => {
  // 이력은 모달이 실제로 열릴 때(kind 지정 시)만 조회한다 — 상세보기를 여는 것만으로는 호출하지 않는다.
  const [businessLogs, setBusinessLogs] = useState<MemberBusinessLogItem[] | null>(null);
  const [partnerLogs, setPartnerLogs] = useState<MemberPartnerLogItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 회원이 바뀌면 캐시 무효화, 모달을 다시 열면 이전 오류 초기화 — 렌더 중 상태 조정
  const [prevMemNo, setPrevMemNo] = useState(memNo);
  if (prevMemNo !== memNo) {
    setPrevMemNo(memNo);
    setBusinessLogs(null);
    setPartnerLogs(null);
    setError(null);
  }
  const [prevKind, setPrevKind] = useState(kind);
  if (prevKind !== kind) {
    setPrevKind(kind);
    setError(null);
  }

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
      if (businessLogs) return;
      getMemberBusinessLogs(memNo)
        .then((d) => { if (alive) setBusinessLogs(d); })
        .catch(() => { if (alive) setError('이력을 불러오지 못했어요.'); });
    } else {
      if (partnerLogs) return;
      getMemberPartnerLogs(memNo)
        .then((d) => { if (alive) setPartnerLogs(d); })
        .catch(() => { if (alive) setError('이력을 불러오지 못했어요.'); });
    }
    return () => { alive = false; };
  }, [kind, memNo, businessLogs, partnerLogs]);

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
  const loading = kind != null && activeLogs == null && !error;

  if (kind == null) return null;

  const fields = kind === 'BUSINESS' ? BIZ_FIELDS : PARTNER_FIELDS;

  return (
    <div className={modalStyles.modalBg} onClick={onClose}>
      <div
        className={[modalStyles.modal, modalStyles.modalWide, styles.historyModal, kind === 'PARTNER' ? styles.historyModalNarrow : ''].filter(Boolean).join(' ')}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.head}>
          <h3>{TITLE[kind]}</h3>
          <button type="button" className={styles.close} onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>수정 일시</th>
                {fields.map((f) => <th key={f.key}>{f.label}</th>)}
              </tr>
            </thead>
            {!loading && !error && rows.length > 0 && (
              <tbody>
                {rows.map((row, i) => (
                  <tr key={i}>
                    <td className={styles.dt}>
                      {row.loggedAt ? row.loggedAt.slice(0, 16).replace('T', ' ') : '—'}
                    </td>
                    {fields.map((f) => (
                      <td key={f.key} className={row.changedKeys.has(f.key) ? styles.changedCell : ''}>
                        {f.format ? f.format(row.values[f.key] ?? null) : (row.values[f.key] ?? '—')}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            )}
          </table>
          {(loading || !!error || rows.length === 0) && (
            <div className={styles.stateMsg}>
              {loading ? '불러오는 중...' : error ? error : '수정 이력이 없습니다.'}
            </div>
          )}
        </div>

        <div className={modalStyles.modalFooter}>
          <button type="button" className={modalStyles.btnCancel} onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default MemberHistoryModal;
