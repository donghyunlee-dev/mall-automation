import express from 'express';
import cors from 'cors';

const app = express();
const PORT = process.env.PORT || 8080;

// Middleware
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', 'http://localhost:5173');
  res.header('Access-Control-Allow-Credentials', 'true');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});
app.use(express.json());

// Session store
const sessions = new Map();
const goods = new Map();
const verificationKeys = new Map();

// Initialize sample goods
let goodsId = 1;
[
  { godoGoodsCode: '10000001', erpGoodsCode: 'ERP-0001', goodsName: 'Sample Goods 1', onlyMallYn: 'N', soldOutYn: 'N', availableQty: 20, safetyRate: 50, status: 'NORMAL' },
  { godoGoodsCode: '10000002', erpGoodsCode: 'ERP-0002', goodsName: 'Sample Goods 2', onlyMallYn: 'Y', soldOutYn: 'Y', availableQty: 0, safetyRate: 50, status: 'UNVERIFIED' },
].forEach(item => {
  goods.set(goodsId, {
    id: goodsId,
    ...item,
    godoSyncYn: 'Y',
    erpSyncYn: 'Y',
    message: null,
    createdId: 'admin@s-food.com',
    createdAt: new Date('2026-04-01T09:00:00Z').toISOString(),
    updatedId: null,
    updatedAt: null,
  });
  goodsId++;
});

// Helper
const getCookie = (req, name) => {
  const cookies = req.headers.cookie?.split(';') ?? [];
  for (const cookie of cookies) {
    const [key, ...rest] = cookie.trim().split('=');
    if (key === name) return rest.join('=');
  }
  return undefined;
};

const checkAuth = (req, res) => {
  const sessionId = getCookie(req, 'sessionId');
  if (!sessionId || !sessions.has(sessionId)) {
    res.status(401).json({ message: 'Unauthorized' });
    return null;
  }
  return sessions.get(sessionId);
};

// Auth APIs
app.post('/api/auth/email-verifications', (req, res) => {
  const { email } = req.body;
  if (!email || !email.endsWith('@s-food.com')) {
    return res.status(400).json({ message: 'Invalid email' });
  }
  const verificationKey = 'verify-' + Date.now();
  const code = '123456';
  verificationKeys.set(verificationKey, { email, attempts: 0, code });
  console.log(`✉️ [OTP] Email: ${email}, Code: ${code}`);
  res.status(201).json({
    verificationKey: verificationKey,
    expiresInSeconds: 300,
    message: 'Verification code sent',
    code: code,
  });
});

app.post('/api/auth/email-verifications/confirm', (req, res) => {
  const { verificationKey, email, code } = req.body;
  const verification = verificationKeys.get(verificationKey);

  if (!verification || verification.email !== email) {
    return res.status(400).json({ message: 'Invalid verification key' });
  }

  if (verification.attempts >= 5) {
    verificationKeys.delete(verificationKey);
    return res.status(429).json({ message: 'Too many attempts' });
  }

  if (verification.code !== code) {
    verification.attempts++;
    return res.status(401).json({ message: 'Invalid code' });
  }

  const sessionId = 'session-' + Date.now();
  sessions.set(sessionId, { email });
  verificationKeys.delete(verificationKey);

  res.setHeader('Set-Cookie', `sessionId=${sessionId}; Path=/; HttpOnly`);
  res.status(200).json({ authenticated: true, email });
});

app.get('/api/auth/session', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json({ email: auth.email });
});

app.post('/api/auth/logout', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const sessionId = getCookie(req, 'sessionId');
  sessions.delete(sessionId);
  res.status(204).send();
});

// Goods APIs
app.get('/api/goods', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const { soldOutYn, onlyMallYn, status, keyword } = req.query;
  let items = Array.from(goods.values()).filter(g => g.status !== 'HIDE');

  if (soldOutYn) items = items.filter(g => g.soldOutYn === soldOutYn);
  if (onlyMallYn) items = items.filter(g => g.onlyMallYn === onlyMallYn);
  if (status) items = items.filter(g => g.status === status);
  if (keyword) items = items.filter(g => g.goodsName.includes(keyword) || g.godoGoodsCode.includes(keyword));

  res.status(200).json({ items });
});

app.get('/api/goods/:id', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const item = goods.get(parseInt(req.params.id));
  if (!item) return res.status(404).json({ message: 'Not found' });
  res.status(200).json(item);
});

