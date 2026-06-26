'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import type { Schedule, CreateReservationBody } from '@/lib/api';
import { createReservation } from '@/lib/api';

interface Props {
  productId: string;
  productName: string;
}

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR') + '원';
}

function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-').map(Number);
  const d = new Date(year, month - 1, day);
  return d.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  });
}

export default function BookingPanel({ productId, productName }: Props) {
  const router = useRouter();

  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [loadingSchedules, setLoadingSchedules] = useState(true);

  const [selectedSchedule, setSelectedSchedule] = useState<Schedule | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSchedules = useCallback(async () => {
    setLoadingSchedules(true);
    try {
      const res = await fetch(`/api/v1/products/${productId}/schedules`);
      const json = await res.json();
      if (json.success) {
        setSchedules(json.data ?? []);
      }
    } catch {
      // 스케줄 로드 실패 시 빈 배열 유지
    } finally {
      setLoadingSchedules(false);
    }
  }, [productId]);

  useEffect(() => {
    fetchSchedules();
  }, [fetchSchedules]);

  const maxQuantity =
    selectedSchedule?.stock == null ? 20 : Math.max(1, selectedSchedule.stock);

  function handleScheduleSelect(schedule: Schedule) {
    if (schedule.soldOut) return;
    setSelectedSchedule(schedule);
    setQuantity(1);
    setError(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedSchedule) {
      setError('출발 일정을 선택해주세요.');
      return;
    }
    if (!name.trim() || !email.trim() || !phone.trim()) {
      setError('예약자 정보를 모두 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setError(null);

    const body: CreateReservationBody = {
      productScheduleId: selectedSchedule.scheduleId,
      quantity,
      paymentMethod: 'BANK_TRANSFER',
      reserver: { name: name.trim(), email: email.trim(), phone: phone.trim() },
    };

    try {
      const result = await createReservation(body);
      router.push(`/reservations/${result.reservationId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '예약에 실패했습니다. 다시 시도해주세요.');
      setSubmitting(false);
    }
  }

  if (loadingSchedules) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
        <span className="ml-2 text-sm text-ink-500">일정 불러오는 중...</span>
      </div>
    );
  }

  if (schedules.length === 0) {
    return (
      <p className="mt-4 text-center text-sm text-ink-500">
        현재 예약 가능한 출발 일정이 없습니다.
      </p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mt-4 space-y-5">
      {/* 날짜 선택 */}
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">출발 일정 선택</p>
        <div className="space-y-2">
          {schedules.map((s) => {
            const isSelected = selectedSchedule?.scheduleId === s.scheduleId;
            return (
              <button
                key={s.scheduleId}
                type="button"
                disabled={s.soldOut}
                onClick={() => handleScheduleSelect(s)}
                className={[
                  'flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left text-sm transition-colors',
                  s.soldOut
                    ? 'cursor-not-allowed border-cream-200 bg-cream-100 text-ink-400'
                    : isSelected
                      ? 'border-brand-500 bg-brand-50 text-ink-800'
                      : 'border-cream-200 bg-white text-ink-700 hover:border-brand-300 hover:bg-cream-50',
                ].join(' ')}
              >
                <span className="font-medium">{formatDate(s.saleDate)}</span>
                <span className="flex items-center gap-2">
                  {s.soldOut ? (
                    <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-500">
                      매진
                    </span>
                  ) : s.stock != null && s.stock <= 5 ? (
                    <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-600">
                      잔여 {s.stock}석
                    </span>
                  ) : null}
                  <span className="font-semibold text-brand-600">{formatPrice(s.price)}</span>
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 수량 선택 */}
      {selectedSchedule && (
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">인원 수</p>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-cream-200 bg-white text-ink-700 hover:bg-cream-100"
            >
              -
            </button>
            <span className="w-8 text-center text-sm font-semibold text-ink-800">{quantity}</span>
            <button
              type="button"
              onClick={() => setQuantity((q) => Math.min(maxQuantity, q + 1))}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-cream-200 bg-white text-ink-700 hover:bg-cream-100"
            >
              +
            </button>
            <span className="text-xs text-ink-500">
              {selectedSchedule.stock == null
                ? '인원 제한 없음'
                : `최대 ${selectedSchedule.stock}명`}
            </span>
          </div>
        </div>
      )}

      {/* 합계 금액 */}
      {selectedSchedule && (
        <div className="rounded-xl bg-cream-100 px-4 py-3">
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-ink-500">
              {formatPrice(selectedSchedule.price)} × {quantity}명
            </span>
            <span className="text-lg font-bold text-brand-600">
              {formatPrice(selectedSchedule.price * quantity)}
            </span>
          </div>
        </div>
      )}

      {/* 예약자 정보 */}
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">예약자 정보</p>
        <div className="space-y-3">
          <input
            type="text"
            placeholder="이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
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
          <input
            type="tel"
            placeholder="연락처 (01012345678)"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
            className="w-full rounded-xl border border-cream-200 bg-white px-4 py-2.5 text-sm text-ink-800 placeholder-ink-400 focus:border-brand-400 focus:outline-none focus:ring-1 focus:ring-brand-400"
          />
        </div>
      </div>

      {/* 오류 메시지 */}
      {error && (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</p>
      )}

      {/* 제출 */}
      <button
        type="submit"
        disabled={submitting || !selectedSchedule}
        className="btn-brand w-full"
      >
        {submitting ? '예약 처리 중...' : '예약하기'}
      </button>
      <p className="text-center text-xs text-ink-400">
        비회원 예약 · 무통장 입금(계좌이체)
      </p>
    </form>
  );
}
