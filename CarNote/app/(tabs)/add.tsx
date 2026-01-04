import React, { useState, useCallback, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  LayoutAnimation,
  UIManager,
  Modal,
} from 'react-native';
import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';

// Android에서 LayoutAnimation 활성화
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}
import { SafeAreaView } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord, RecordCategory } from '@/contexts';
import { useRouter, useLocalSearchParams } from 'expo-router';

const CATEGORIES = {
  정비: ['엔진오일', '타이어', '브레이크', '에어필터', '배터리', '냉각수'],
  주유: ['주유'],
  기타: ['세차', '보험', '검사'],
};

// 선택한 항목이 어떤 카테고리에 속하는지 판별
const getSelectedCategory = (type: string): '정비' | '주유' | '기타' | null => {
  if (!type) return null;
  if (CATEGORIES.정비.includes(type)) return '정비';
  if (CATEGORIES.주유.includes(type)) return '주유';
  if (CATEGORIES.기타.includes(type)) return '기타';
  return null;
};

export default function AddScreen() {
  const router = useRouter();
  const { date: dateParam } = useLocalSearchParams<{ date?: string }>();
  const { cars, selectedCar, selectCar, updateCar } = useCar();
  const { addRecord } = useRecord();
  const [maintenanceType, setMaintenanceType] = useState('');
  const [cost, setCost] = useState('');
  const [mileage, setMileage] = useState('');
  const [location, setLocation] = useState('');
  const [fuelAmount, setFuelAmount] = useState('');
  const [memo, setMemo] = useState('');
  const [date, setDate] = useState(new Date());
  const [carDropdownOpen, setCarDropdownOpen] = useState(false);

  // 쿼리 파라미터로 전달된 날짜가 있으면 해당 날짜로 설정
  useEffect(() => {
    if (dateParam) {
      setDate(new Date(dateParam));
    }
  }, [dateParam]);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [datePickerExpanded, setDatePickerExpanded] = useState(false);

  // 날짜 제한: 오늘 기준 앞뒤로 1년
  const today = new Date();
  const minDate = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());
  const maxDate = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());

  const selectedCategory = getSelectedCategory(maintenanceType);

  // 폼 초기화
  const resetForm = () => {
    setMaintenanceType('');
    setCost('');
    setMileage('');
    setLocation('');
    setFuelAmount('');
    setMemo('');
    setDate(new Date());
  };

  // 카테고리 선택 시 애니메이션과 함께 상태 변경
  const handleCategorySelect = useCallback((type: string) => {
    LayoutAnimation.configureNext(
      LayoutAnimation.create(
        250,
        LayoutAnimation.Types.easeInEaseOut,
        LayoutAnimation.Properties.opacity
      )
    );
    setMaintenanceType(type);
  }, []);

  const handleSave = () => {
    // 필수 검증: 차량과 카테고리 선택 필요
    if (!selectedCar) {
      alert('차량을 선택해주세요');
      return;
    }
    if (!maintenanceType) {
      alert('카테고리를 선택해주세요');
      return;
    }

    const recordMileage = mileage ? Number(mileage) : undefined;

    // 기록 저장
    addRecord({
      carId: selectedCar.id,
      category: selectedCategory as RecordCategory,
      type: maintenanceType,
      date: date,
      cost: cost ? Number(cost) : undefined,
      mileage: recordMileage,
      location: location || undefined,
      fuelAmount: fuelAmount ? Number(fuelAmount) : undefined,
      memo: memo || undefined,
    });

    // 주행거리가 입력되었고, 기존 차량 주행거리보다 크면 차량 주행거리 업데이트
    if (recordMileage && recordMileage > selectedCar.mileage) {
      updateCar(selectedCar.id, { mileage: recordMileage });
    }

    // 폼 초기화 및 홈으로 이동
    resetForm();
    router.push('/');
  };

  const formatDate = (d: Date) => {
    return d.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  // 날짜 변경 핸들러
  const onDateChange = (event: DateTimePickerEvent, selectedDate?: Date) => {
    if (Platform.OS === 'android') {
      setShowDatePicker(false);
      setDatePickerExpanded(false);
    }
    if (selectedDate) {
      setDate(selectedDate);
    }
  };

  // 날짜 선택기 토글
  const toggleDatePicker = useCallback(() => {
    LayoutAnimation.configureNext(
      LayoutAnimation.create(
        300,
        LayoutAnimation.Types.easeInEaseOut,
        LayoutAnimation.Properties.opacity
      )
    );
    setDatePickerExpanded(!datePickerExpanded);
  }, [datePickerExpanded]);

  // 짧은 날짜 포맷
  const formatShortDate = (d: Date) => {
    return d.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  // 차량 드롭다운 토글
  const toggleCarDropdown = useCallback(() => {
    LayoutAnimation.configureNext(
      LayoutAnimation.create(
        250,
        LayoutAnimation.Types.easeInEaseOut,
        LayoutAnimation.Properties.opacity
      )
    );
    setCarDropdownOpen(!carDropdownOpen);
  }, [carDropdownOpen]);

  // 차량 선택 핸들러
  const handleCarSelect = (carId: string) => {
    selectCar(carId);
    LayoutAnimation.configureNext(
      LayoutAnimation.create(
        250,
        LayoutAnimation.Types.easeInEaseOut,
        LayoutAnimation.Properties.opacity
      )
    );
    setCarDropdownOpen(false);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.push('/')} style={styles.backButton}>
            <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>기록 추가</Text>
          <TouchableOpacity onPress={handleSave} style={styles.saveButton}>
            <Text style={styles.saveButtonText}>저장</Text>
          </TouchableOpacity>
        </View>

        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* 정비 날짜 */}
          <GlassCard>
            <CardHeader icon="calendar-outline" iconColor={colors.iconBlue} title="날짜" />
            <TouchableOpacity
              style={styles.dateSelector}
              onPress={() => Platform.OS === 'android' ? setShowDatePicker(true) : setDatePickerExpanded(true)}
              activeOpacity={0.7}
            >
              <Text style={styles.dateLabelText}>날짜</Text>
              <View style={styles.dateValueContainer}>
                <Text style={styles.dateValueText}>{formatShortDate(date)}</Text>
                <Ionicons name="calendar" size={16} color={colors.primary} />
              </View>
            </TouchableOpacity>
          </GlassCard>

          {/* Android: 시스템 다이얼로그 */}
          {Platform.OS === 'android' && showDatePicker && (
            <DateTimePicker
              value={date}
              mode="date"
              display="default"
              onChange={onDateChange}
              minimumDate={minDate}
              maximumDate={maxDate}
            />
          )}

          {/* 차량 선택 */}
          <GlassCard>
            <CardHeader icon="car-sport" iconColor={colors.primary} title="차량 선택" />
            <TouchableOpacity style={styles.selector} onPress={toggleCarDropdown}>
              <Text style={styles.selectorText}>
                {selectedCar ? selectedCar.name : (cars.length > 0 ? '차량을 선택하세요' : '차량을 추가하세요')}
              </Text>
              <Ionicons
                name={carDropdownOpen ? "chevron-up" : "chevron-down"}
                size={18}
                color={colors.primary}
              />
            </TouchableOpacity>

            {/* 차량 드롭다운 목록 */}
            {carDropdownOpen && (
              <View style={styles.carDropdownList}>
                {cars.length === 0 ? (
                  <View style={styles.carDropdownEmpty}>
                    <Ionicons name="car-outline" size={24} color="rgba(0,0,0,0.3)" />
                    <Text style={styles.carDropdownEmptyText}>등록된 차량이 없습니다</Text>
                  </View>
                ) : (
                  cars.map((car, index) => (
                    <TouchableOpacity
                      key={car.id}
                      style={[
                        styles.carDropdownItem,
                        selectedCar?.id === car.id && styles.carDropdownItemActive,
                        index < cars.length - 1 && styles.carDropdownItemBorder,
                      ]}
                      onPress={() => handleCarSelect(car.id)}
                      activeOpacity={0.7}
                    >
                      <Ionicons
                        name="car-sport"
                        size={18}
                        color={selectedCar?.id === car.id ? colors.primary : 'rgba(0,0,0,0.4)'}
                      />
                      <Text style={[
                        styles.carDropdownItemText,
                        selectedCar?.id === car.id && styles.carDropdownItemTextActive,
                      ]}>
                        {car.name}
                      </Text>
                      {selectedCar?.id === car.id && (
                        <Ionicons name="checkmark" size={18} color={colors.primary} />
                      )}
                    </TouchableOpacity>
                  ))
                )}
              </View>
            )}
          </GlassCard>

          {/* 카테고리 */}
          <GlassCard>
            <CardHeader icon="grid-outline" iconColor={colors.primary} title="카테고리" />
            {Object.entries(CATEGORIES).map(([category, items]) => (
              <View key={category} style={styles.categorySection}>
                <Text style={styles.categoryLabel}>{category}</Text>
                <View style={styles.chips}>
                  {items.map((type) => (
                    <TouchableOpacity
                      key={type}
                      style={[
                        styles.chip,
                        maintenanceType === type && styles.chipActive,
                      ]}
                      onPress={() => handleCategorySelect(type)}
                    >
                      <Text
                        style={[
                          styles.chipText,
                          maintenanceType === type && styles.chipTextActive,
                        ]}
                      >
                        {type}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>
            ))}
          </GlassCard>

          {/* 비용 - 정비, 주유, 기타 모두 표시 */}
          {selectedCategory && (
            <GlassCard>
              <CardHeader icon="cash-outline" iconColor={colors.iconGold} title="비용" />
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="비용을 입력하세요"
                  placeholderTextColor="rgba(0,0,0,0.3)"
                  value={cost}
                  onChangeText={setCost}
                  keyboardType="numeric"
                />
                <Text style={styles.unit}>원</Text>
              </View>
            </GlassCard>
          )}

          {/* 누적 주행거리 - 정비만 표시 */}
          {selectedCategory === '정비' && (
            <GlassCard>
              <CardHeader icon="speedometer-outline" iconColor="#48BB78" title="누적 주행거리" />
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="현재 누적 주행거리"
                  placeholderTextColor="rgba(0,0,0,0.3)"
                  value={mileage}
                  onChangeText={setMileage}
                  keyboardType="numeric"
                />
                <Text style={styles.unit}>km</Text>
              </View>
            </GlassCard>
          )}

          {/* 장소 - 정비, 주유만 표시 */}
          {(selectedCategory === '정비' || selectedCategory === '주유') && (
            <GlassCard>
              <CardHeader icon="location-outline" iconColor="#4A90D9" title="장소" />
              <TextInput
                style={styles.input}
                placeholder="장소를 입력하세요"
                placeholderTextColor="rgba(0,0,0,0.3)"
                value={location}
                onChangeText={setLocation}
              />
            </GlassCard>
          )}

          {/* 주유량 - 주유만 표시 */}
          {selectedCategory === '주유' && (
            <GlassCard>
              <CardHeader icon="water-outline" iconColor="#4A90D9" title="주유량" />
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="주유량을 입력하세요"
                  placeholderTextColor="rgba(0,0,0,0.3)"
                  value={fuelAmount}
                  onChangeText={setFuelAmount}
                  keyboardType="numeric"
                />
                <Text style={styles.unit}>L</Text>
              </View>
            </GlassCard>
          )}

          {/* 메모 - 항상 표시 */}
          <GlassCard>
            <CardHeader icon="document-text-outline" iconColor="#A0AEC0" title="메모" />
            <TextInput
              style={[styles.input, styles.textArea]}
              placeholder="메모를 입력하세요"
              placeholderTextColor="rgba(0,0,0,0.3)"
              value={memo}
              onChangeText={setMemo}
              multiline
              numberOfLines={4}
              textAlignVertical="top"
            />
          </GlassCard>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* 날짜 선택 모달 */}
      <Modal
        visible={datePickerExpanded}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setDatePickerExpanded(false)}
      >
        <View style={styles.dateModalContainer}>
          <View style={styles.dateModalHeader}>
            <TouchableOpacity onPress={() => setDatePickerExpanded(false)}>
              <Text style={styles.dateModalCancel}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.dateModalTitle}>날짜 선택</Text>
            <TouchableOpacity onPress={() => setDatePickerExpanded(false)}>
              <Text style={styles.dateModalSave}>완료</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.datePickerModalContent}>
            <DateTimePicker
              value={date}
              mode="date"
              display="inline"
              onChange={onDateChange}
              minimumDate={minDate}
              maximumDate={maxDate}
              locale="ko-KR"
              style={styles.datePickerModal}
              accentColor={colors.primary}
              themeVariant="light"
            />
          </View>
        </View>
      </Modal>

    </SafeAreaView>
  );
}