app.post('/api/goods', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const items = req.body;
  const results = items.map(item => {
    const id = goodsId++;
    goods.set(id, {
      id,
      ...item,
      status: 'UNVERIFIED',
      message: null,
      godoSyncYn: 'Y',
      erpSyncYn: 'Y',
      soldOutYn: 'N',
      availableQty: 0,
      safetyRate: 50,
      createdId: auth.email,
      createdAt: new Date().toISOString(),
      updatedId: null,
      updatedAt: null,
    });
    return { id, godoGoodsCode: item.godoGoodsCode, erpGoodsCode: item.erpGoodsCode, status: 'UNVERIFIED', message: null };
  });
  res.status(201).json({ items: results });
});

app.put('/api/goods/:id', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const id = parseInt(req.params.id);
  const item = goods.get(id);
  if (!item) return res.status(404).json({ message: 'Not found' });
  Object.assign(item, req.body, { updatedId: auth.email, updatedAt: new Date().toISOString() });
  res.status(200).json(item);
});

app.delete('/api/goods/:id', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const id = parseInt(req.params.id);
  if (!goods.has(id)) return res.status(404).json({ message: 'Not found' });
  goods.delete(id);
  res.status(204).send();
});

// Batch schedule APIs
const batchSchedule = {
  jobName: 'product-sync',
  enabled: true,
  fixedDelaySeconds: 600,
  running: false,
  lastStartedAt: null,
  lastEndedAt: null,
  lastStatus: null,
  lastMessage: null,
};

app.get('/api/admin/batch-schedules/:jobName', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json(batchSchedule);
});

app.put('/api/admin/batch-schedules/:jobName', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const { enabled, fixedDelaySeconds } = req.body;
  if (typeof enabled === 'boolean') batchSchedule.enabled = enabled;
  if (typeof fixedDelaySeconds === 'number') batchSchedule.fixedDelaySeconds = fixedDelaySeconds;
  res.status(200).json(batchSchedule);
});

app.post('/api/admin/batch-schedules/:jobName/run', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  batchSchedule.lastStartedAt = new Date().toISOString();
  batchSchedule.lastEndedAt = new Date().toISOString();
  batchSchedule.lastStatus = 'SUCCESS';
  batchSchedule.lastMessage = null;
  res.status(200).json({ triggered: true, status: 'STARTED' });
});

// ════════════ Members ════════════
const GROUP_NAMES = {
  1: '일반회원', 2: '사업자회원_화이트', 3: '사업자회원_로제',
  5: '사업자회원_레드', 6: '사업자회원_VIP', 9: 'B2B 거래처',
};
const FAMILY = [
  ['김', 'kim'], ['이', 'lee'], ['박', 'park'], ['최', 'choi'], ['정', 'jung'],
  ['강', 'kang'], ['조', 'cho'], ['윤', 'yoon'], ['장', 'jang'], ['임', 'lim'],
  ['한', 'han'], ['오', 'oh'], ['서', 'seo'], ['신', 'shin'], ['권', 'kwon'],
  ['황', 'hwang'], ['안', 'ahn'], ['송', 'song'], ['전', 'jeon'], ['홍', 'hong'],
];
const GIVEN = ['민준', '서연', '도윤', '지우', '예준', '하은', '시우', '지호', '주원', '지민',
  '현우', '수빈', '준서', '지윤', '건우', '채원', '우진', '다은', '선우', '유진'];
const SERVICES = ['도매', '소매', '제조', '유통', '음식점', '프랜차이즈'];
const ITEMS = ['식품', '농산물', '수산물', '축산물', '가공식품', '음료'];
const PATHS = ['pc', 'mobile', 'app'];

const daysAgoStr = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
};
const maskName = (n) =>
  n.length <= 1 ? n : n.length === 2 ? n[0] + '*' : n[0] + '*'.repeat(n.length - 2) + n[n.length - 1];
const maskEmail = (e) => {
  const [u, d] = e.split('@');
  return u.slice(0, 2) + '***@' + d;
};
const maskPhone = (p) => (p ? p.replace(/^(\d{3})-?\d{3,4}-?(\d{4})$/, '$1-****-$2') : null);

const pickGroup = (i) => {
  if (i < 40) return 1;
  if (i < 55) return 2;
  if (i < 65) return 3;
  if (i < 75) return 5;
  if (i < 80) return 6;
  return 9;
};
const pickDaysAgo = (i) => {
  const m = i % 10;
  if (m === 0) return 0;            // 오늘
  if (m <= 3) return m;             // 최근 1~3일
  if (m <= 5) return 8 + (i % 18);  // 최근 한 달
  return 40 + i * 8;               // 과거 분산
};

