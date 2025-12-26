import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { CarProvider } from '@/contexts';

export default function RootLayout() {
  return (
    <CarProvider>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(tabs)" />
      </Stack>
    </CarProvider>
  );
}
