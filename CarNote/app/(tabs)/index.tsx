import React from 'react';
import { View, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { CarHeader, InfoCard } from '@/components/home';
import { SectionHeader, EmptyState } from '@/components/common';
import { colors, spacing, layout } from '@/styles';
import { useCar } from '@/contexts';

export default function HomeScreen() {
  const router = useRouter();
  const { selectedCar, cars } = useCar();

  // 선택된 차량이 있으면 해당 정보, 없으면 기본값
  const carName = selectedCar?.name || (cars.length > 0 ? '차량을 선택하세요' : '차량을 추가하세요');
  const carMileage = selectedCar?.mileage || 0;

  const nextMaintenanceDate = null;
  const lastMaintenance = null; // TODO: 최근 정비 기록에서 가져오기 (예: "엔진오일 · 3일 전")
  const monthlyExpense = 0;

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
            value={lastMaintenance || '기록 없음'}
          />
          <InfoCard
            icon="cash-outline"
            iconColor={colors.iconGold}
            label="이번 달 지출"
            value={`${monthlyExpense.toLocaleString()}원`}
          />
        </View>

        {/* Recent Maintenance */}
        <SectionHeader title="최근 정비 내역" />

        <EmptyState
          title="정비 기록이 없습니다."
          description="여기를 눌러 첫 번째 정비 기록을 추가해보세요."
          onPress={goToAdd}
        />
      </ScrollView>
    </SafeAreaView>
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
});
