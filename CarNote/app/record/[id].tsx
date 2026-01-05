import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Modal,
  TextInput,
  Platform,
  LayoutAnimation,
  UIManager,
} from 'react-native';
import DateTimePicker, { DateTimePickerEvent } from '@react-native-community/datetimepicker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar, useRecord, RecordCategory, useInsurance } from '@/contexts';

// Android에서 LayoutAnimation 활성화
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

const CATEGORIES = {
  정비: ['엔진오일', '타이어', '브레이크', '에어필터', '배터리', '냉각수', '기타'],
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
  const { records, updateRecord, deleteRecord } = useRecord();
  const { insurers } = useInsurance();

  const record = records.find(r => r.id === id);
  const car = record ? cars.find(c => c.id === record.carId) : null;

  // 수정 모달 상태
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editType, setEditType] = useState('');
  const [editCost, setEditCost] = useState('');
  const [editMileage, setEditMileage] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editFuelAmount, setEditFuelAmount] = useState('');
  const [editMemo, setEditMemo] = useState('');
  const [editDate, setEditDate] = useState(new Date());
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [datePickerExpanded, setDatePickerExpanded] = useState(false);

  // 날짜 제한
  const today = new Date();
  const minDate = new Date(today.getFullYear() - 10, today.getMonth(), today.getDate());
  const maxDate = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());

  // 수정 모달 열기
  const openEditModal = () => {
    if (record) {
      setEditType(record.type);
      setEditCost(record.cost?.toString() || '');
      setEditMileage(record.mileage?.toString() || '');
      setEditLocation(record.location || '');
      setEditFuelAmount(record.fuelAmount?.toString() || '');
      setEditMemo(record.memo || '');
      setEditDate(new Date(record.date));
      setEditModalVisible(true);
    }
  };

  // 날짜 변경 핸들러
  const onDateChange = (event: DateTimePickerEvent, selectedDate?: Date) => {
    if (Platform.OS === 'android') {
      setShowDatePicker(false);
      setDatePickerExpanded(false);
    }
    if (selectedDate) {
      setEditDate(selectedDate);
    }
  };

  // 카테고리 선택 핸들러
  const handleCategorySelect = useCallback((type: string) => {
    LayoutAnimation.configureNext(
      LayoutAnimation.create(
        250,
        LayoutAnimation.Types.easeInEaseOut,
        LayoutAnimation.Properties.opacity
      )
    );
    setEditType(type);
  }, []);

  // 수정 저장
  const handleSave = async () => {
    if (!record || !editType) return;

    const selectedCategory = getSelectedCategory(editType);
    if (!selectedCategory) return;

    try {
      await updateRecord(record.id, {
        category: selectedCategory as RecordCategory,
        type: editType,
        date: editDate,
        cost: editCost ? Number(editCost) : undefined,
        mileage: editMileage ? Number(editMileage) : undefined,
        location: editLocation || undefined,
        fuelAmount: editFuelAmount ? Number(editFuelAmount) : undefined,
        memo: editMemo || undefined,
      });
      setEditModalVisible(false);
    } catch (error) {
      Alert.alert('오류', '수정에 실패했습니다.');
    }
  };

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

  // 짧은 날짜 포맷
  const formatShortDate = (d: Date) => {
    return d.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const selectedCategory = getSelectedCategory(editType);

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
        <View style={styles.headerActions}>
          <TouchableOpacity onPress={openEditModal} style={styles.headerButton}>
            <Ionicons name="pencil" size={20} color={colors.primary} />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleDelete} style={styles.headerButton}>
            <Ionicons name="trash-outline" size={22} color={colors.error} />
          </TouchableOpacity>
        </View>
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

      {/* 수정 모달 */}
      <Modal
        visible={editModalVisible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setEditModalVisible(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setEditModalVisible(false)}>
              <Text style={styles.modalCancel}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>기록 수정</Text>
            <TouchableOpacity onPress={handleSave}>
              <Text style={styles.modalSave}>저장</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent} showsVerticalScrollIndicator={false}>
            {/* 날짜 */}
            <View style={styles.editSection}>
              <Text style={styles.editLabel}>날짜</Text>
              <TouchableOpacity
                style={styles.dateSelector}
                onPress={() => Platform.OS === 'android' ? setShowDatePicker(true) : setDatePickerExpanded(true)}
              >
                <Text style={styles.dateSelectorText}>{formatShortDate(editDate)}</Text>
                <Ionicons name="calendar" size={18} color={colors.primary} />
              </TouchableOpacity>
            </View>

            {/* Android 날짜 피커 */}
            {Platform.OS === 'android' && showDatePicker && (
              <DateTimePicker
                value={editDate}
                mode="date"
                display="default"
                onChange={onDateChange}
                minimumDate={minDate}
                maximumDate={maxDate}
              />
            )}

            {/* 카테고리 */}
            <View style={styles.editSection}>
              <Text style={styles.editLabel}>카테고리</Text>
              {Object.entries(CATEGORIES).map(([category, items]) => (
                <View key={category} style={styles.categorySection}>
                  <Text style={styles.categoryLabel}>{category}</Text>
                  <View style={styles.chips}>
                    {items.map((type) => (
                      <TouchableOpacity
                        key={type}
                        style={[
                          styles.chip,
                          editType === type && styles.chipActive,
                        ]}
                        onPress={() => handleCategorySelect(type)}
                      >
                        <Text
                          style={[
                            styles.chipText,
                            editType === type && styles.chipTextActive,
                          ]}
                        >
                          {type}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </View>
              ))}
            </View>

            {/* 비용 */}
            <View style={styles.editSection}>
              <Text style={styles.editLabel}>비용</Text>
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="비용을 입력하세요"
                  placeholderTextColor={colors.textTertiary}
                  value={editCost}
                  onChangeText={setEditCost}
                  keyboardType="numeric"
                />
                <Text style={styles.unit}>원</Text>
              </View>
            </View>

            {/* 누적 주행거리 - 정비만 */}
            {selectedCategory === '정비' && (
              <View style={styles.editSection}>
                <Text style={styles.editLabel}>누적 주행거리</Text>
                <View style={styles.inputWithUnit}>
                  <TextInput
                    style={[styles.input, styles.inputFlex]}
                    placeholder="주행거리를 입력하세요"
                    placeholderTextColor={colors.textTertiary}
                    value={editMileage}
                    onChangeText={setEditMileage}
                    keyboardType="numeric"
                  />
                  <Text style={styles.unit}>km</Text>
                </View>
              </View>
            )}

            {/* 장소 - 정비, 주유만 */}
            {(selectedCategory === '정비' || selectedCategory === '주유') && (
              <View style={styles.editSection}>
                <Text style={styles.editLabel}>장소</Text>
                <TextInput
                  style={styles.input}
                  placeholder="장소를 입력하세요"
                  placeholderTextColor={colors.textTertiary}
                  value={editLocation}
                  onChangeText={setEditLocation}
                />
              </View>
            )}

            {/* 주유량 - 주유만 */}
            {selectedCategory === '주유' && (
              <View style={styles.editSection}>
                <Text style={styles.editLabel}>주유량</Text>
                <View style={styles.inputWithUnit}>
                  <TextInput
                    style={[styles.input, styles.inputFlex]}
                    placeholder="주유량을 입력하세요"
                    placeholderTextColor={colors.textTertiary}
                    value={editFuelAmount}
                    onChangeText={setEditFuelAmount}
                    keyboardType="numeric"
                  />
                  <Text style={styles.unit}>L</Text>
                </View>
              </View>
            )}

            {/* 메모 */}
            <View style={styles.editSection}>
              <Text style={styles.editLabel}>메모</Text>
              <TextInput
                style={[styles.input, styles.textArea]}
                placeholder="메모를 입력하세요"
                placeholderTextColor={colors.textTertiary}
                value={editMemo}
                onChangeText={setEditMemo}
                multiline
                numberOfLines={4}
                textAlignVertical="top"
              />
            </View>
          </ScrollView>
        </View>

        {/* iOS 날짜 선택 모달 */}
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
                value={editDate}
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
      </Modal>
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
  headerActions: {
    flexDirection: 'row',
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
  // 모달 스타일
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
  modalSave: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.primary,
  },
  modalContent: {
    flex: 1,
    padding: layout.screenPadding,
  },
  // 수정 폼 스타일
  editSection: {
    marginBottom: spacing[5],
  },
  editLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: spacing[2],
  },
  input: {
    backgroundColor: colors.cardBackground,
    borderRadius: 12,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    fontSize: 16,
    color: colors.textPrimary,
    borderWidth: 1,
    borderColor: colors.border,
  },
  inputFlex: {
    flex: 1,
  },
  inputWithUnit: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  unit: {
    fontSize: 16,
    color: colors.textSecondary,
    fontWeight: '500',
  },
  textArea: {
    height: 100,
    paddingTop: spacing[3],
  },
  dateSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.cardBackground,
    borderRadius: 12,
    paddingHorizontal: spacing[4],
    paddingVertical: spacing[3],
    borderWidth: 1,
    borderColor: colors.border,
  },
  dateSelectorText: {
    fontSize: 16,
    color: colors.textPrimary,
  },
  categorySection: {
    marginBottom: spacing[3],
  },
  categoryLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textSecondary,
    marginBottom: spacing[2],
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
    backgroundColor: colors.cardBackground,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: 14,
    color: colors.textSecondary,
    fontWeight: '500',
  },
  chipTextActive: {
    color: colors.background,
  },
  // 날짜 선택 모달 스타일
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
});
