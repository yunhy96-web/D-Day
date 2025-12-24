import { Tabs } from 'expo-router';
import React from 'react';

import { useTheme } from '@/contexts';

export default function TabLayout() {
  const { colors: _colors } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: { display: 'none' },
      }}>
      <Tabs.Screen name="index" />
      <Tabs.Screen name="explore" options={{ href: null }} />
      <Tabs.Screen name="add" options={{ href: null }} />
      <Tabs.Screen name="profile" options={{ href: null }} />
    </Tabs>
  );
}
