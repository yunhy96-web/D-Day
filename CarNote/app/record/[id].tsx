import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord } from '@/contexts';

// 날짜를 "2025년 12월 26일" 형식으로 표시
const formatDate = (date: Date): string => {
  return new Date(date).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export default function RecordDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { cars } = useCar();
  const { records, deleteRecord } = useRecord();

  const record = records.find(r => r.id === id);
  const car = record ? cars.find(c => c.id === record.carId) : null;

  const handleDelete = () => {
    Alert.alert(
      '기록 삭제',
      '이 기록을 삭제하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => {
            if (id) {
              deleteRecord(id);
              router.back();
            }
          },
        },
      ]
    );
  };

  if (!record) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.errorState}>
          <Text style={styles.errorText}>기록을 찾을 수 없습니다</Text>
          <TouchableOpacity onPress={() => router.back()}>
            <Text style={styles.backLink}>돌아가기</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.headerButton}>
          <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>기록 상세</Text>
        <TouchableOpacity onPress={handleDelete} style={styles.headerButton}>
          <Ionicons name="trash-outline" size={22} color={colors.error} />
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 상단 요약 */}
        <View style={styles.summarySection}>
          <Text style={styles.recordType}>{record.type}</Text>
          {record.cost && (
            <Text style={styles.recordCost}>{record.cost.toLocaleString()}원</Text>
          )}
        </View>

        {/* 기간 정보 */}
        <DetailCard title="기간 정보" icon="calendar-outline" iconColor={colors.iconBlue}>
          <DetailRow label="날짜" value={formatDate(record.date)} />
        </DetailCard>

        {/* 차량 정보 */}
        <DetailCard title="차량 정보" icon="car-sport" iconColor={colors.primary}>
          <DetailRow label="차량" value={car?.name || '-'} />
          {record.mileage && (
            <DetailRow label="누적 주행거리" value={`${record.mileage.toLocaleString()}km`} />
          )}
        </DetailCard>

        {/* 장소 정보 - 있을 때만 표시 */}
        {record.location && (
          <DetailCard title="장소 정보" icon="location-outline" iconColor={colors.iconBlue}>
            <DetailRow label="장소" value={record.location} />
          </DetailCard>
        )}

        {/* 주유량 - 주유 카테고리일 때만 표시 */}
        {record.category === '주유' && record.fuelAmount && (
          <DetailCard title="주유 정보" icon="water-outline" iconColor={colors.iconBlue}>
            <DetailRow label="주유량" value={`${record.fuelAmount}L`} />
          </DetailCard>
        )}

        {/* 비용 상세 */}
        {record.cost && (
          <DetailCard title="비용 상세" icon="cash-outline" iconColor={colors.iconGold}>
            <DetailRow
              label="총 비용"
              value={`${record.cost.toLocaleString()}원`}
              valueColor="rgba(0,0,0,0.7)"
              bold
            />
          </DetailCard>
        )}

        {/* 메모 */}
        {record.memo && (
          <DetailCard title="메모" icon="document-text-outline" iconColor={colors.textSecondary}>
            <Text style={styles.memoText}>{record.memo}</Text>
          </DetailCard>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// 상세 카드 컴포넌트
function DetailCard({
  title,
  icon,
  iconColor,
  children,
}: {
  title: string;
  icon: keyof typeof Ionicons.glyphMap;
  iconColor: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.card}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.cardContent}>
        <View style={styles.cardHeader}>
          <View style={[styles.cardIcon, { backgroundColor: `${iconColor}20` }]}>
            <Ionicons name={icon} size={18} color={iconColor} />
          </View>
          <Text style={styles.cardTitle}>{title}</Text>
        </View>
        <View style={styles.cardBody}>{children}</View>
      </View>
    </View>
  );
}

// 상세 행 컴포넌트
function DetailRow({
  label,
  value,
  valueColor,
  bold,
}: {
  label: string;
  value: string;
  valueColor?: string;
  bold?: boolean;
}) {
  return (
    <View style={styles.detailRow}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text
        style={[
          styles.detailValue,
          valueColor && { color: valueColor },
          bold && { fontWeight: '700' },
        ]}
      >
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[3],
  },
  headerButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 40,
    gap: spacing[4],
  },
  summarySection: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing[2],
  },
  recordType: {
    fontSize: 22,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  recordCost: {
    fontSize: 24,
    fontWeight: '700',
    color: colors.primary,
  },
  card: {
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  blurView: {
    ...StyleSheet.absoluteFillObject,
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255, 255, 255, 0.75)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 16,
  },
  cardContent: {
    padding: spacing[4],
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    marginBottom: spacing[3],
  },
  cardIcon: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  cardBody: {
    gap: spacing[2],
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing[1],
  },
  detailLabel: {
    fontSize: 15,
    color: 'rgba(0,0,0,0.5)',
  },
  detailValue: {
    fontSize: 15,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.8)',
  },
  memoText: {
    fontSize: 15,
    color: 'rgba(0,0,0,0.7)',
    lineHeight: 22,
  },
  errorState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[3],
  },
  errorText: {
    fontSize: 16,
    color: colors.textSecondary,
  },
  backLink: {
    fontSize: 16,
    color: colors.primary,
    fontWeight: '600',
  },
});
