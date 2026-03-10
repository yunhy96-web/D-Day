import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SiteProvider } from '@/contexts';

export default function RootLayout() {
  return (
    <SiteProvider>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false }} />
    </SiteProvider>
  );
}
