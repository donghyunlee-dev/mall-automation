export type MemberStatus = 'UNREGISTERED' | 'NORMAL' | 'INACTIVE';

export interface MemberListItem {
  memNo: number;
  memId: string;
  memName: string | null;
  groupSno: number;
  groupName: string;
  email: string | null;
  cellPhone: string | null;
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceCode: string | null;
  serviceName: string | null;
  itemCode: string | null;
  itemName: string | null;
  comAddress: string | null;
  partnerCode: string | null;
  partnerName: string | null;
  memberStatus: MemberStatus | null;
  entryDt: string;
  appFl: 'y' | 'n';
  // 중복 회원번호. 백엔드가 세미콜론으로 연결(예: "900102;900103"). 없으면 null
  duplicateMemNo?: string | null;
}

export interface MemberPage {
  content: MemberListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MemberSearchParams {
  searchType: 'MEM_ID' | 'NAME' | 'PARTNER_CODE' | 'PARTNER_NAME' | 'EMAIL' | 'CELL_PHONE';
  keyword: string;
  group: 'ALL' | 'GENERAL' | 'BUSINESS' | 'SKILLED' | 'B2B';
  sortKey: string;
  sortDir: 'asc' | 'desc';
  page: number;
  size: number;
  entryDtFrom: string;
  entryDtTo: string;
  memberStatus?: MemberStatus;
}

export type MemberUploadType = 'LIST' | 'TEMPLATE';

export interface MemberUploadRequest {
  type: MemberUploadType;
  fileName: string;
  applyDeletion: boolean;
  rows: (string | number | null)[][];
}

export interface MemberUploadResponse {
  totalInserted: number;
  totalUpdated: number;
  totalDeleted: number;
  totalFailed: number;
  failedRows: Array<{ rowIndex: number; reason: string }>;
  deletionSkipped: boolean;
  deletionSkippedReason: string | null;
  syncedAt: string;
}

// ── 상세 (GET /api/members/{memNo}) ──

export interface MemberBusinessDetail {
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceCode: string | null;
  serviceName: string | null;
  itemCode: string | null;
  itemName: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
  duplicateMemNo: string | null;
}

export interface MemberPartnerDetail {
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n';
  memberStatus: MemberStatus;
  updatedAt: string;
}

export interface MemberDetail {
  memNo: number;
  memId: string;
  memName: string | null;
  groupSno: number;
  groupName: string;
  appFl: 'y' | 'n';
  appDt: string | null;
  email: string | null;
  cellPhone: string | null;
  phone: string | null;
  address: string | null;
  entryDt: string;
  lastLoginDt: string | null;
  entryPath: string | null;
  maillingFl: 'y' | 'n';
  smsFl: 'y' | 'n';
  nickName: string | null;
  sexFl: 'm' | 'w' | null;
  birthDt: string | null;
  marriFl: 'y' | 'n' | null;
  marriDate: string | null;
  job: string | null;
  interest: string | null;
  loginCount: number;
  saleAmount: number;
  mileage: number;
  deposit: number;
  recommId: string | null;
  ex1: string | null;
  ex2: string | null;
  ex3: string | null;
  ex4: string | null;
  ex5: string | null;
  ex6: string | null;
  memo: string | null;
  business: MemberBusinessDetail | null;
  partner: MemberPartnerDetail | null;
}

// ── 변경 이력 ──

export type MemberHistoryKind = 'BUSINESS' | 'PARTNER';

export interface MemberHistoryEntry {
  changedAt: string;
  field: string;
  before: string | null;
  after: string | null;
  editor: string;
}

// ── 파트너 수정 (PUT /api/members/{memNo}/partner) ──

export interface UpdatePartnerRequest {
  memberStatus: MemberStatus | null;
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n' | null;
  bizLicenseFile: string | null;
}

export interface MemberPartnerResponse {
  memberStatus: MemberStatus;
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n';
  bizLicenseFile: string | null;
  updatedAt: string;
}

// ── 사업자 정보 수정 (PUT /api/members/{memNo}/business) ──

export interface UpdateBusinessRequest {
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceName: string | null;
  serviceCode: string | null;
  itemName: string | null;
  itemCode: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
}

export interface UpdateBusinessResponse {
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceName: string | null;
  serviceCode: string | null;
  itemName: string | null;
  itemCode: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
  updatedAt: string;
}

// ── 변경 이력 (GET /api/members/{memNo}/business/logs, /partner/logs) ──

export interface MemberBusinessLogItem {
  id: number;
  memNo: number;
  company: string | null;
  businessNo: string | null;
  ceoName: string | null;
  serviceName: string | null;
  serviceCode: string | null;
  itemName: string | null;
  itemCode: string | null;
  comAddress: string | null;
  bizLicenseFile: string | null;
  loggedAt: string;
  loggedBy: string | null;
}

export interface MemberPartnerLogItem {
  id: number;
  memNo: number;
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: string | null;
  memberStatus: string | null;
  loggedAt: string;
  loggedBy: string | null;
}
