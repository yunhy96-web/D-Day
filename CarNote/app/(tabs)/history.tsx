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

type ViewMode = 'list' | 'calendar';

export default function HistoryScreen() {
  const router = useRouter();
  const { selectedCar, cars, selectCar } = useCar();
  const { getRecordsByCarId } = useRecord();

  // 필터 상태
  const [selectedCategory, setSelectedCategory] = useState<RecordCategory | 'all'>('all');
  const [showCarPicker, setShowCarPicker] = useState(false);
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [currentMonth, setCurrentMonth] = useState(new Date());

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
          <View style={styles.headerRight}>
            {/* 뷰 모드 토글 */}
            <View style={styles.viewToggle}>
              <TouchableOpacity
                style={[styles.viewToggleButton, viewMode === 'list' && styles.viewToggleButtonActive]}
                onPress={() => setViewMode('list')}
              >
                <Ionicons
                  name="list"
                  size={18}
                  color={viewMode === 'list' ? colors.primary : 'rgba(255,255,255,0.5)'}
                />
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.viewToggleButton, viewMode === 'calendar' && styles.viewToggleButtonActive]}
                onPress={() => setViewMode('calendar')}
              >
                <Ionicons
                  name="calendar"
                  size={18}
                  color={viewMode === 'calendar' ? colors.primary : 'rgba(255,255,255,0.5)'}
                />
              </TouchableOpacity>
            </View>
            <TouchableOpacity onPress={() => router.push('/add')} style={styles.addButton}>
              <Ionicons name="add" size={26} color={colors.primary} />
            </TouchableOpacity>
          </View>
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

      {viewMode === 'list' ? (
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
      ) : (
        <CalendarView
          records={filteredRecords}
          currentMonth={currentMonth}
          onMonthChange={setCurrentMonth}
          onRecordPress={handleRecordPress}
          onAddPress={() => router.push('/add')}
          selectedCar={selectedCar}
          router={router}
        />
      )}
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

// 캘린더 뷰 컴포넌트
interface CalendarViewProps {
  records: MaintenanceRecord[];
  currentMonth: Date;
  onMonthChange: (date: Date) => void;
  onRecordPress: (recordId: string) => void;
  onAddPress: () => void;
  selectedCar: { id: string; name: string } | null;
  router: ReturnType<typeof useRouter>;
}

