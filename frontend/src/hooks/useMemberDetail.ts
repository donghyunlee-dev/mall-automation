import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { getMemberDetail, updateMemberPartner, updateMemberBusiness } from '../api/members';
import type { MemberDetail, UpdatePartnerRequest, UpdateBusinessRequest } from '../types/member';

interface UseMemberDetailReturn {
  member: MemberDetail | null;
  loading: boolean;
  error: string | null;
  notFound: boolean;
  updatePartner: (req: UpdatePartnerRequest) => Promise<boolean>;
  partnerUpdating: boolean;
  updateBusiness: (req: UpdateBusinessRequest) => Promise<boolean>;
  businessUpdating: boolean;
  refetch: () => void;
}

const useMemberDetail = (
  memNo: number | null,
  onPartnerError?: (message: string) => void,
): UseMemberDetailReturn => {
  const [member, setMember] = useState<MemberDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [partnerUpdating, setPartnerUpdating] = useState(false);
  const [businessUpdating, setBusinessUpdating] = useState(false);

  const fetch = useCallback(async (no: number) => {
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const data = await getMemberDetail(no);
      setMember(data);
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.status === 404) {
        setNotFound(true);
        setError('회원을 찾을 수 없어요.');
      } else {
        setError('회원 정보를 불러오지 못했어요.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (memNo == null) {
      setMember(null);
      setError(null);
      setNotFound(false);
      return;
    }
    void fetch(memNo);
  }, [memNo, fetch]);

  const updatePartner = useCallback(
    async (req: UpdatePartnerRequest): Promise<boolean> => {
      if (memNo == null) return false;
      setPartnerUpdating(true);
      try {
        await updateMemberPartner(memNo, req);
        await fetch(memNo);
        return true;
      } catch (e) {
        const status = axios.isAxiosError(e) ? e.response?.status : undefined;
        let msg = '거래처 정보 저장에 실패했어요. 다시 시도해 주세요.';
        if (status === 404) {
          msg = '회원을 찾을 수 없어요. 목록에서 다시 확인해 주세요.';
        } else if (status === 409) {
          msg = '이미 다른 회원이 사용 중인 거래처 코드예요. 다른 코드를 입력해 주세요.';
        } else if (status === 400) {
          msg = '거래처 상태 값이 올바르지 않아요.';
        }
        onPartnerError?.(msg);
        return false;
      } finally {
        setPartnerUpdating(false);
      }
    },
    [memNo, fetch, onPartnerError],
  );

  const updateBusiness = useCallback(
    async (req: UpdateBusinessRequest): Promise<boolean> => {
      if (memNo == null) return false;
      setBusinessUpdating(true);
      try {
        await updateMemberBusiness(memNo, req);
        await fetch(memNo);
        return true;
      } catch {
        onPartnerError?.('사업자 정보 저장에 실패했어요. 다시 시도해 주세요.');
        return false;
      } finally {
        setBusinessUpdating(false);
      }
    },
    [memNo, fetch, onPartnerError],
  );

  const refetch = useCallback(() => {
    if (memNo != null) void fetch(memNo);
  }, [memNo, fetch]);

  return { member, loading, error, notFound, updatePartner, partnerUpdating, updateBusiness, businessUpdating, refetch };
};

export default useMemberDetail;
