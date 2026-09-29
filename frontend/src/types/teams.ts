// Adaptive Card v1.4 — Teams 웹훅 알림 타입

export interface AdaptiveCardTextBlock {
  type: 'TextBlock';
  text: string;
  weight?: 'bolder' | 'default' | 'lighter';
  size?: 'small' | 'default' | 'medium' | 'large' | 'extraLarge';
  color?: 'default' | 'dark' | 'light' | 'accent' | 'good' | 'warning' | 'attention';
  wrap?: boolean;
  separator?: boolean;
}

export interface AdaptiveCardFactSet {
  type: 'FactSet';
  facts: Array<{ title: string; value: string }>;
}

export interface AdaptiveCardColumn {
  type: 'Column';
  width?: string | number;
  items: AdaptiveCardTextBlock[];
}

export interface AdaptiveCardColumnSet {
  type: 'ColumnSet';
  columns: AdaptiveCardColumn[];
}

export interface AdaptiveCardContainer {
  type: 'Container';
  items: (AdaptiveCardTextBlock | AdaptiveCardColumnSet)[];
  separator?: boolean;
}

export type AdaptiveCardElement =
  | AdaptiveCardTextBlock
  | AdaptiveCardFactSet
  | AdaptiveCardColumnSet
  | AdaptiveCardContainer;

export interface AdaptiveCardActionOpenUrl {
  type: 'Action.OpenUrl';
  title: string;
  url: string;
}

export interface AdaptiveCard {
  $schema: 'http://adaptivecards.io/schemas/adaptive-card.json';
  type: 'AdaptiveCard';
  version: '1.4';
  body: AdaptiveCardElement[];
  actions?: AdaptiveCardActionOpenUrl[];
}

export interface TeamsWebhookPayload {
  type: 'message';
  attachments: Array<{
    contentType: 'application/vnd.microsoft.card.adaptive';
    content: AdaptiveCard;
  }>;
}
