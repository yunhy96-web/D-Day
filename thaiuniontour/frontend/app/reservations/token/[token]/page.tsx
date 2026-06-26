export const dynamic = 'force-dynamic';

import { notFound, redirect } from 'next/navigation';
import { getReservationByToken } from '@/lib/api';

interface Props {
  params: { token: string };
}

export default async function ReservationTokenPage({ params }: Props) {
  const reservation = await getReservationByToken(params.token);

  if (!reservation) {
    notFound();
  }

  redirect(`/reservations/${reservation.id}`);
}
