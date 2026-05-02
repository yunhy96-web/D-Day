/**
 * 백엔드 통신은 백엔드 모드(수동 트리거)에서만 사용.
 * 키워드는 앱 SQLite에서 관리하고, 백엔드 호출 시 함께 전달.
 */
export const BACKEND_URL = 'https://nonmediative-jodie-bracted.ngrok-free.dev';

const defaultHeaders = {
  'Content-Type': 'application/json',
  'ngrok-skip-browser-warning': 'true',
};

export interface CheckNowProduct {
  name: string;
  url: string;
}

export interface CheckNowResponse {
  totalCount: number;
  matchedCount: number;
  matched: CheckNowProduct[];
  checkedAt: string;
}

export interface KeywordOverride {
  includeKeywords: string[];
  excludeKeywords: string[];
}

interface BackendSiteSummary {
  id: string;
  url: string;
}

function normalizeUrl(url: string): string {
  return url.trim().replace(/\/+$/, '').toLowerCase();
}

async function findBackendSiteIdByUrl(url: string): Promise<string | null> {
  const res = await fetch(`${BACKEND_URL}/api/sites`, { headers: defaultHeaders });
  if (!res.ok) throw new Error(`사이트 조회 실패: ${res.status}`);
  const sites = (await res.json()) as BackendSiteSummary[];
  const target = normalizeUrl(url);
  const match = sites.find((s) => normalizeUrl(s.url) === target);
  return match ? match.id : null;
}

export async function checkSiteNow(
  id: string,
  override?: KeywordOverride
): Promise<CheckNowResponse> {
  const res = await fetch(`${BACKEND_URL}/api/sites/${id}/check-now`, {
    method: 'POST',
    headers: defaultHeaders,
    body: override ? JSON.stringify(override) : undefined,
  });
  if (!res.ok) throw new Error(`즉시 확인 실패: ${res.status}`);
  return res.json();
}

export async function checkSiteNowByUrl(
  url: string,
  override?: KeywordOverride
): Promise<CheckNowResponse | null> {
  const id = await findBackendSiteIdByUrl(url);
  if (!id) return null;
  return checkSiteNow(id, override);
}
