import { useEffect } from 'react';
import { ActivityIndicator, View, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { useSites } from '@/contexts';
import { colors } from '@/styles';

export default function IndexScreen() {
  const { sites, isLoading } = useSites();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && sites.length > 0) {
      router.replace(`/site/${sites[0].id}` as any);
    }
  }, [isLoading, sites, router]);

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color={colors.primary} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
