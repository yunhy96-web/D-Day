import { StyleSheet, Text, View, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSiteHistory } from '@/hooks/useSiteHistory';
import { useSites } from '@/contexts';
import { colors, layout, spacing } from '@/styles';
import type { Change } from '@/types';

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function ChangeCard({ change }: { change: Change }) {
  const diff = change.newCount - change.oldCount;
  const diffColor = diff > 0 ? colors.success : colors.error;
  const diffText = diff > 0 ? `+${diff}` : `${diff}`;

  return (
    <View style={styles.changeCard}>
      <View style={styles.changeHeader}>
        <Text style={styles.changeTime}>{formatDateTime(change.detectedAt)}</Text>
        <View style={styles.changeBadge}>
          <Text style={[styles.changeDiff, { color: diffColor }]}>
            {change.oldCount} → {change.newCount} ({diffText})
          </Text>
        </View>
      </View>

      {change.addedProducts.length > 0 && (
        <View style={styles.changeSection}>
          <Text style={styles.addedLabel}>추가된 상품</Text>
          {change.addedProducts.map((p, i) => (
            <Text key={i} style={styles.addedProduct}>+ {p.name}</Text>
          ))}
        </View>
      )}

      {change.removedProducts.length > 0 && (
        <View style={styles.changeSection}>
          <Text style={styles.removedLabel}>삭제된 상품</Text>
          {change.removedProducts.map((p, i) => (
            <Text key={i} style={styles.removedProduct}>- {p.name}</Text>
          ))}
        </View>
      )}
    </View>
  );
}

export default function HistoryScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { sites } = useSites();
  const router = useRouter();
  const site = sites.find((s) => s.id === id);
  const { changes, isLoading, reload } = useSiteHistory(id ?? null);

  return (
    <SafeAreaView style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title} numberOfLines={1}>
          {site?.name ?? '이력'}
        </Text>
        <TouchableOpacity onPress={reload} style={styles.backButton}>
          <Ionicons name="refresh-outline" size={22} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {isLoading ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : changes.length === 0 ? (
        <View style={styles.centered}>
          <Ionicons name="documents-outline" size={48} color={colors.textTertiary} />
          <Text style={styles.emptyText}>아직 변동 이력이 없습니다</Text>
          <Text style={styles.emptySubtext}>상품 수가 변경되면 여기에 기록됩니다</Text>
        </View>
      ) : (
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {changes.map((change) => (
            <ChangeCard key={change.id} change={change} />
          ))}
        </ScrollView>
      )}
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
    paddingHorizontal: spacing[3],
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: spacing[3],
  },
  emptyText: {
    fontSize: 16,
    color: colors.textSecondary,
  },
  emptySubtext: {
    fontSize: 13,
    color: colors.textTertiary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: spacing[8],
  },
  changeCard: {
    backgroundColor: colors.cardBackground,
    borderRadius: layout.cardBorderRadius,
    padding: spacing[4],
    marginBottom: spacing[3],
    borderWidth: 1,
    borderColor: colors.border,
  },
  changeHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing[2],
  },
  changeTime: {
    fontSize: 13,
    color: colors.textTertiary,
  },
  changeBadge: {
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[1],
    borderRadius: spacing[1],
    backgroundColor: colors.backgroundLight,
  },
  changeDiff: {
    fontSize: 13,
    fontWeight: '700',
  },
  changeSection: {
    marginTop: spacing[2],
  },
  addedLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.success,
    marginBottom: spacing[1],
  },
  addedProduct: {
    fontSize: 13,
    color: colors.success,
    paddingLeft: spacing[2],
    paddingVertical: 2,
  },
  removedLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.error,
    marginBottom: spacing[1],
  },
  removedProduct: {
    fontSize: 13,
    color: colors.error,
    paddingLeft: spacing[2],
    paddingVertical: 2,
  },
});
