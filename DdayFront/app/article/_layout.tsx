import { Stack } from 'expo-router';
import { useTheme } from '@/contexts';

export default function ArticleLayout() {
  const { colors } = useTheme();

  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.background },
        headerTintColor: colors.primary,
        headerTitleStyle: { color: colors.textPrimary },
        headerShadowVisible: false,
        headerBackTitle: 'Back',
      }}
    >
      <Stack.Screen
        name="add"
        options={{
          title: 'New Post',
          presentation: 'modal',
        }}
      />
      <Stack.Screen
        name="[uuid]"
        options={{
          headerShown: false,
        }}
      />
      <Stack.Screen
        name="edit/[uuid]"
        options={{
          title: 'Edit Post',
        }}
      />
    </Stack>
  );
}
