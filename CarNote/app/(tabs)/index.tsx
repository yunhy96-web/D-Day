import React from 'react';
import { View, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { CarHeader, InfoCard } from '@/components/home';
import { SectionHeader, EmptyState } from '@/components/common';
import { colors, spacing, layout } from '@/styles';

export default function HomeScreen() {
  const router = useRouter();

  // TODO: Replace with actual data
  const car = {
    name: '내 차',
    mileage: 0,
  };

  const nextMaintenanceDate = null;
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
          carName={car.name}
          mileage={car.mileage}
          onCarSelect={() => {}}
          onMaintenancePress={goToAdd}
        />

        {/* Info Cards */}
        <View style={styles.infoCards}>
          <InfoCard
            icon="calendar-outline"
            iconColor={colors.iconBlue}
            label="다음 정비일"
            value={nextMaintenanceDate || '예정 없음'}
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
