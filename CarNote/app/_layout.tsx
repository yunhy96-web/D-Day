import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { CarProvider, RecordProvider, InsuranceProvider } from '@/contexts';

export default function RootLayout() {
  return (
    <CarProvider>
      <RecordProvider>
        <InsuranceProvider>
          <StatusBar style="light" />
          <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="(tabs)" />
          </Stack>
        </InsuranceProvider>
      </RecordProvider>
    </CarProvider>
  );
}