const members = new Map(); // memNo → full record
for (let i = 0; i < 100; i++) {
  const memNo = 10001 + i;
  const [famKo, famEn] = FAMILY[i % FAMILY.length];
  const memName = famKo + GIVEN[(i * 7) % GIVEN.length];
  const memId = famEn + memNo;
  const groupSno = pickGroup(i);
  const email = `${famEn}${memNo}@example.com`;
  const cellPhone = `010-${String(1000 + i).slice(-4)}-${String(5678 + i).slice(-4)}`;
  const entryDt = daysAgoStr(pickDaysAgo(i));
  const appFl = i % 9 === 4 ? 'n' : 'y';

  const business =
    groupSno === 1
      ? null
      : {
          company: `(주)${memName}상사`,
          businessNo: `${String(100 + i).slice(-3)}-${String(10 + (i % 80)).padStart(2, '0')}-${10000 + i}`,
          ceoName: memName,
          service: SERVICES[i % SERVICES.length],
          item: ITEMS[i % ITEMS.length],
          comAddress: `0${6000 + (i % 3000)} 서울시 강남구 테헤란로 ${100 + i}`,
          bizLicenseFile: `license_${memId}.pdf`,
        };

  const hasPartner = groupSno === 9 || (groupSno !== 1 && i % 3 === 0);
  const partner = hasPartner
    ? {
        partnerRegisteredFl: 'y',
        partnerCode: String(990000 + i),
        partnerName: `(에쓰푸드몰)${memName}거래처`,
        taxInvoiceFl: i % 2 === 0 ? 'y' : 'n',
        duplicateCheck: null,
        updatedAt: new Date(Date.now() - i * 36e5).toISOString(),
      }
    : null;

  members.set(memNo, {
    memNo, memId, memName, groupSno, groupName: GROUP_NAMES[groupSno],
    appFl, appDt: appFl === 'y' ? entryDt : null,
    email, cellPhone, phone: i % 4 === 0 ? `02-${String(100 + i).slice(-3)}-${String(4000 + i).slice(-4)}` : null,
    address: `0${6000 + (i % 3000)} 서울시 강남구 테헤란로 ${100 + i} ${100 + (i % 900)}호`,
    entryDt, lastLoginDt: i % 5 === 0 ? null : daysAgoStr(i % 30), entryPath: PATHS[i % PATHS.length],
    maillingFl: i % 2 === 0 ? 'y' : 'n', smsFl: i % 3 === 0 ? 'y' : 'n',
    nickName: null, sexFl: i % 2 === 0 ? 'm' : 'w', birthDt: `19${85 + (i % 15)}-0${1 + (i % 9)}-1${i % 9}`,
    marriFl: null, marriDate: null, job: null, interest: null,
    loginCount: (i * 13) % 200, saleAmount: (i % 7) * 50000, mileage: (i % 5) * 300, deposit: 0,
    recommId: null,
    ex1: business ? ITEMS[(i + 2) % ITEMS.length] + '제조' : null,
    ex2: business ? (i % 2 === 0 ? 'Y' : 'N') : null,
    ex3: business && i % 3 === 0 ? `${memName} 대리` : null,
    ex4: null, ex5: null, ex6: null, memo: null,
    business, partner,
  });
}

const toListItem = (m) => ({
  memNo: m.memNo, memId: m.memId, memName: maskName(m.memName),
  groupSno: m.groupSno, groupName: m.groupName, email: maskEmail(m.email), cellPhone: maskPhone(m.cellPhone),
  company: m.business?.company ?? null, businessNo: m.business?.businessNo ?? null,
  ceoName: m.business?.ceoName ?? null, service: m.business?.service ?? null,
  item: m.business?.item ?? null, comAddress: m.business?.comAddress ?? null,
  partnerRegisteredFl: m.partner?.partnerRegisteredFl ?? null,
  partnerCode: m.partner?.partnerCode ?? null, partnerName: m.partner?.partnerName ?? null,
  entryDt: m.entryDt, appFl: m.appFl,
});

const SORT_FIELD = { mem_no: 'memNo', mem_id: 'memId', group_sno: 'groupSno', app_fl: 'appFl', entry_dt: 'entryDt' };