function CalendarView({
  records,
  currentMonth,
  onMonthChange,
  onRecordPress,
  onAddPress,
  selectedCar,
  router,
}: CalendarViewProps) {
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  // 현재 월의 날짜 계산
  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();

  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);
  const startDayOfWeek = firstDayOfMonth.getDay(); // 0 = 일요일
  const daysInMonth = lastDayOfMonth.getDate();

  // 이전/다음 달 이동
  const goToPrevMonth = () => {
    onMonthChange(new Date(year, month - 1, 1));
    setSelectedDate(null);
  };

  const goToNextMonth = () => {
    onMonthChange(new Date(year, month + 1, 1));
    setSelectedDate(null);
  };

  // 날짜별 기록 매핑
  const recordsByDate = useMemo(() => {
    const map: { [key: string]: MaintenanceRecord[] } = {};
    records.forEach(record => {
      const date = new Date(record.date);
      const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      if (!map[key]) map[key] = [];
      map[key].push(record);
    });
    return map;
  }, [records]);

  // 선택된 날짜의 기록
  const selectedDateRecords = useMemo(() => {
    if (!selectedDate) return [];
    const key = `${selectedDate.getFullYear()}-${selectedDate.getMonth()}-${selectedDate.getDate()}`;
    return recordsByDate[key] || [];
  }, [selectedDate, recordsByDate]);

  // 오늘 날짜
  const today = new Date();
  const isToday = (day: number) =>
    today.getFullYear() === year &&
    today.getMonth() === month &&
    today.getDate() === day;

  // 선택된 날짜인지 확인
  const isSelected = (day: number) =>
    selectedDate &&
    selectedDate.getFullYear() === year &&
    selectedDate.getMonth() === month &&
    selectedDate.getDate() === day;

  // 날짜 선택
  const handleDayPress = (day: number) => {
    const newDate = new Date(year, month, day);
    if (selectedDate && isSelected(day)) {
      setSelectedDate(null);
    } else {
      setSelectedDate(newDate);
    }
  };

  // 빈 셀 생성 (월 시작 전)
  const emptyCells = Array(startDayOfWeek).fill(null);
  // 날짜 셀 생성
  const dayCells = Array.from({ length: daysInMonth }, (_, i) => i + 1);

  const weekDays = ['일', '월', '화', '수', '목', '금', '토'];

  if (!selectedCar) {
    return (
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
    );
  }

  return (
    <ScrollView style={styles.scrollView} contentContainerStyle={styles.calendarScrollContent}>
      {/* 월 네비게이션 */}
      <View style={styles.monthNav}>
        <TouchableOpacity onPress={goToPrevMonth} style={styles.monthNavButton}>
          <Ionicons name="chevron-back" size={24} color="rgba(255,255,255,0.8)" />
        </TouchableOpacity>
        <Text style={styles.monthTitle}>
          {year}년 {month + 1}월
        </Text>
        <TouchableOpacity onPress={goToNextMonth} style={styles.monthNavButton}>
          <Ionicons name="chevron-forward" size={24} color="rgba(255,255,255,0.8)" />
        </TouchableOpacity>
      </View>

      {/* 캘린더 그리드 */}
      <View style={styles.calendarCard}>
        <BlurView intensity={40} tint="light" style={styles.blurView}>
          <View style={styles.glassOverlay} />
        </BlurView>
        <View style={styles.calendarContent}>
          {/* 요일 헤더 */}
          <View style={styles.weekDaysRow}>
            {weekDays.map((day, index) => (
              <View key={day} style={styles.weekDayCell}>
                <Text style={[
                  styles.weekDayText,
                  index === 0 && styles.sundayText,
                  index === 6 && styles.saturdayText,
                ]}>
                  {day}
                </Text>
              </View>
            ))}
          </View>

          {/* 날짜 그리드 */}
          <View style={styles.daysGrid}>
            {emptyCells.map((_, index) => (
              <View key={`empty-${index}`} style={styles.dayCell} />
            ))}
            {dayCells.map(day => {
              const dateKey = `${year}-${month}-${day}`;
              const hasRecords = !!recordsByDate[dateKey];
              const dayOfWeek = (startDayOfWeek + day - 1) % 7;

              return (
                <TouchableOpacity
                  key={day}
                  style={[
                    styles.dayCell,
                    isToday(day) && styles.todayCell,
                    isSelected(day) && styles.selectedDayCell,
                  ]}
                  onPress={() => handleDayPress(day)}
                  activeOpacity={0.7}
                >
                  <Text style={[
                    styles.dayText,
                    dayOfWeek === 0 && styles.sundayText,
                    dayOfWeek === 6 && styles.saturdayText,
                    isToday(day) && styles.todayText,
                    isSelected(day) && styles.selectedDayText,
                  ]}>
                    {day}
                  </Text>
                  <View style={styles.recordDotsContainer}>
                    {hasRecords && recordsByDate[dateKey].slice(0, 3).map((record, idx) => (
                      <View
                        key={idx}
                        style={[styles.recordDot, { backgroundColor: getCategoryColor(record.category) }]}
                      />
                    ))}
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      </View>

      {/* 선택된 날짜의 기록 */}
      {selectedDate && (
        <View style={styles.selectedDateSection}>
          <Text style={styles.selectedDateTitle}>
            {selectedDate.getMonth() + 1}월 {selectedDate.getDate()}일 기록
          </Text>
          {selectedDateRecords.length === 0 ? (
            <TouchableOpacity style={styles.noRecordCard} onPress={onAddPress}>
              <Ionicons name="add-circle-outline" size={24} color={colors.primary} />
              <Text style={styles.noRecordText}>기록 추가하기</Text>
            </TouchableOpacity>
          ) : (
            selectedDateRecords.map(record => (
              <RecordCard
                key={record.id}
                record={record}
                onPress={() => onRecordPress(record.id)}
              />
            ))
          )}
        </View>
      )}
    </ScrollView>
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
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  viewToggle: {
    flexDirection: 'row',
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: 10,
    padding: 4,
  },
  viewToggleButton: {
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[1],
    borderRadius: 8,
  },
  viewToggleButtonActive: {
    backgroundColor: 'rgba(255,255,255,0.9)',
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
  // 캘린더 스타일
  calendarScrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
  },
  monthNav: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing[3],
  },
  monthNavButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  monthTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  calendarCard: {
    borderRadius: 20,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
  },
  calendarContent: {
    padding: spacing[3],
  },
  weekDaysRow: {
    flexDirection: 'row',
    marginBottom: spacing[2],
  },
  weekDayCell: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: spacing[2],
  },
  weekDayText: {
    fontSize: 13,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.5)',
  },
  sundayText: {
    color: '#E53935',
  },
  saturdayText: {
    color: '#1E88E5',
  },
  daysGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  dayCell: {
    width: '14.28%',
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing[1],
  },
  todayCell: {
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    borderRadius: 12,
  },
  selectedDayCell: {
    backgroundColor: colors.primary,
    borderRadius: 12,
  },
  dayText: {
    fontSize: 15,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.8)',
  },
  todayText: {
    fontWeight: '700',
    color: colors.primary,
  },
  selectedDayText: {
    fontWeight: '700',
    color: '#fff',
  },
  recordDotsContainer: {
    flexDirection: 'row',
    gap: 3,
    marginTop: 2,
    height: 5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  recordDot: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
  },
  selectedDateSection: {
    marginTop: spacing[4],
    gap: spacing[3],
  },
  selectedDateTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  noRecordCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    paddingVertical: spacing[5],
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(212, 168, 75, 0.3)',
    borderStyle: 'dashed',
  },
  noRecordText: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.primary,
  },
});
