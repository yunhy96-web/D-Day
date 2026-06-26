'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import type { ReservationDetail } from '@/lib/api';

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR') + '원';
}

function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-').map(Number);
  const d = new Date(year, month - 1, day);
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: '입금 대기',
  CONFIRMED: '예약 확정',
  CANCELLED: '취소됨',
};

const STATUS_STYLE: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-500',
};

export default function ReservationLookupPage() {
  const router = useRouter();
  const [no, setNo] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ReservationDetail | null>(null);

  async function handleLookup(e: React.FormEvent) {
    e.preventDefault();
    if (!no.trim() || !email.trim()) {
      setError('예약번호와 이메일을 모두 입력해주세요.');
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch(
        `/api/v1/reservations/lookup?no=${encodeURIComponent(no.trim())}&email=${encodeURIComponent(email.trim())}`,
      );
      const json = await res.json();
      if (json.success && json.data) {
        setResult(json.data as ReservationDetail);
      } else {
        setError('예약 정보를 찾을 수 없습니다. 예약번호와 이메일을 확인해주세요.');
      }
    } catch {
      setError('조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-10">
      <h1 className="mb-2 text-xl font-bold text-ink-900">예약 조회</h1>
      <p className="mb-6 text-sm text-ink-500">예약번호와 예약 시 입력한 이메일로 조회합니다.</p>

      <form onSubmit={handleLookup} className="space-y-4">
        <input
          type="text"
          placeholder="예약번호"
          value={no}
          onChange={(e) => setNo(e.target.value)}
          required
          className="w-full rounded-xl border border-cream-200 bg-white px-4 py-2.5 text-sm text-ink-800 placeholder-ink-400 focus:border-brand-400 focus:outline-none focus:ring-1 focus:ring-brand-400"
        />
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="w-full rounded-xl border border-cream-200 bg-white px-4 py-2.5 text-sm text-ink-800 placeholder-ink-400 focus:border-brand-400 focus:outline-none focus:ring-1 focus:ring-brand-400"
        />

        {error && (
          <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>
        )}

        <button type="submit" disabled={loading} className="btn-brand w-full">
          {loading ? '조회 중...' : '예약 조회'}
        </button>
      </form>

      {result && (
        <div className="mt-8 rounded-2xl border border-cream-200 bg-white p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-ink-800">{result.reservationNo}</span>
            <span
              className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS_STYLE[result.status] ?? 'bg-cream-200 text-ink-500'}`}
            >
              {STATUS_LABEL[result.status] ?? result.status}
            </span>
          </div>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-ink-500">출발일</span>
              <span className="text-ink-800">{formatDate(result.saleDate)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-ink-500">인원</span>
              <span className="text-ink-800">{result.quantity}명</span>
            </div>
            <div className="flex justify-between">
              <span className="text-ink-500">결제 금액</span>
              <span className="font-bold text-brand-600">{formatPrice(result.amount)}</span>
            </div>
          </div>
          <button
            type="button"
            onClick={() => router.push(`/reservations/${result.id}`)}
            className="btn-brand-outline w-full"
          >
            상세 보기
          </button>
        </div>
      )}
    </div>
  );
}
