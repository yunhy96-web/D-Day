import { StyleSheet, Text, View, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSites } from '@/contexts';
import { colors, layout, spacing, shadows } from '@/styles';

export default function ManageScreen() {
  const { sites, deleteSite } = useSites();
  const router = useRouter();

  const handleDelete = (id: string, name: string) => {
    Alert.alert(
      '사이트 삭제',
      `"${name}"을(를) 삭제하시겠습니까?\n관련 이력도 모두 삭제됩니다.`,
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => deleteSite(id),
        },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>사이트 관리</Text>
        <TouchableOpacity
          onPress={() => router.push('/add-site' as any)}
          style={styles.addButton}
        >
          <Ionicons name="add-outline" size={26} color={colors.primary} />
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {sites.map((site) => (
          <View key={site.id} style={styles.siteCard}>
            <TouchableOpacity
              style={styles.siteInfo}
              onPress={() => router.push(`/site/${site.id}` as any)}
              activeOpacity={0.7}
            >
              <Text style={styles.siteName}>{site.name}</Text>
              <Text style={styles.siteUrl} numberOfLines={1}>{site.url}</Text>
              <Text style={styles.siteMeta}>
                셀렉터: {site.selector} · {site.refreshInterval}초 주기
              </Text>
            </TouchableOpacity>
            <View style={styles.siteActions}>
              <TouchableOpacity
                onPress={() => router.push(`/add-site?id=${site.id}` as any)}
                style={styles.actionButton}
              >
                <Ionicons name="create-outline" size={20} color={colors.textSecondary} />
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => handleDelete(site.id, site.name)}
                style={styles.actionButton}
              >
                <Ionicons name="trash-outline" size={20} color={colors.error} />
              </TouchableOpacity>
            </View>
          </View>
        ))}

        {sites.length === 0 && (
          <View style={styles.empty}>
            <Ionicons name="globe-outline" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>등록된 사이트가 없습니다</Text>
          </View>
        )}
      </ScrollView>

      {/* 추가 버튼 */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => router.push('/add-site' as any)}
        activeOpacity={0.7}
      >
        <Ionicons name="add" size={28} color={colors.white} />
        <Text style={styles.fabText}>사이트 추가</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: layout.screenPadding,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing[4],
    marginBottom: spacing[5],
  },
  backButton: {
    padding: spacing[1],
  },
  title: {
    flex: 1,
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
    textAlign: 'center',
  },
  addButton: {
    padding: spacing[1],
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: spacing[16],
  },
  siteCard: {
    flexDirection: 'row',
    backgroundColor: colors.cardBackground,
    borderRadius: layout.cardBorderRadius,
    padding: spacing[4],
    marginBottom: spacing[3],
    borderWidth: 1,
    borderColor: colors.border,
  },
  siteInfo: {
    flex: 1,
    gap: spacing[1],
  },
  siteName: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  siteUrl: {
    fontSize: 12,
    color: colors.textTertiary,
  },
  siteMeta: {
    fontSize: 11,
    color: colors.textTertiary,
    marginTop: spacing[1],
  },
  siteActions: {
    justifyContent: 'center',
    gap: spacing[3],
    paddingLeft: spacing[3],
  },
  actionButton: {
    padding: spacing[1],
  },
  empty: {
    alignItems: 'center',
    marginTop: spacing[16],
    gap: spacing[3],
  },
  emptyText: {
    fontSize: 16,
    color: colors.textSecondary,
  },
  fab: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    backgroundColor: colors.primary,
    borderRadius: layout.buttonBorderRadius,
    paddingVertical: spacing[4],
    marginBottom: spacing[6],
    ...shadows.md,
  },
  fabText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.white,
  },
});