app.get('/api/members', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const { searchType, keyword, group, sortKey, sortDir, entryDtFrom, entryDtTo } = req.query;
  let list = Array.from(members.values());

  // 등급 필터
  if (group === 'GENERAL') list = list.filter((m) => m.groupSno === 1);
  else if (group === 'BUSINESS') list = list.filter((m) => [2, 3, 5, 6, 9].includes(m.groupSno));
  else if (group === 'B2B') list = list.filter((m) => m.groupSno === 9);

  // 가입일 필터
  if (entryDtFrom) list = list.filter((m) => m.entryDt >= entryDtFrom);
  if (entryDtTo) list = list.filter((m) => m.entryDt <= entryDtTo);

  // 검색
  if (keyword) {
    const kw = String(keyword);
    list = list.filter((m) => {
      switch (searchType) {
        case 'NAME': return m.memName.includes(kw);
        case 'EMAIL': return m.email.includes(kw);
        case 'CELL_PHONE': return m.cellPhone.includes(kw);
        case 'PARTNER_CODE': return (m.partner?.partnerCode ?? '').includes(kw);
        case 'PARTNER_NAME': return (m.partner?.partnerName ?? '').includes(kw);
        default: return m.memId.includes(kw);
      }
    });
  }

  // 정렬
  const field = SORT_FIELD[sortKey] ?? 'memNo';
  const dir = sortDir === 'asc' ? 1 : -1;
  list.sort((a, b) => (a[field] < b[field] ? -1 : a[field] > b[field] ? 1 : 0) * dir);

  // 페이징
  const page = Math.max(0, parseInt(req.query.page) || 0);
  const size = Math.max(1, parseInt(req.query.size) || 10);
  const totalElements = list.length;
  const content = list.slice(page * size, page * size + size).map(toListItem);

  res.status(200).json({
    content, page, size, totalElements, totalPages: Math.ceil(totalElements / size),
  });
});

app.get('/api/members/summary', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const all = Array.from(members.values());
  res.status(200).json({
    totalCount: all.length,
    businessCount: all.filter((m) => [2, 3, 5, 6, 9].includes(m.groupSno)).length,
    b2bCount: all.filter((m) => m.groupSno === 9).length,
    pendingApprovalCount: all.filter((m) => m.appFl === 'n').length,
    lastSyncedAt: new Date().toISOString(),
  });
});

app.get('/api/members/:memNo', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const m = members.get(parseInt(req.params.memNo));
  if (!m) return res.status(404).json({ status: 404, error: 'Not Found', message: '회원을 찾을 수 없습니다', path: req.path });
  res.status(200).json(m);
});

app.put('/api/members/:memNo/partner', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const m = members.get(parseInt(req.params.memNo));
  if (!m) return res.status(404).json({ status: 404, error: 'Not Found', message: 'MEMBER_NOT_FOUND', path: req.path });
  if (!m.partner) return res.status(404).json({ status: 404, error: 'Not Found', message: 'MEMBER_PARTNER_NOT_FOUND', path: req.path });

  const { partnerRegisteredFl, partnerCode, partnerName, taxInvoiceFl, duplicateCheck, bizLicenseFile } = req.body;
  m.partner.partnerRegisteredFl = partnerRegisteredFl ?? 'n';
  m.partner.partnerCode = partnerCode ?? null;
  m.partner.partnerName = partnerName ?? null;
  m.partner.taxInvoiceFl = taxInvoiceFl ?? 'n';
  m.partner.duplicateCheck = duplicateCheck ?? null;
  m.partner.updatedAt = new Date().toISOString();
  if (m.business && bizLicenseFile != null) m.business.bizLicenseFile = bizLicenseFile;

  res.status(200).json({
    partnerRegisteredFl: m.partner.partnerRegisteredFl,
    partnerCode: m.partner.partnerCode,
    partnerName: m.partner.partnerName,
    taxInvoiceFl: m.partner.taxInvoiceFl,
    duplicateCheck: m.partner.duplicateCheck,
    bizLicenseFile: m.business?.bizLicenseFile ?? null,
    updatedAt: m.partner.updatedAt,
  });
});

app.post('/api/members/uploads', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const rows = Array.isArray(req.body?.rows) ? req.body.rows : [];
  res.status(200).json({
    totalInserted: 0, totalUpdated: rows.length, totalDeleted: 0, totalFailed: 0,
    failedRows: [], deletionSkipped: false, deletionSkippedReason: null,
    syncedAt: new Date().toISOString(),
  });
});

// Dashboard APIs (mock data for local UI verification only)
app.get('/api/dashboard/summary', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json({
    revenue: 12345000, orderCount: 87, buyerCount: 62, orderFrequency: 1.4,
    avgSku: 2.3, avgQty: 3.1, aov: 141896, arppu: 199113,
  });
});

