import React, { useState, useCallback } from 'react';
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

// Android에서 LayoutAnimation 활성화
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}
import { SafeAreaView } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar } from '@/contexts';

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
  const { cars, selectedCar, selectCar } = useCar();
  const [maintenanceType, setMaintenanceType] = useState('');
  const [cost, setCost] = useState('');
  const [mileage, setMileage] = useState('');
  const [location, setLocation] = useState('');
  const [fuelAmount, setFuelAmount] = useState('');
  const [memo, setMemo] = useState('');
  const [date] = useState(new Date());
  const [carModalVisible, setCarModalVisible] = useState(false);

  const selectedCategory = getSelectedCategory(maintenanceType);

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
    // TODO: Save to SQLite
    console.log('Save maintenance record');
  };

  const formatDate = (d: Date) => {
    return d.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        {/* Header */}
        <View style={styles.header}>
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
            <TouchableOpacity style={styles.dateSelector}>
              <Text style={styles.dateText}>{formatDate(date)}</Text>
              <Ionicons name="chevron-forward" size={18} color="rgba(0,0,0,0.3)" />
            </TouchableOpacity>
          </GlassCard>

          {/* 차량 선택 */}
          <GlassCard>
            <CardHeader icon="car-sport" iconColor={colors.primary} title="차량 선택" />
            <TouchableOpacity style={styles.selector} onPress={() => setCarModalVisible(true)}>
              <Text style={styles.selectorText}>
                {selectedCar ? selectedCar.name : (cars.length > 0 ? '차량을 선택하세요' : '차량을 추가하세요')}
              </Text>
              <Ionicons name="chevron-expand-outline" size={18} color="rgba(0,0,0,0.3)" />
            </TouchableOpacity>
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

          {/* 주행거리 - 정비만 표시 */}
          {selectedCategory === '정비' && (
            <GlassCard>
              <CardHeader icon="speedometer-outline" iconColor="#48BB78" title="주행거리" />
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="현재 주행거리"
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

      {/* 차량 선택 모달 */}
      <Modal
        visible={carModalVisible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setCarModalVisible(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setCarModalVisible(false)}>
              <Text style={styles.modalCancel}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>차량 선택</Text>
            <View style={{ width: 40 }} />
          </View>

          <ScrollView style={styles.modalContent}>
            {cars.length === 0 ? (
              <View style={styles.emptyCarState}>
                <Ionicons name="car-outline" size={48} color={colors.textTertiary} />
                <Text style={styles.emptyCarText}>등록된 차량이 없습니다</Text>
                <Text style={styles.emptyCarSubtext}>설정에서 차량을 추가해주세요</Text>
              </View>
            ) : (
              cars.map((car) => (
                <TouchableOpacity
                  key={car.id}
                  style={[
                    styles.carOption,
                    selectedCar?.id === car.id && styles.carOptionActive,
                  ]}
                  onPress={() => {
                    selectCar(car.id);
                    setCarModalVisible(false);
                  }}
                >
                  <View style={styles.carOptionIcon}>
                    <Ionicons name="car-sport" size={22} color={colors.primary} />
                  </View>
                  <View style={styles.carOptionInfo}>
                    <Text style={styles.carOptionName}>{car.name}</Text>
                    {car.plateNumber && (
                      <Text style={styles.carOptionDetail}>{car.plateNumber}</Text>
                    )}
                  </View>
                  {selectedCar?.id === car.id && (
                    <Ionicons name="checkmark-circle" size={24} color={colors.primary} />
                  )}
                </TouchableOpacity>
              ))
            )}
          </ScrollView>
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
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '700',
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
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.05)',
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
});
