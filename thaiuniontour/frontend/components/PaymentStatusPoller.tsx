'use client';

import { useState, useEffect, useRef } from 'react';
import { getPaymentStatus } from '@/lib/api';

interface Props {
  reservationId: string;
  initialStatus: 'PAID' | 'PENDING';
}

export default function PaymentStatusPoller({ reservationId, initialStatus }: Props) {
  const [status, setStatus] = useState<'PAID' | 'PENDING'>(initialStatus);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (status === 'PAID') return;

    intervalRef.current = setInterval(async () => {
      const result = await getPaymentStatus(reservationId);
      if (result?.status === 'PAID') {
        setStatus('PAID');
        if (intervalRef.current) clearInterval(intervalRef.current);
      }
    }, 3000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [reservationId, status]);

  if (status === 'PAID') {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-green-50 px-4 py-3">
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-green-500 text-white text-xs font-bold">
          ✓
        </span>
        <span className="text-sm font-semibold text-green-700">결제 완료</span>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2 rounded-xl bg-amber-50 px-4 py-3">
      <div className="h-4 w-4 animate-spin rounded-full border-2 border-amber-500 border-t-transparent" />
      <span className="text-sm text-amber-700">입금 확인 대기 중...</span>
    </div>
  );
}