app.get('/api/dashboard/hourly-revenue', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const data = Array.from({ length: 24 }, (_, hour) => ({
    hour, revenue: Math.round(Math.sin(hour / 3) * 500000 + 600000),
  }));
  res.status(200).json(data);
});

app.get('/api/dashboard/grade-revenue', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json([
    { grade: 'VIP', revenue: 6000000 },
    { grade: '일반회원', revenue: 4000000 },
    { grade: '사업자회원', revenue: 2345000 },
  ]);
});

app.get('/api/dashboard/grade-detail', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json([
    { grade: 'VIP', revenue: 6000000, orderCount: 30, buyerCount: 20, avgSku: 2.5, avgQty: 3.5, aov: 200000, arppu: 300000 },
    { grade: '일반회원', revenue: 4000000, orderCount: 40, buyerCount: 30, avgSku: 2.0, avgQty: 2.8, aov: 100000, arppu: 133333 },
    { grade: '사업자회원', revenue: 2345000, orderCount: 17, buyerCount: 12, avgSku: 2.2, avgQty: 3.0, aov: 137941, arppu: 195417 },
  ]);
});

app.get('/api/dashboard/top-items', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json([
    { productName: '리얼 스모크햄_1000(냉동)', revenue: 3200000, qty: 120 },
    { productName: '잠봉 슬라이스 (3종 총합)', revenue: 2100000, qty: 95 },
    { productName: '숙성 베이컨_500', revenue: 1500000, qty: 70 },
  ]);
});

// bucket 단위(DAY/MONTH/QUARTER)에 맞춰 endDate로부터 count개의 버킷 말일을 오래된 순으로 생성한다.
const buildBucketEnds = (bucket, count, end) => {
  const [endYear, endMonth] = end.split('-').map(Number);
  if (bucket === 'MONTH' || bucket === 'QUARTER') {
    const monthsPerBucket = bucket === 'QUARTER' ? 3 : 1;
    const endMonthIndex = endYear * 12 + (endMonth - 1);
    const endBucketIndex = Math.floor(endMonthIndex / monthsPerBucket);
    return Array.from({ length: count }, (_, i) => {
      const bucketIndex = endBucketIndex - (count - 1 - i);
      const lastMonthIndex = bucketIndex * monthsPerBucket + (monthsPerBucket - 1);
      const y = Math.floor(lastMonthIndex / 12);
      const m = lastMonthIndex % 12; // 0-indexed
      const lastDay = new Date(y, m + 1, 0).getDate();
      return `${y}-${String(m + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
    });
  }
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(end);
    d.setDate(d.getDate() - (count - 1 - i));
    return d.toISOString().slice(0, 10);
  });
};

app.get('/api/dashboard/revenue-trend', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const count = parseInt(req.query.count) || 20;
  const end = req.query.endDate || new Date().toISOString().slice(0, 10);
  const bucketEnds = buildBucketEnds(req.query.bucket, count, end);
  const data = bucketEnds.map((iso) => ({
    start: iso, end: iso, revenue: Math.round(Math.random() * 5000000 + 5000000), orderCount: Math.round(Math.random() * 50 + 50),
  }));
  res.status(200).json(data);
});

app.get('/api/dashboard/grade-pivot', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  const count = parseInt(req.query.count) || 3;
  const end = req.query.endDate || new Date().toISOString().slice(0, 10);
  const bucketEnds = buildBucketEnds(req.query.bucket, count, end);
  const grades = ['VIP', '일반회원', '사업자회원'];
  const byGrade = {};
  grades.forEach((grade, gi) => {
    byGrade[grade] = bucketEnds.map(() => Math.round((Math.random() * 2000000 + 500000) / (gi + 1)));
  });
  res.status(200).json({ bucketEnds, byGrade });
});

app.get('/api/dashboard/yoy-comparison', (req, res) => {
  const auth = checkAuth(req, res);
  if (!auth) return;
  res.status(200).json({
    current: { revenue: 12345000, orderCount: 87, buyerCount: 62, orderFrequency: 1.4, avgSku: 2.3, avgQty: 3.1, aov: 141896, arppu: 199113 },
    previous: { revenue: 9800000, orderCount: 70, buyerCount: 55, orderFrequency: 1.3, avgSku: 2.1, avgQty: 2.9, aov: 140000, arppu: 178181 },
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Mock API Server running on http://localhost:${PORT}`);
  console.log(`👥 Members seeded: ${members.size}`);
});
