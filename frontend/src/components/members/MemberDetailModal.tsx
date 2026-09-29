import { useEffect, useState } from 'react';
import type { KeyboardEvent as ReactKeyboardEvent } from 'react';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';
import useMemberDetail from '../../hooks/useMemberDetail';
import { getMemberGroupInfo } from '../../utils/memberGroup';
import { GODO_ADMIN_MEMBER_URL } from '../../utils/constants';
import MemberHistoryModal from './MemberHistoryModal';
import CommonCodeSearchModal from '../modals/CommonCodeSearchModal';
import { getCommonCodes } from '../../api/commonCodes';
import type { CommonCodeGroup, CommonCodeItem } from '../../types/commonCode';
import modalStyles from '../modals/Modal.module.scss';
import styles from './MemberDetailModal.module.scss';
import type {
  MemberDetail,
  MemberBusinessDetail,
  MemberPartnerDetail,
  MemberHistoryKind,
  MemberStatus,
  UpdatePartnerRequest,
  UpdateBusinessRequest,
} from '../../types/member';

interface Props {
  memNo: number | null;
  onClose: () => void;
  onToast: (message: string, type: 'success' | 'error') => void;
  onSaved?: () => void;
  onNavigate?: (memNo: number) => void;
  suppressDuplicate?: boolean;
  serviceCodes?: CommonCodeItem[];
  itemCodes?: CommonCodeItem[];
}

const dash = (v: string | null | undefined): string => (v && v.trim() ? v : '—');

function parseAddress(raw: string | null | undefined): [string, string] {
  if (!raw?.trim()) return ['', ''];
  const m = raw.match(/^(\d{4,6})\s+(.+)$/);
  return m ? [m[1], m[2]] : ['', raw];
}

function formatAddressDisplay(raw: string | null | undefined): string {
  if (!raw?.trim()) return '—';
  const [zip, rest] = parseAddress(raw);
  return zip ? `(${zip}) ${rest}` : raw;
}

