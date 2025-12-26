import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, layout } from '@/styles';

const MAINTENANCE_TYPES = [
  '엔진오일 교체',
  '타이어 교체',
  '브레이크 패드',
  '에어필터',
  '배터리',
  '냉각수',
];

export default function AddScreen() {
  const [maintenanceType, setMaintenanceType] = useState('');
  const [cost, setCost] = useState('');
  const [mileage, setMileage] = useState('');
  const [location, setLocation] = useState('');
  const [memo, setMemo] = useState('');
  const [date] = useState(new Date());

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
          <Text style={styles.headerTitle}>정비 기록 추가</Text>
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
            <CardHeader icon="calendar-outline" iconColor={colors.iconBlue} title="정비 날짜" required />
            <TouchableOpacity style={styles.dateSelector}>
              <Text style={styles.dateText}>{formatDate(date)}</Text>
              <Ionicons name="chevron-forward" size={18} color="rgba(0,0,0,0.3)" />
            </TouchableOpacity>
          </GlassCard>

          {/* 차량 선택 */}
          <GlassCard>
            <CardHeader icon="car-sport" iconColor={colors.primary} title="차량 선택" />
            <TouchableOpacity style={styles.selector}>
              <Text style={styles.selectorText}>내 차</Text>
              <Ionicons name="chevron-expand-outline" size={18} color="rgba(0,0,0,0.3)" />
            </TouchableOpacity>
          </GlassCard>

          {/* 정비 항목 */}
          <GlassCard>
            <CardHeader icon="construct" iconColor={colors.primary} title="정비 항목" required />
            <TextInput
              style={styles.input}
              placeholder="예: 엔진오일 교체, 타이어 교체"
              placeholderTextColor="rgba(0,0,0,0.3)"
              value={maintenanceType}
              onChangeText={setMaintenanceType}
            />
            <View style={styles.chips}>
              {MAINTENANCE_TYPES.map((type) => (
                <TouchableOpacity
                  key={type}
                  style={[
                    styles.chip,
                    maintenanceType === type && styles.chipActive,
                  ]}
                  onPress={() => setMaintenanceType(type)}
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
          </GlassCard>

          {/* 비용 */}
          <GlassCard>
            <CardHeader icon="cash-outline" iconColor={colors.iconGold} title="비용" required />
            <View style={styles.inputWithUnit}>
              <TextInput
                style={[styles.input, styles.inputFlex]}
                placeholder="정비 비용을 입력하세요"
                placeholderTextColor="rgba(0,0,0,0.3)"
                value={cost}
                onChangeText={setCost}
                keyboardType="numeric"
              />
              <Text style={styles.unit}>원</Text>
            </View>
          </GlassCard>

          {/* 주행거리 */}
          <GlassCard>
            <CardHeader icon="speedometer-outline" iconColor="#48BB78" title="주행거리" />
            <View style={styles.inputWithUnit}>
              <TextInput
                style={[styles.input, styles.inputFlex]}
                placeholder="정비 당시 주행거리"
                placeholderTextColor="rgba(0,0,0,0.3)"
                value={mileage}
                onChangeText={setMileage}
                keyboardType="numeric"
              />
              <Text style={styles.unit}>km</Text>
            </View>
          </GlassCard>

          {/* 정비소 */}
          <GlassCard>
            <CardHeader icon="location-outline" iconColor="#4A90D9" title="정비소" />
            <TextInput
              style={styles.input}
              placeholder="정비소명을 입력하세요"
              placeholderTextColor="rgba(0,0,0,0.3)"
              value={location}
              onChangeText={setLocation}
            />
          </GlassCard>

          {/* 메모 */}
          <GlassCard>
            <CardHeader icon="document-text-outline" iconColor="#A0AEC0" title="메모" subtitle="(선택사항)" />
            <TextInput
              style={[styles.input, styles.textArea]}
              placeholder="추가 메모사항이 있다면 입력하세요"
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
});
