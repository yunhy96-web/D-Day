import { Stack } from 'expo-router';
import { colors } from '@/styles';

export default function ArticleLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.white },
        headerTintColor: colors.primary,
        headerTitleStyle: { color: colors.textPrimary },
        headerShadowVisible: false,
        headerBackTitle: 'Back',
      }}
    >
      <Stack.Screen
        name="add"
        options={{
          title: 'New Article',
          presentation: 'modal',
        }}
      />
      <Stack.Screen
        name="[uuid]"
        options={{
          title: 'Edit Article',
        }}
      />
    </Stack>
  );
}
