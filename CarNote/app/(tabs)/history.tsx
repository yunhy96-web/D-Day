import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord, MaintenanceRecord, RecordCategory } from '@/contexts';

// 날짜를 "2025년 12월 26일" 형식으로 표시
const formatDate = (date: Date): string => {
  return new Date(date).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
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

// 카테고리별 색상
const getCategoryColor = (category: string): string => {
  switch (category) {
    case '정비': return colors.success;
    case '주유': return colors.iconBlue;
    case '기타': return colors.textSecondary;
    default: return colors.primary;
  }
};

// 카테고리 옵션
const categoryOptions: { value: RecordCategory | 'all'; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: '정비', label: '정비' },
  { value: '주유', label: '주유' },
  { value: '기타', label: '기타' },
];

export default function HistoryScreen() {
  const router = useRouter();
  const { selectedCar, cars, selectCar } = useCar();
  const { getRecordsByCarId } = useRecord();

  // 필터 상태
  const [selectedCategory, setSelectedCategory] = useState<RecordCategory | 'all'>('all');
  const [showCarPicker, setShowCarPicker] = useState(false);

  // 선택된 차량의 기록 가져오기
  const allRecords = selectedCar ? getRecordsByCarId(selectedCar.id) : [];

  // 필터링된 기록
  const filteredRecords = useMemo(() => {
    if (selectedCategory === 'all') {
      return allRecords;
    }
    return allRecords.filter(record => record.category === selectedCategory);
  }, [allRecords, selectedCategory]);

  const handleRecordPress = (recordId: string) => {
    router.push(`/record/${recordId}`);
  };

  const handleCarSelect = (carId: string) => {
    selectCar(carId);
    setShowCarPicker(false);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <View style={styles.headerTop}>
          <TouchableOpacity onPress={() => router.push('/')} style={styles.backButton}>
            <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.title}>내역</Text>
          <TouchableOpacity onPress={() => router.push('/add')} style={styles.addButton}>
            <Ionicons name="add" size={26} color={colors.primary} />
          </TouchableOpacity>
        </View>
      </View>

      {/* 필터 영역 */}
      <View style={styles.filterContainer}>
        {/* 차량 선택 */}
        <TouchableOpacity
          style={styles.carSelector}
          onPress={() => setShowCarPicker(!showCarPicker)}
          activeOpacity={0.7}
        >
          <Ionicons name="car-sport" size={16} color={colors.primary} />
          <Text style={styles.carSelectorText} numberOfLines={1}>
            {selectedCar?.name || '차량 선택'}
          </Text>
          <Ionicons
            name={showCarPicker ? 'chevron-up' : 'chevron-down'}
            size={16}
            color="rgba(0,0,0,0.4)"
          />
        </TouchableOpacity>

        {/* 카테고리 필터 */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.categoryFilters}
        >
          {categoryOptions.map((option) => (
            <TouchableOpacity
              key={option.value}
              style={[
                styles.categoryChip,
                selectedCategory === option.value && styles.categoryChipActive,
              ]}
              onPress={() => setSelectedCategory(option.value)}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.categoryChipText,
                  selectedCategory === option.value && styles.categoryChipTextActive,
                ]}
              >
                {option.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 차량 선택 드롭다운 */}
      {showCarPicker && (
        <View style={styles.carPickerDropdown}>
          {cars.map((car) => (
            <TouchableOpacity
              key={car.id}
              style={[
                styles.carPickerItem,
                selectedCar?.id === car.id && styles.carPickerItemActive,
              ]}
              onPress={() => handleCarSelect(car.id)}
            >
              <Ionicons
                name="car-sport"
                size={18}
                color={selectedCar?.id === car.id ? colors.primary : 'rgba(0,0,0,0.5)'}
              />
              <Text
                style={[
                  styles.carPickerItemText,
                  selectedCar?.id === car.id && styles.carPickerItemTextActive,
                ]}
              >
                {car.name}
              </Text>
              {selectedCar?.id === car.id && (
                <Ionicons name="checkmark" size={18} color={colors.primary} />
              )}
            </TouchableOpacity>
          ))}
        </View>
      )}

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {!selectedCar ? (
          <View style={styles.emptyState}>
            <Ionicons name="car-outline" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>차량을 먼저 추가해주세요</Text>
            <TouchableOpacity
              style={styles.emptyButton}
              onPress={() => router.push('/settings')}
            >
              <Text style={styles.emptyButtonText}>차량 추가하기</Text>
            </TouchableOpacity>
          </View>
        ) : filteredRecords.length === 0 ? (
          <View style={styles.emptyState}>
            <Ionicons name="document-text-outline" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>
              {selectedCategory === 'all' ? '기록이 없습니다' : `${selectedCategory} 기록이 없습니다`}
            </Text>
            <TouchableOpacity
              style={styles.emptyButton}
              onPress={() => router.push('/add')}
            >
              <Text style={styles.emptyButtonText}>첫 기록 추가하기</Text>
            </TouchableOpacity>
          </View>
        ) : (
          filteredRecords.map((record) => (
            <RecordCard
              key={record.id}
              record={record}
              onPress={() => handleRecordPress(record.id)}
            />
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// 기록 카드 컴포넌트
function RecordCard({ record, onPress }: { record: MaintenanceRecord; onPress: () => void }) {
  const icon = getCategoryIcon(record.category);
  const iconColor = getCategoryColor(record.category);

  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.7}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.cardContent}>
        <View style={[styles.iconContainer, { backgroundColor: `${iconColor}20` }]}>
          <Ionicons name={icon} size={22} color={iconColor} />
        </View>
        <View style={styles.cardInfo}>
          <Text style={styles.cardType}>{record.type}</Text>
          <Text style={styles.cardDate}>{formatDate(record.date)}</Text>
        </View>
        {record.cost && (
          <Text style={styles.cardCost}>{record.cost.toLocaleString()}원</Text>
        )}
        <Ionicons name="chevron-forward" size={20} color="rgba(0,0,0,0.3)" />
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[3],
  },
  headerTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  backButton: {
    width: 40,
    height: 40,
    alignItems: 'flex-start',
    justifyContent: 'center',
    marginLeft: -spacing[2],
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  addButton: {
    width: 40,
    height: 40,
    alignItems: 'flex-end',
    justifyContent: 'center',
    marginRight: -spacing[2],
  },
  filterContainer: {
    paddingHorizontal: layout.screenPadding,
    gap: spacing[3],
    marginBottom: spacing[3],
  },
  carSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(212, 168, 75, 0.1)',
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: 12,
    gap: spacing[2],
    alignSelf: 'flex-start',
  },
  carSelectorText: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.primary,
    maxWidth: 120,
  },
  categoryFilters: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  categoryChip: {
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[2],
    borderRadius: 20,
    backgroundColor: 'rgba(255,255,255,0.15)',
  },
  categoryChipActive: {
    backgroundColor: colors.primary,
  },
  categoryChipText: {
    fontSize: 14,
    fontWeight: '600',
    color: 'rgba(255,255,255,0.7)',
  },
  categoryChipTextActive: {
    color: colors.background,
  },
  carPickerDropdown: {
    marginHorizontal: layout.screenPadding,
    marginBottom: spacing[3],
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
    overflow: 'hidden',
  },
  carPickerItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    gap: spacing[3],
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.05)',
  },
  carPickerItemActive: {
    backgroundColor: 'rgba(212, 168, 75, 0.1)',
  },
  carPickerItemText: {
    flex: 1,
    fontSize: 15,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.7)',
  },
  carPickerItemTextActive: {
    color: colors.primary,
    fontWeight: '600',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
    gap: spacing[3],
  },
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing[16],
    gap: spacing[3],
  },
  emptyText: {
    fontSize: 16,
    color: colors.textSecondary,
    marginTop: spacing[2],
  },
  emptyButton: {
    marginTop: spacing[2],
    paddingHorizontal: spacing[6],
    paddingVertical: spacing[3],
    backgroundColor: colors.primary,
    borderRadius: 12,
  },
  emptyButtonText: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.background,
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
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[4],
    gap: spacing[3],
  },
  iconContainer: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardInfo: {
    flex: 1,
    gap: 2,
  },
  cardType: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  cardDate: {
    fontSize: 13,
    color: 'rgba(0,0,0,0.45)',
  },
  cardCost: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.7)',
  },
});
