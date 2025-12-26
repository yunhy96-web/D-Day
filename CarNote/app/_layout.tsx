import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { CarProvider, RecordProvider } from '@/contexts';

export default function RootLayout() {
  return (
    <CarProvider>
      <RecordProvider>
        <StatusBar style="light" />
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(tabs)" />
        </Stack>
      </RecordProvider>
    </CarProvider>
  );
}
