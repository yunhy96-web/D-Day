import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';

interface CarHeaderProps {
  carName: string;
  mileage: number;
  onCarSelect?: () => void;
  onMaintenancePress?: () => void;
}

export function CarHeader({ carName, mileage, onCarSelect, onMaintenancePress }: CarHeaderProps) {
  return (
    <View style={styles.container}>
      {/* 차량 정보 글래스 카드 */}
      <TouchableOpacity style={styles.carCard} onPress={onCarSelect} activeOpacity={0.8}>
        <BlurView intensity={40} tint="light" style={styles.blurView}>
          <View style={styles.glassOverlay} />
        </BlurView>
        <View style={styles.carCardContent}>
          <View style={styles.carIconWrapper}>
            <Ionicons name="car-sport" size={22} color={colors.primary} />
          </View>
          <View style={styles.carInfo}>
            <View style={styles.carNameRow}>
              <Text style={styles.carName}>{carName}</Text>
              <Ionicons name="chevron-down" size={16} color="rgba(0,0,0,0.4)" />
            </View>
            <Text style={styles.mileage}>{mileage.toLocaleString()} km</Text>
          </View>
        </View>
      </TouchableOpacity>

      {/* 정비 버튼 */}
      <TouchableOpacity style={styles.maintenanceButton} onPress={onMaintenancePress} activeOpacity={0.8}>
        <BlurView intensity={40} tint="light" style={styles.blurView}>
          <View style={styles.buttonGlassOverlay} />
        </BlurView>
        <Ionicons name="construct" size={22} color={colors.primary} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
    gap: spacing[3],
  },
  carCard: {
    flex: 1,
    height: 72,
    borderRadius: 24,
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
    borderRadius: 24,
  },
  carCardContent: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing[4],
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
  carNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
  },
  carName: {
    fontSize: 17,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  mileage: {
    fontSize: 14,
    color: 'rgba(0,0,0,0.45)',
    fontWeight: '500',
  },
  maintenanceButton: {
    width: 56,
    height: 56,
    borderRadius: 28,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    elevation: 5,
  },
  buttonGlassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255, 255, 255, 0.75)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.9)',
    borderRadius: 28,
  },
});
