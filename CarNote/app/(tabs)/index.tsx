import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { CarHeader, InfoCard } from '@/components/home';
import { SectionHeader, EmptyState } from '@/components/common';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord, MaintenanceRecord } from '@/contexts';

// 날짜를 "3일 전", "오늘" 등으로 표시
const formatRelativeDate = (date: Date): string => {
  const now = new Date();
  const diffTime = now.getTime() - new Date(date).getTime();
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return '오늘';
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
  return `${Math.floor(diffDays / 30)}개월 전`;
};

// 카테고리별 아이콘
const getCategoryIcon = (category: string): keyof typeof Ionicons.glyphMap => {
  switch (category) {
    case '정비': return 'construct-outline';
    case '주유': return 'water-outline';
    case '기타': return 'ellipsis-horizontal-outline';
    default: return 'document-outline';
  }
};

export default function HomeScreen() {
  const router = useRouter();
  const { selectedCar, cars } = useCar();
  const { getRecentRecords, getLastMaintenance, getMonthlyExpense } = useRecord();

  // 선택된 차량이 있으면 해당 정보, 없으면 기본값
  const carName = selectedCar?.name || (cars.length > 0 ? '차량을 선택하세요' : '차량을 추가하세요');
  const carMileage = selectedCar?.mileage || 0;

  // 선택된 차량의 기록 가져오기
  const recentRecords = selectedCar ? getRecentRecords(selectedCar.id, 3) : [];
  const lastMaintenanceRecord = selectedCar ? getLastMaintenance(selectedCar.id) : null;
  const monthlyExpense = selectedCar ? getMonthlyExpense(selectedCar.id) : 0;

  // 마지막 정비 표시 텍스트
  const lastMaintenanceValue = lastMaintenanceRecord?.type || '기록 없음';
  const lastMaintenanceSubtitle = lastMaintenanceRecord
    ? formatRelativeDate(lastMaintenanceRecord.date)
    : undefined;

  const nextMaintenanceDate = null; // TODO: 다가오는 일정 기능

  const goToAdd = () => {
    router.push('/add');
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Car Header */}
        <CarHeader
          carName={carName}
          mileage={carMileage}
          onCarSelect={() => router.push('/settings')}
          onMaintenancePress={goToAdd}
        />

        {/* Info Cards */}
        <View style={styles.infoCards}>
          <InfoCard
            icon="calendar-outline"
            iconColor={colors.iconBlue}
            label="다가오는 일정"
            value={nextMaintenanceDate || '예정 없음'}
          />
          <InfoCard
            icon="construct-outline"
            iconColor={colors.success}
            label="마지막 정비"
            value={lastMaintenanceValue}
            subtitle={lastMaintenanceSubtitle}
          />
          <InfoCard
            icon="cash-outline"
            iconColor={colors.iconGold}
            label="이번 달 지출"
            value={`${monthlyExpense.toLocaleString()}원`}
          />
        </View>

        {/* Recent Maintenance */}
        <SectionHeader title="최근 기록" />

        {recentRecords.length === 0 ? (
          <EmptyState
            title="기록이 없습니다."
            description="여기를 눌러 첫 번째 기록을 추가해보세요."
            onPress={goToAdd}
          />
        ) : (
          <View style={styles.recordList}>
            {recentRecords.map((record) => (
              <RecordItem
                key={record.id}
                record={record}
                onPress={() => router.push(`/record/${record.id}`)}
              />
            ))}
            {/* 기록 추가하기 카드 */}
            <AddRecordCard onPress={goToAdd} />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// 기록 아이템 컴포넌트
function RecordItem({ record, onPress }: { record: MaintenanceRecord; onPress: () => void }) {
  const icon = getCategoryIcon(record.category);

  return (
    <TouchableOpacity style={styles.recordItem} onPress={onPress} activeOpacity={0.7}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.recordContent}>
        <View style={[styles.recordIcon, { backgroundColor: `${colors.primary}20` }]}>
          <Ionicons name={icon} size={20} color={colors.primary} />
        </View>
        <View style={styles.recordInfo}>
          <Text style={styles.recordType}>{record.type}</Text>
          <Text style={styles.recordDate}>{formatRelativeDate(record.date)}</Text>
        </View>
        {record.cost && (
          <Text style={styles.recordCost}>{record.cost.toLocaleString()}원</Text>
        )}
      </View>
    </TouchableOpacity>
  );
}

// 기록 추가하기 카드 컴포넌트
function AddRecordCard({ onPress }: { onPress: () => void }) {
  return (
    <TouchableOpacity style={styles.addRecordCard} onPress={onPress} activeOpacity={0.7}>
      <View style={styles.addRecordContent}>
        <View style={styles.addRecordIcon}>
          <Ionicons name="add" size={24} color={colors.primary} />
        </View>
        <Text style={styles.addRecordText}>기록 추가하기</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 120, // 리퀴드 탭바 높이 + 여유 공간
  },
  infoCards: {
    flexDirection: 'row',
    gap: spacing[3],
    paddingHorizontal: layout.screenPadding,
  },
  recordList: {
    paddingHorizontal: layout.screenPadding,
    gap: spacing[3],
  },
  recordItem: {
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
  recordContent: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[4],
    gap: spacing[3],
  },
  recordIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  recordInfo: {
    flex: 1,
    gap: 2,
  },
  recordType: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  recordDate: {
    fontSize: 13,
    color: 'rgba(0,0,0,0.45)',
  },
  recordCost: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.primary,
  },
  addRecordCard: {
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(212, 168, 75, 0.3)',
    borderStyle: 'dashed',
    backgroundColor: 'rgba(212, 168, 75, 0.05)',
  },
  addRecordContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing[4],
    gap: spacing[2],
  },
  addRecordIcon: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  addRecordText: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.primary,
  },
});
