import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Modal,
  LayoutAnimation,
  UIManager,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';
import { useCar, Car } from '@/contexts';

// Android에서 LayoutAnimation 활성화
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

export default function SettingsScreen() {
  const router = useRouter();
  const { cars, addCar, updateCar, deleteCar } = useCar();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCar, setEditingCar] = useState<Car | null>(null);

  // 폼 상태
  const [carName, setCarName] = useState('');
  const [plateNumber, setPlateNumber] = useState('');
  const [mileage, setMileage] = useState('');

  const resetForm = () => {
    setCarName('');
    setPlateNumber('');
    setMileage('');
    setEditingCar(null);
  };

  const openAddModal = () => {
    resetForm();
    setModalVisible(true);
  };

  const openEditModal = (car: Car) => {
    setEditingCar(car);
    setCarName(car.name);
    setPlateNumber(car.plateNumber || '');
    setMileage(car.mileage.toString());
    setModalVisible(true);
  };

  const handleSave = () => {
    if (!carName.trim()) return;

    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);

    if (editingCar) {
      // 수정
      updateCar(editingCar.id, {
        name: carName,
        plateNumber: plateNumber || undefined,
        mileage: Number(mileage) || 0,
      });
    } else {
      // 추가
      addCar({
        name: carName,
        plateNumber: plateNumber || undefined,
        mileage: Number(mileage) || 0,
      });
    }

    setModalVisible(false);
    resetForm();
  };

  const handleDelete = (carId: string) => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    deleteCar(carId);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.push('/')} style={styles.backButton}>
          <Ionicons name="chevron-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>설정</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 보유 차량 섹션 */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>보유 차량</Text>
            <TouchableOpacity style={styles.addButton} onPress={openAddModal}>
              <Ionicons name="add" size={20} color={colors.primary} />
              <Text style={styles.addButtonText}>추가</Text>
            </TouchableOpacity>
          </View>

          {cars.length === 0 ? (
            <GlassCard>
              <View style={styles.emptyState}>
                <View style={styles.emptyIconContainer}>
                  <Ionicons name="car-outline" size={32} color="rgba(0,0,0,0.2)" />
                </View>
                <Text style={styles.emptyText}>등록된 차량이 없습니다</Text>
                <Text style={styles.emptySubtext}>차량을 추가해보세요</Text>
              </View>
            </GlassCard>
          ) : (
            cars.map((car) => (
              <GlassCard key={car.id}>
                <TouchableOpacity
                  style={styles.carItem}
                  onPress={() => openEditModal(car)}
                  activeOpacity={0.7}
                >
                  <View style={styles.carIconWrapper}>
                    <Ionicons name="car-sport" size={22} color={colors.primary} />
                  </View>
                  <View style={styles.carInfo}>
                    <Text style={styles.carName}>{car.name}</Text>
                    <View style={styles.carDetails}>
                      {car.plateNumber && (
                        <Text style={styles.carDetail}>{car.plateNumber}</Text>
                      )}
                      <Text style={styles.carDetail}>{car.mileage.toLocaleString()} km</Text>
                    </View>
                  </View>
                  <TouchableOpacity
                    style={styles.deleteButton}
                    onPress={() => handleDelete(car.id)}
                  >
                    <Ionicons name="trash-outline" size={18} color="rgba(0,0,0,0.3)" />
                  </TouchableOpacity>
                </TouchableOpacity>
              </GlassCard>
            ))
          )}
        </View>
      </ScrollView>

      {/* 차량 추가/수정 모달 */}
      <Modal
        visible={modalVisible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setModalVisible(false)}>
              <Text style={styles.modalCancel}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>
              {editingCar ? '차량 수정' : '차량 추가'}
            </Text>
            <TouchableOpacity onPress={handleSave}>
              <Text style={styles.modalSave}>저장</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>차량명</Text>
              <TextInput
                style={styles.input}
                placeholder="예: 아반떼, K5, 테슬라 모델3"
                placeholderTextColor="rgba(255,255,255,0.4)"
                value={carName}
                onChangeText={setCarName}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>차량 번호 (선택)</Text>
              <TextInput
                style={styles.input}
                placeholder="예: 12가 3456"
                placeholderTextColor="rgba(255,255,255,0.4)"
                value={plateNumber}
                onChangeText={setPlateNumber}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>현재 주행거리</Text>
              <View style={styles.inputWithUnit}>
                <TextInput
                  style={[styles.input, styles.inputFlex]}
                  placeholder="0"
                  placeholderTextColor="rgba(255,255,255,0.4)"
                  value={mileage}
                  onChangeText={setMileage}
                  keyboardType="numeric"
                />
                <Text style={styles.unit}>km</Text>
              </View>
            </View>
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
  backButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerSpacer: {
    width: 44,
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
  },
  section: {
    gap: spacing[3],
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing[1],
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  addButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
  },
  addButtonText: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.primary,
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
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: spacing[4],
    gap: spacing[2],
  },
  emptyIconContainer: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(0,0,0,0.05)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing[2],
  },
  emptyText: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.5)',
  },
  emptySubtext: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.3)',
  },
  carItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  carIconWrapper: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  carInfo: {
    flex: 1,
    gap: 2,
  },
  carName: {
    fontSize: 17,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  carDetails: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  carDetail: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.45)',
    fontWeight: '500',
  },
  deleteButton: {
    padding: spacing[2],
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
  modalSave: {
    fontSize: 16,
    fontWeight: '600',
    color: colors.primary,
  },
  modalContent: {
    flex: 1,
    padding: layout.screenPadding,
  },
  inputGroup: {
    marginBottom: spacing[4],
  },
  inputLabel: {
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
  inputWithUnit: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  inputFlex: {
    flex: 1,
  },
  unit: {
    fontSize: 16,
    color: colors.textSecondary,
    fontWeight: '500',
  },
});
