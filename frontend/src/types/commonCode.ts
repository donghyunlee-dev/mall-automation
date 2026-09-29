export interface CommonCodeItem {
  id: number;
  groupCode: string;
  groupName: string;
  code: string;
  codeName: string;
  note: string | null;
  useYn: string;
  createdAt: string;
  updatedAt: string;
}

export type CommonCodeGroup = 'SERVICE' | 'ITEM';
