export const dynamic = 'force-dynamic';

import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import { getReservation } from '@/lib/api';
import PaymentStatusPoller from '@/components/PaymentStatusPoller';

interface Props {
  params: { id: string };
}

export const metadata: Metadata = {
  title: '예약 상세 | ThaiUnionTour',
};

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

function formatDateTime(isoStr: string): string {
  const d = new Date(isoStr);
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
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

export default async function ReservationDetailPage({ params }: Props) {
  const reservation = await getReservation(params.id);

  if (!reservation) {
    notFound();
  }

  const statusLabel = STATUS_LABEL[reservation.status] ?? reservation.status;
  const statusStyle = STATUS_STYLE[reservation.status] ?? 'bg-cream-200 text-ink-500';

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="mb-6 text-xl font-bold text-ink-900">예약 상세</h1>

      {/* 상태 + 예약번호 */}
      <div className="mb-4 flex items-center justify-between">
        <span className="text-sm text-ink-500">
          예약번호 <span className="font-semibold text-ink-800">{reservation.reservationNo}</span>
        </span>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle}`}>
          {statusLabel}
        </span>
      </div>

      {/* 기본 정보 카드 */}
      <div className="rounded-2xl border border-cream-200 bg-white p-6 shadow-sm space-y-4">
        <InfoRow label="출발일" value={formatDate(reservation.saleDate)} />
        <InfoRow label="인원" value={`${reservation.quantity}명`} />
        <InfoRow label="결제 금액" value={formatPrice(reservation.amount)} highlight />
        <InfoRow label="결제 방법" value="무통장 입금(계좌이체)" />
        <InfoRow label="예약자" value={reservation.reserver.name} />
        <InfoRow label="이메일" value={reservation.reserver.email} />
        <InfoRow label="연락처" value={reservation.reserver.phone} />
        {reservation.paymentDeadline && (
          <InfoRow label="입금 기한" value={formatDateTime(reservation.paymentDeadline)} />
        )}
        {reservation.confirmedAt && (
          <InfoRow label="확정일시" value={formatDateTime(reservation.confirmedAt)} />
        )}
        {reservation.cancelledAt && (
          <InfoRow label="취소일시" value={formatDateTime(reservation.cancelledAt)} />
        )}
      </div>

      {/* PENDING 상태: 은행 입금 안내 */}
      {reservation.status === 'PENDING' && reservation.bankInfo && (
        <div className="mt-6 rounded-2xl border-2 border-brand-300 bg-brand-50 p-6">
          <h2 className="mb-4 text-base font-bold text-ink-900">입금 안내</h2>
          <div className="space-y-3">
            <BankRow label="은행" value={reservation.bankInfo.bank} />
            <BankRow label="계좌번호" value={reservation.bankInfo.account} />
            <BankRow label="예금주" value={reservation.bankInfo.holder} />
            <BankRow
              label="입금 기한"
              value={formatDateTime(reservation.bankInfo.deadline)}
            />
          </div>
          <p className="mt-4 text-sm text-ink-600 leading-relaxed">
            입금 후 운영자 확인 시 예약이 확정됩니다. 입금자명은 예약자 성함과 동일하게
            입력해주세요.
          </p>
        </div>
      )}

      {/* 결제 상태 폴링 */}
      {reservation.status === 'PENDING' && (
        <div className="mt-4">
          <PaymentStatusPoller
            reservationId={reservation.id}
            initialStatus="PENDING"
          />
        </div>
      )}
      {reservation.status === 'CONFIRMED' && (
        <div className="mt-4">
          <PaymentStatusPoller
            reservationId={reservation.id}
            initialStatus="PAID"
          />
        </div>
      )}

      {/* 예약 조회 링크 */}
      <div className="mt-8 text-center">
        <a
          href="/reservations/lookup"
          className="text-sm text-brand-600 underline underline-offset-2 hover:text-brand-700"
        >
          예약 조회 페이지로 이동
        </a>
      </div>
    </div>
  );
}

function InfoRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="shrink-0 text-sm text-ink-500">{label}</span>
      <span className={`text-right text-sm ${highlight ? 'font-bold text-brand-600 text-base' : 'text-ink-800'}`}>
        {value}
      </span>
    </div>
  );
}

function BankRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="shrink-0 text-sm font-medium text-ink-600">{label}</span>
      <span className="text-right text-sm font-semibold text-ink-900">{value}</span>
    </div>
  );
}
