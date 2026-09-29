import client from './client';
import type {
  MemberPage,
  MemberSearchParams,
  MemberUploadRequest,
  MemberUploadResponse,
  MemberDetail,
  UpdatePartnerRequest,
  MemberPartnerResponse,
  UpdateBusinessRequest,
  UpdateBusinessResponse,
  MemberBusinessLogItem,
  MemberPartnerLogItem,
} from '../types/member';

export async function getMembers(params: Partial<MemberSearchParams>): Promise<MemberPage> {
  const res = await client.get<MemberPage>('/api/members', { params });
  return {
    content: res.data.content ?? [],
    page: res.data.page ?? 0,
    size: res.data.size ?? 10,
    totalElements: res.data.totalElements ?? 0,
    totalPages: res.data.totalPages ?? 0,
  };
}

export async function uploadMembers(req: MemberUploadRequest): Promise<MemberUploadResponse> {
  const res = await client.post<MemberUploadResponse>('/api/members/uploads', req);
  return res.data;
}

export async function getMemberDetail(memNo: number): Promise<MemberDetail> {
  const res = await client.get<MemberDetail>(`/api/members/${memNo}`);
  return res.data;
}

export async function deleteMember(memNo: number): Promise<void> {
  await client.delete(`/api/members/${memNo}`);
}

export async function updateMemberPartner(
  memNo: number,
  req: UpdatePartnerRequest,
): Promise<MemberPartnerResponse> {
  const res = await client.put<MemberPartnerResponse>(`/api/members/${memNo}/partner`, req);
  return res.data;
}

export async function updateMemberBusiness(
  memNo: number,
  req: UpdateBusinessRequest,
): Promise<UpdateBusinessResponse> {
  const res = await client.put<UpdateBusinessResponse>(`/api/members/${memNo}/business`, req);
  return res.data;
}

export async function getMemberBusinessLogs(memNo: number): Promise<MemberBusinessLogItem[]> {
  const res = await client.get<{ logs: MemberBusinessLogItem[] }>(`/api/members/${memNo}/business/logs`);
  return res.data.logs;
}

export async function getMemberPartnerLogs(memNo: number): Promise<MemberPartnerLogItem[]> {
  const res = await client.get<{ logs: MemberPartnerLogItem[] }>(`/api/members/${memNo}/partner/logs`);
  return res.data.logs;
}
