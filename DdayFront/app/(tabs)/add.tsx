import { Redirect } from 'expo-router';

// This screen is just a placeholder for the Write tab
// The actual navigation is handled by tabPress listener in _layout.tsx
export default function AddScreen() {
  return <Redirect href="/article/add" />;
}