const MemberDetailModal = ({ memNo, onClose, onToast, onSaved, onNavigate, suppressDuplicate, serviceCodes, itemCodes }: Props) => {
  const { member, loading, error, notFound, updatePartner, partnerUpdating, updateBusiness, businessUpdating } = useMemberDetail(
    memNo,
    (msg) => onToast(msg, 'error'),
  );
  const [historyKind, setHistoryKind] = useState<MemberHistoryKind | null>(null);
  const [isBizDirty, setIsBizDirty] = useState(false);
  const [isPartnerDirty, setIsPartnerDirty] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const isDirty = isBizDirty || isPartnerDirty;

  useLockBodyScroll(memNo != null);

  // memNo 변경 시 편집 상태 초기화 — 렌더 중 상태 조정 (React 권장 패턴)
  const [prevMemNo, setPrevMemNo] = useState(memNo);
  if (prevMemNo !== memNo) {
    setPrevMemNo(memNo);
    setIsBizDirty(false);
    setIsPartnerDirty(false);
    setShowCloseConfirm(false);
  }

  // 404 → 모달 닫기 + 안내
  useEffect(() => {
    if (notFound) {
      onToast('회원을 찾을 수 없어요.', 'error');
      onClose();
    }
  }, [notFound, onToast, onClose]);

  // ESC 닫기 — 미저장 변경이 있으면 확인 다이얼로그를 먼저 띄운다
  // (이력·공통코드 모달이 위에 떠 있으면 그쪽 캡처 핸들러가 전파를 막아 여기까지 오지 않음)
  useEffect(() => {
    if (memNo == null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (showCloseConfirm) {
        setShowCloseConfirm(false);
        return;
      }
      if (isDirty) {
        setShowCloseConfirm(true);
        return;
      }
      onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [memNo, showCloseConfirm, isDirty, onClose]);

  if (memNo == null) return null;

  const handleClose = () => {
    if (isDirty) {
      setShowCloseConfirm(true);
    } else {
      onClose();
    }
  };

  const openGodoAdmin = () => {
    window.open(`${GODO_ADMIN_MEMBER_URL}${memNo}`, '_blank', 'noopener,noreferrer');
  };

  const grade = member ? getMemberGroupInfo(member.groupSno) : null;
  // 사업자 정보·거래처 정보는 등급·데이터 유무와 무관하게 항상 표시
  // (데이터가 없으면 '—' 표기, 거래처 신규 등록은 빈 폼으로 처리)
  const showBiz = !!member;
  const showPartner = !!member;
  const hasDuplicate = !!member?.business?.duplicateMemNo && !suppressDuplicate;
  // 사업자번호가 중복되는 회원번호 목록 (백엔드가 세미콜론으로 연결)
  const duplicateMemNos =
    hasDuplicate && member?.business?.duplicateMemNo
      ? member.business.duplicateMemNo.split(';').map((s) => Number(s.trim())).filter(Boolean)
      : [];

  return (
    <>
    <div className={modalStyles.modalBg} onClick={handleClose}>
      <div
        className={[modalStyles.modal, styles.detailModal].join(' ')}
        onClick={(e) => e.stopPropagation()}
      >
        {loading && !member ? (
          <div className={styles.loadingBox}>
            <span className={styles.spinner} /> 불러오는 중...
          </div>
        ) : error && !member ? (
          <div className={modalStyles.errBox}>{error}</div>
        ) : member ? (
          <>
            {/* 헤더 */}
            <div className={styles.detailHead}>
              <span className={styles.detailName}>{member.memId}</span>
              {grade && (
                <span
                  className={styles.badge}
                  style={{ background: grade.badgeBg, color: grade.badgeText }}
                >
                  {member.groupName || grade.label}
                </span>
              )}
              <div className={styles.detailActions}>
                <button type="button" className={styles.btnGodo} onClick={openGodoAdmin}>
                  고도몰 어드민 열기 ↗
                </button>
                <button
                  type="button"
                  className={styles.modalClose}
                  onClick={handleClose}
                  aria-label="닫기"
                >
                  ×
                </button>
              </div>
            </div>

            {/* 중복 회원 안내 배너 */}
            {hasDuplicate && (
              <div className={styles.dupBanner}>
                <span className={styles.dupText}>⚠ 중복 가능성이 있는 회원이 있습니다</span>
              </div>
            )}

            <div className={styles.detailBody}>
            {/* 기본 정보 */}
            <div className={styles.infoCard} style={{ marginBottom: 12 }}>
              <div className={styles.cardHeader}>
                <span className={styles.cardTitle}>기본 정보</span>
              </div>
              <div className={[styles.infoBody, styles.cols2].join(' ')}>
                <Row label="회원번호" value={String(member.memNo)} />
                <Row label="아이디" value={member.memId} />
                <Row label="이름" value={dash(member.memName)} />
                <Row label="휴대폰" value={dash(member.cellPhone)} />
                <Row label="이메일" value={dash(member.email)} />
                <Row label="전화" value={dash(member.phone)} />
                <Row label="주소" value={formatAddressDisplay(member.address)} />
                <Row label="가입일 / 경로" value={`${member.entryDt} / ${dash(member.entryPath)}`} />
                <Row label="최근 접속" value={dash(member.lastLoginDt)} />
              </div>
            </div>

            {/* 조건부: 사업자 정보 + 에쓰푸드 거래처 정보 */}
            <div className={styles.condGrid}>
              {showBiz && (
                <BusinessCard
                  key={`biz-${member.memNo}`}
                  business={member.business}
                  duplicateMemNos={duplicateMemNos}
                  onNavigate={onNavigate}
                  saving={businessUpdating}
                  onSave={updateBusiness}
                  onToast={onToast}
                  onSaved={onSaved}
                  onHistory={() => setHistoryKind('BUSINESS')}
                  onDirtyChange={setIsBizDirty}
                  serviceCodes={serviceCodes}
                  itemCodes={itemCodes}
                />
              )}

              {showPartner && (
                <PartnerCard
                  key={member.memNo}
                  member={member}
                  saving={partnerUpdating}
                  onSave={updatePartner}
                  onToast={onToast}
                  onSaved={onSaved}
                  onHistory={() => setHistoryKind('PARTNER')}
                  onDirtyChange={setIsPartnerDirty}
                />
              )}
            </div>
            </div>
          </>
        ) : null}
      </div>

      {member && (
        <MemberHistoryModal
          kind={historyKind}
          memNo={member.memNo}
          onClose={() => setHistoryKind(null)}
          currentBusiness={member.business}
          currentPartner={member.partner}
        />
      )}
    </div>
    {showCloseConfirm && (
      <div className={modalStyles.modalBg} onClick={() => setShowCloseConfirm(false)}>
        <div className={modalStyles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>수정사항이 저장되지 않았어요</h3>
          <p className={modalStyles.confirmTextBlock}>저장하지 않고 닫으시겠어요?</p>
          <div className={modalStyles.modalFooter}>
            <button type="button" className={modalStyles.btnCancel} onClick={() => setShowCloseConfirm(false)}>
              계속 편집
            </button>
            <button type="button" className={modalStyles.btnDelete} onClick={() => { setShowCloseConfirm(false); onClose(); }}>
              닫기
            </button>
          </div>
        </div>
      </div>
    )}
    </>
  );
};

// ── 사업자 정보 (편집 가능, API 미연동 — 저장은 화면 임시 반영) ──

interface BusinessCardProps {
  business: MemberBusinessDetail | null;
  duplicateMemNos: number[];
  onNavigate?: (memNo: number) => void;
  saving: boolean;
  onSave: (req: UpdateBusinessRequest) => Promise<boolean>;
  onToast: (message: string, type: 'success' | 'error') => void;
  onSaved?: () => void;
  onHistory: () => void;
  onDirtyChange?: (dirty: boolean) => void;
  serviceCodes?: CommonCodeItem[];
  itemCodes?: CommonCodeItem[];
}

const BusinessCard = ({ business, duplicateMemNos, onNavigate, saving, onSave, onToast, onSaved, onHistory, onDirtyChange, serviceCodes: preloadedServiceCodes, itemCodes: preloadedItemCodes }: BusinessCardProps) => {
  const [company, setCompany] = useState(business?.company ?? '');
  const [businessNo, setBusinessNo] = useState(business?.businessNo ?? '');
  const [ceoName, setCeoName] = useState(business?.ceoName ?? '');
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
  const [comZip, setComZip] = useState(() => parseAddress(business?.comAddress)[0]);
  const [comAddr, setComAddr] = useState(() => parseAddress(business?.comAddress)[1]);

  // 저장 후 서버 값으로 갱신된 주소를 재동기화 — 렌더 중 상태 조정
  const [prevComAddress, setPrevComAddress] = useState(business?.comAddress);
  if (prevComAddress !== business?.comAddress) {
    setPrevComAddress(business?.comAddress);
    const [zip, addr] = parseAddress(business?.comAddress);
    setComZip(zip);
    setComAddr(addr);
  }

  const markDirty = () => onDirtyChange?.(true);

  // 공통코드 목록 — 부모에서 preload된 경우 즉시 사용, 없으면 자체 fetch
  const [serviceCodes, setServiceCodes] = useState<CommonCodeItem[]>(preloadedServiceCodes ?? []);
  const [itemCodes, setItemCodes] = useState<CommonCodeItem[]>(preloadedItemCodes ?? []);
  const [codesLoaded, setCodesLoaded] = useState(
    preloadedServiceCodes != null && preloadedItemCodes != null,
  );
  useEffect(() => {
    if (preloadedServiceCodes != null && preloadedItemCodes != null) return;
    let alive = true;
    Promise.all([getCommonCodes('SERVICE'), getCommonCodes('ITEM')])
      .then(([svc, itm]) => {
        if (!alive) return;
        setServiceCodes(svc);
        setItemCodes(itm);
      })
      .catch(() => undefined)
      .finally(() => {
        if (alive) setCodesLoaded(true);
      });
    return () => {
      alive = false;
    };
  }, [preloadedServiceCodes, preloadedItemCodes]);

  // 사업자 정보가 있는 회원만 검증 (개인회원의 빈 업태·업종은 제외)
  const hasBizIdentity = !!(businessNo.trim() || company.trim());
  const isValidPair = (list: CommonCodeItem[], code: string, name: string): boolean =>
    !!code.trim() && !!name.trim() && list.some((c) => c.code === code.trim() && c.codeName === name.trim());
  const serviceInvalid = codesLoaded && hasBizIdentity && !isValidPair(serviceCodes, serviceCode, serviceName);
  const itemInvalid = codesLoaded && hasBizIdentity && !isValidPair(itemCodes, itemCode, itemName);

  const handleSave = async () => {
    const ok = await onSave({
      company: company.trim() || null,
      businessNo: businessNo.trim() || null,
      ceoName: ceoName.trim() || null,
      serviceName: serviceName.trim() || null,
      serviceCode: serviceCode.trim() || null,
      itemName: itemName.trim() || null,
      itemCode: itemCode.trim() || null,
      comAddress: [comZip.trim(), comAddr.trim()].filter(Boolean).join(' ') || null,
      bizLicenseFile: null,
    });
    if (ok) {
      onToast('사업자 정보를 저장했어요.', 'success');
      onDirtyChange?.(false);
      onSaved?.();
      setConfirmedService({ code: serviceCode.trim(), name: serviceName.trim() });
      setConfirmedItem({ code: itemCode.trim(), name: itemName.trim() });
    }
  };

  // 코드/명 blur·Enter 커밋 — 직전 확정 값과 실제로 다를 때만 검색 모달을 연다
  const commitCodeName = (
    group: CommonCodeGroup,
    codeVal: string,
    nameVal: string,
    confirmed: { code: string; name: string },
  ) => {
    // 검색 모달이 이미 열려 있으면(같은 필드의 재-blur든, 다른 필드로 포커스가 이동한 경우든)
    // 진행 중인 검색을 다른 필드의 자동 트리거로 덮어쓰지 않는다.
    if (pickerOpen !== null) return;
    // 확정 값이 코드/명 둘 다 채워진 완전한 세트일 때만 "값이 그대로면 재검색 생략"을 적용한다.
    // 확정 값이 불완전(예: 명은 있는데 코드가 없는 레거시 데이터)하면, 값이 그대로여도 항상 검색을 띄운다.
    const confirmedComplete = !!confirmed.code && !!confirmed.name;
    if (confirmedComplete && codeVal === confirmed.code && nameVal === confirmed.name) return;
    const query = codeVal || nameVal;
    if (!query) return;
    setPickerQuery(query);
    setPickerAutoSelect(true);
    setPickerOpen(group);
  };
  const commitService = () => commitCodeName('SERVICE', serviceCode.trim(), serviceName.trim(), confirmedService);
  const commitItem = () => commitCodeName('ITEM', itemCode.trim(), itemName.trim(), confirmedItem);

  return (
    <div className={styles.infoCard}>
      <div className={styles.cardHeader}>
        <span className={styles.cardTitle}>사업자 정보</span>
        {duplicateMemNos.map((no) => (
          <button
            key={no}
            type="button"
            className={styles.dupChip}
            onClick={() => onNavigate?.(no)}
            title="중복 회원 상세 보기"
          >
            {no}
          </button>
        ))}
        <div className={styles.hdRight}>
          <button type="button" className={styles.btnGhost} onClick={onHistory}>
            수정이력
          </button>
          <button type="button" className={styles.btnSave} onClick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
      <div className={[styles.infoBody, styles.cols2].join(' ')}>
        <EditRow label="상호" value={company} onChange={(v) => { setCompany(v); markDirty(); }} />
        <EditRow label="사업자번호" value={businessNo} onChange={(v) => { setBusinessNo(v); markDirty(); }} />
        <EditRow label="대표자명" value={ceoName} onChange={(v) => { setCeoName(v); markDirty(); }} />
        <div className={[styles.infoRow, styles.fullRow].join(' ')}>
          <span className={styles.infoLabel}>사업장 주소</span>
          <div className={styles.addrGroup}>
            <input
              className={[styles.infoInput, styles.addrZip].join(' ')}
              value={comZip}
              onChange={(e) => { setComZip(e.target.value); markDirty(); }}
              placeholder="우편번호"
              maxLength={5}
            />
            <input
              className={[styles.infoInput, styles.addrInput].join(' ')}
              value={comAddr}
              onChange={(e) => { setComAddr(e.target.value); markDirty(); }}
              placeholder="주소"
            />
          </div>
        </div>
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
      </div>
    </div>
  );
};

interface PartnerCardProps {
  member: MemberDetail;
  saving: boolean;
  onSave: (req: UpdatePartnerRequest) => Promise<boolean>;
  onToast: (message: string, type: 'success' | 'error') => void;
  onSaved?: () => void;
  onHistory: () => void;
  onDirtyChange?: (dirty: boolean) => void;
}

const PartnerCard = ({ member, saving, onSave, onToast, onSaved, onHistory, onDirtyChange }: PartnerCardProps) => {
  const p: MemberPartnerDetail | null = member.partner;
  const [pCode, setPCode] = useState(p?.partnerCode ?? '');
  const [pName, setPName] = useState(p?.partnerName ?? '');
  const [lifecycle, setLifecycle] = useState<MemberStatus>(
    p?.memberStatus === 'INACTIVE'
      ? 'INACTIVE'
      : p?.memberStatus === 'NORMAL'
        ? 'NORMAL'
        : 'UNREGISTERED',
  );

  const markDirty = () => onDirtyChange?.(true);
  const registered = lifecycle !== 'UNREGISTERED';

  const handleSave = async () => {
    if (registered && !pCode.trim()) {
      onToast('거래처 코드를 입력해 주세요.', 'error');
      return;
    }
    const ok = await onSave({
      memberStatus: lifecycle,
      partnerCode: pCode.trim() || null,
      partnerName: pName.trim() || null,
      taxInvoiceFl: p?.taxInvoiceFl ?? null,
      bizLicenseFile: member.business?.bizLicenseFile ?? null,
    });
    if (ok) {
      onToast('거래처 정보를 저장했어요.', 'success');
      onDirtyChange?.(false);
      onSaved?.();
    }
  };

  return (
    <div className={styles.infoCard}>
      <div className={styles.cardHeader}>
        <span className={styles.cardTitle}>에쓰푸드 거래처 정보</span>
        <div className={styles.hdRight}>
          <button type="button" className={styles.btnGhost} onClick={onHistory}>
            수정이력
          </button>
          <button type="button" className={styles.btnSave} onClick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
      <div className={styles.infoBody}>
        {/* 거래처 코드·명은 백엔드에서 자동 채번 → 미등록일 때도 항상 표시 */}
        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>거래처 코드</span>
          <input
            className={[styles.infoInput, styles.codeInput].join(' ')}
            value={pCode}
            onChange={(e) => { setPCode(e.target.value); markDirty(); }}
            placeholder=""
          />
        </div>
        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>거래처 명</span>
          <input
            className={[styles.infoInput, styles.nameInput].join(' ')}
            value={pName}
            onChange={(e) => { setPName(e.target.value); markDirty(); }}
            placeholder=""
          />
        </div>
        {/* 거래처 상태 — 미등록 / 등록 / 불용 통합 */}
        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>거래처 상태</span>
          <div className={styles.typeSel}>
            <button
              type="button"
              className={[styles.typeOpt, styles.typeOptPos, lifecycle === 'NORMAL' ? styles.on : ''].join(' ')}
              onClick={() => { setLifecycle('NORMAL'); markDirty(); }}
            >
              등록
            </button>
            <button
              type="button"
              className={[styles.typeOpt, styles.typeOptNeu, lifecycle === 'UNREGISTERED' ? styles.on : ''].join(' ')}
              onClick={() => { setLifecycle('UNREGISTERED'); markDirty(); }}
            >
              미등록
            </button>
            <button
              type="button"
              className={[styles.typeOpt, styles.typeOptNeg, lifecycle === 'INACTIVE' ? styles.on : ''].join(' ')}
              onClick={() => { setLifecycle('INACTIVE'); markDirty(); }}
            >
              불용
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

interface RowProps {
  label: string;
  value: string;
}

const Row = ({ label, value }: RowProps) => (
  <div className={styles.infoRow}>
    <span className={styles.infoLabel}>{label}</span>
    <span className={[styles.infoValue, value === '—' ? styles.empty : ''].join(' ')}>{value}</span>
  </div>
);

interface EditRowProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  className?: string;
  inputClassName?: string;
}

const EditRow = ({ label, value, onChange, className, inputClassName }: EditRowProps) => (
  <div className={[styles.infoRow, className].filter(Boolean).join(' ')}>
    <span className={styles.infoLabel}>{label}</span>
    <input
      className={[styles.infoInput, inputClassName].filter(Boolean).join(' ')}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  </div>
);

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

export default MemberDetailModal;
