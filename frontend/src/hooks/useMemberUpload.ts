import { useState, useCallback } from 'react';
import type { ParseResult } from '../utils/parseMemberExcel';
import { uploadMembers } from '../api/members';
import type { MemberUploadResponse, MemberUploadType } from '../types/member';

type UploadStatus = 'idle' | 'parsing' | 'uploading' | 'done' | 'error';

interface UseMemberUploadReturn {
  status: UploadStatus;
  detectedType: MemberUploadType | null;
  result: MemberUploadResponse | null;
  error: string | null;
  isDamagedFile: boolean;
  upload: (file: File, applyDeletion: boolean) => Promise<void>;
  reset: () => void;
}

const useMemberUpload = (): UseMemberUploadReturn => {
  const [status, setStatus] = useState<UploadStatus>('idle');
  const [detectedType, setDetectedType] = useState<MemberUploadType | null>(null);
  const [result, setResult] = useState<MemberUploadResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isDamagedFile, setIsDamagedFile] = useState(false);

  const upload = useCallback(async (file: File, applyDeletion: boolean) => {
    setStatus('parsing');
    setError(null);
    setResult(null);
    setIsDamagedFile(false);

    let parsed: ParseResult;
    try {
      // xlsx 파서 번들은 실제 업로드 시점에만 로드 (코드 분할)
      const { parseMemberExcel } = await import('../utils/parseMemberExcel');
      parsed = await parseMemberExcel(file);
    } catch (err) {
      setStatus('error');
      setError(err instanceof Error ? err.message : '파일을 파싱할 수 없어요.');
      return;
    }

    setDetectedType(parsed.type);

    if (parsed.isDamaged) {
      setIsDamagedFile(true);
      setStatus('error');
      setError(
        'Excel에서 다시 저장한 파일은 데이터가 빠질 수 있습니다. 고도몰에서 받은 원본 파일을 그대로 업로드해 주세요.',
      );
      return;
    }

    setStatus('uploading');
    try {
      const res = await uploadMembers({
        type: parsed.type,
        fileName: file.name,
        applyDeletion,
        rows: parsed.rows,
      });
      setResult(res);
      setStatus('done');
    } catch {
      setStatus('error');
      setError('업로드에 실패했어요. 다시 시도해주세요.');
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setDetectedType(null);
    setResult(null);
    setError(null);
    setIsDamagedFile(false);
  }, []);

  return { status, detectedType, result, error, isDamagedFile, upload, reset };
};

export default useMemberUpload;
