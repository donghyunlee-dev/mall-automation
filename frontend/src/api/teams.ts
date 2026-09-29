import type { TeamsWebhookPayload, AdaptiveCard, AdaptiveCardColumnSet } from '../types/teams';
import type { BatchSchedule } from '../types/batch';
import type { GoodsStatusLog } from '../types/goods';

const STATUS_LABEL: Record<string, string> = {
  ON_SALE: '판매중',
  OUT_OF_STOCK: '품절',
};

const formatDatetime = (isoStr: string | null): string => {
  if (!isoStr) return '—';
  const d = new Date(isoStr);
  const y = d.getFullYear();
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const mi = String(d.getMinutes()).padStart(2, '0');
  return `${y}.${mo}.${day} ${h}:${mi}`;
};

const makeTableRow = (
  col1: string,
  col2: string,
  col3: string,
  isBold = false,
): AdaptiveCardColumnSet => ({
  type: 'ColumnSet',
  columns: [
    {
      type: 'Column',
      width: 'stretch',
      items: [{ type: 'TextBlock', text: col1, wrap: true, weight: isBold ? 'bolder' : 'default' }],
    },
    {
      type: 'Column',
      width: 'auto',
      items: [{ type: 'TextBlock', text: col2, wrap: false, weight: isBold ? 'bolder' : 'default' }],
    },
    {
      type: 'Column',
      width: 'auto',
      items: [{ type: 'TextBlock', text: col3, wrap: false, weight: isBold ? 'bolder' : 'default' }],
    },
  ],
});

export const buildStatusChangeCard = (
  schedule: BatchSchedule,
  logs: GoodsStatusLog[],
  dashboardUrl: string,
): TeamsWebhookPayload => {
  const tableRows: AdaptiveCardColumnSet[] = [
    makeTableRow('상품명', '고도몰 코드', '변경 내용', true),
    ...logs.map((log) =>
      makeTableRow(
        log.godoGoodsCode,
        log.godoGoodsCode,
        `${STATUS_LABEL[log.beforeStatus] ?? log.beforeStatus} → ${STATUS_LABEL[log.afterStatus] ?? log.afterStatus}`,
      ),
    ),
  ];

  const card: AdaptiveCard = {
    $schema: 'http://adaptivecards.io/schemas/adaptive-card.json',
    type: 'AdaptiveCard',
    version: '1.4',
    body: [
      {
        type: 'TextBlock',
        text: '📦 에쓰푸드몰 판매 상태 변경 알림',
        weight: 'bolder',
        size: 'medium',
        wrap: true,
      },
      {
        type: 'FactSet',
        facts: [
          { title: '실행 시각', value: formatDatetime(schedule.lastStartedAt) },
          { title: '변경 건수', value: `${logs.length}건` },
        ],
      },
      {
        type: 'Container',
        separator: true,
        items: tableRows,
      },
    ],
    actions: [
      {
        type: 'Action.OpenUrl',
        title: '상품 관리 바로가기',
        url: dashboardUrl,
      },
    ],
  };

  return {
    type: 'message',
    attachments: [
      {
        contentType: 'application/vnd.microsoft.card.adaptive',
        content: card,
      },
    ],
  };
};

export const buildErrorCard = (
  schedule: BatchSchedule,
): TeamsWebhookPayload => {
  const card: AdaptiveCard = {
    $schema: 'http://adaptivecards.io/schemas/adaptive-card.json',
    type: 'AdaptiveCard',
    version: '1.4',
    body: [
      {
        type: 'TextBlock',
        text: '⚠️ 에쓰푸드몰 자동화 오류 발생',
        weight: 'bolder',
        size: 'medium',
        color: 'attention',
        wrap: true,
      },
      {
        type: 'FactSet',
        facts: [
          { title: '발생 시각', value: formatDatetime(schedule.lastEndedAt) },
          { title: '오류 내용', value: schedule.lastMessage ?? '알 수 없는 오류' },
        ],
      },
      {
        type: 'TextBlock',
        text: '자동화 서비스가 정상적으로 동작하지 않을 수 있습니다. 담당 개발자에게 문의해주세요.',
        wrap: true,
        separator: true,
      },
    ],
  };

  return {
    type: 'message',
    attachments: [
      {
        contentType: 'application/vnd.microsoft.card.adaptive',
        content: card,
      },
    ],
  };
};

export const sendTeamsNotification = (payload: TeamsWebhookPayload): Promise<void> => {
  const webhookUrl = import.meta.env.VITE_TEAMS_WEBHOOK_URL as string | undefined;
  if (!webhookUrl) return Promise.resolve();

  return fetch(webhookUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }).then((res) => {
    if (!res.ok) throw new Error(`Teams webhook responded with ${res.status}`);
  });
};