// 글래스 카드 컴포넌트
function GlassCard({ children }: { children: React.ReactNode }) {
  return (
    <View style={styles.card}>
      <BlurView intensity={40} tint="light" style={styles.blurView}>
        <View style={styles.glassOverlay} />
      </BlurView>
      <View style={styles.cardContent}>{children}</View>
    </View>
  );
}

// 카드 헤더 컴포넌트
function CardHeader({
  icon,
  iconColor,
  title,
  subtitle,
  required,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  iconColor: string;
  title: string;
  subtitle?: string;
  required?: boolean;
}) {
  return (
    <View style={styles.cardHeader}>
      <View style={[styles.cardIcon, { backgroundColor: `${iconColor}20` }]}>
        <Ionicons name={icon} size={18} color={iconColor} />
      </View>
      <Text style={styles.cardTitle}>
        {title}
        {required && <Text style={styles.required}> *</Text>}
        {subtitle && <Text style={styles.subtitle}> {subtitle}</Text>}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  keyboardView: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[3],
  },
  backButton: {
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
  saveButton: {
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[2],
  },
  saveButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.primary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
    gap: spacing[3],
  },
  card: {
    borderRadius: 20,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
  },
  blurView: {
    ...StyleSheet.absoluteFillObject,
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255, 255, 255, 0.75)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 20,
  },
  cardContent: {
    padding: spacing[4],
    gap: spacing[3],
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
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
  required: {
    color: colors.primary,
  },
  subtitle: {
    fontSize: 14,
    fontWeight: '400',
    color: 'rgba(0,0,0,0.4)',
  },
  input: {
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    fontSize: 16,
    color: 'rgba(0,0,0,0.8)',
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.05)',
  },
  inputFlex: {
    flex: 1,
  },
  textArea: {
    height: 100,
    paddingTop: spacing[3],
  },
  inputWithUnit: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  unit: {
    fontSize: 16,
    color: 'rgba(0,0,0,0.4)',
    fontWeight: '500',
  },
  dateSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing[2],
  },
  dateLabelText: {
    fontSize: 15,
    color: 'rgba(0,0,0,0.5)',
    fontWeight: '500',
  },
  dateValueContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
    borderRadius: 8,
    gap: spacing[1],
  },
  dateValueText: {
    fontSize: 15,
    color: colors.primary,
    fontWeight: '600',
  },
  dateText: {
    fontSize: 16,
    color: 'rgba(0,0,0,0.8)',
    fontWeight: '500',
  },
  selector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.05)',
  },
  selectorText: {
    fontSize: 16,
    color: 'rgba(0,0,0,0.8)',
    fontWeight: '500',
  },
  categorySection: {
    gap: spacing[2],
  },
  categoryLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.4)',
    marginBottom: spacing[1],
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[2],
  },
  chip: {
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[2],
    borderRadius: 20,
    backgroundColor: 'rgba(0,0,0,0.05)',
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.08)',
  },
  chipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.6)',
    fontWeight: '500',
  },
  chipTextActive: {
    color: colors.background,
  },
  // Modal styles
  modalContainer: {
    flex: 1,
    backgroundColor: colors.background,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  modalCancel: {
    fontSize: 16,
    color: colors.textSecondary,
  },
  modalContent: {
    flex: 1,
    padding: layout.screenPadding,
  },
  emptyCarState: {
    alignItems: 'center',
    paddingVertical: spacing[8],
    gap: spacing[2],
  },
  emptyCarText: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.textSecondary,
    marginTop: spacing[2],
  },
  emptyCarSubtext: {
    fontSize: 14,
    color: colors.textTertiary,
  },
  carOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[4],
    backgroundColor: colors.cardBackground,
    borderRadius: 16,
    marginBottom: spacing[3],
    borderWidth: 1,
    borderColor: colors.border,
  },
  carOptionActive: {
    borderColor: colors.primary,
    backgroundColor: `${colors.primary}10`,
  },
  carOptionIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  carOptionInfo: {
    flex: 1,
    marginLeft: spacing[3],
    gap: 2,
  },
  carOptionName: {
    fontSize: 17,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  carOptionDetail: {
    fontSize: 14,
    color: colors.textSecondary,
  },
  // 날짜 선택 모달 전용 스타일 (밝은 배경)
  dateModalContainer: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  dateModalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
    backgroundColor: '#F2F2F7',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  dateModalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  dateModalCancel: {
    fontSize: 16,
    color: '#8E8E93',
  },
  dateModalSave: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.primary,
  },
  datePickerModalContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-start',
    paddingTop: spacing[4],
    backgroundColor: '#F2F2F7',
  },
  datePickerModal: {
    width: '100%',
    height: 400,
    backgroundColor: '#F2F2F7',
  },
  // 차량 드롭다운 스타일
  carDropdownList: {
    marginTop: spacing[3],
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.06)',
    paddingTop: spacing[2],
  },
  carDropdownEmpty: {
    alignItems: 'center',
    paddingVertical: spacing[4],
    gap: spacing[2],
  },
  carDropdownEmptyText: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.4)',
  },
  carDropdownItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[2],
    gap: spacing[3],
    borderRadius: 12,
  },
  carDropdownItemActive: {
    backgroundColor: 'rgba(212, 168, 75, 0.1)',
  },
  carDropdownItemBorder: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.04)',
  },
  carDropdownItemText: {
    flex: 1,
    fontSize: 16,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.7)',
  },
  carDropdownItemTextActive: {
    color: colors.primary,
    fontWeight: '600',
  },
});
