import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SiteProvider } from '@/contexts';
import { ensureNotificationPermission } from '@/utils/pushNotifications';

export default function RootLayout() {
  useEffect(() => {
    ensureNotificationPermission();
  }, []);

  return (
    <SiteProvider>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false }} />
    </SiteProvider>
  );
}
