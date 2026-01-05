import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Linking,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import Constants from 'expo-constants';
import { colors, spacing, layout } from '@/styles';

// 앱 정보 상수
const APP_VERSION = Constants.expoConfig?.version || '1.0.0';
const DEVELOPER_EMAIL = 'yhy0818@gmail.com';
const PRIVACY_POLICY_URL = 'https://canyon-petroleum-c80.notion.site/2d58531fa7f2803e8e09cf1898d6392b';
const COPYRIGHT_YEAR = new Date().getFullYear();
const COPYRIGHT_HOLDER = 'HuiYeong Yun';

export default function SettingsScreen() {
  const router = useRouter();

  // 외부 링크 열기
  const openURL = async (url: string) => {
    try {
      const supported = await Linking.canOpenURL(url);
      if (supported) {
        await Linking.openURL(url);
      } else {
        Alert.alert('오류', '링크를 열 수 없습니다.');
      }
    } catch (error) {
      Alert.alert('오류', '링크를 열 수 없습니다.');
    }
  };

  // 이메일 보내기
  const sendEmail = () => {
    const subject = encodeURIComponent('[CarNote] 문의사항');
    const body = encodeURIComponent(`\n\n---\n앱 버전: ${APP_VERSION}`);
    openURL(`mailto:${DEVELOPER_EMAIL}?subject=${subject}&body=${body}`);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.title}>설정</Text>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* 차량 관리 */}
        <TouchableOpacity
          style={styles.menuCard}
          onPress={() => router.push('/cars')}
          activeOpacity={0.7}
        >
          <BlurView intensity={40} tint="light" style={styles.blurView}>
            <View style={styles.glassOverlay} />
          </BlurView>
          <View style={styles.menuContent}>
            <View style={styles.menuIconWrapper}>
              <Ionicons name="car-sport" size={22} color={colors.primary} />
            </View>
            <Text style={styles.menuText}>차량 관리</Text>
            <Ionicons name="chevron-forward" size={20} color="rgba(0,0,0,0.3)" />
          </View>
        </TouchableOpacity>

        {/* 보험사 관리 */}
        <TouchableOpacity
          style={styles.menuCard}
          onPress={() => router.push('/insurers')}
          activeOpacity={0.7}
        >
          <BlurView intensity={40} tint="light" style={styles.blurView}>
            <View style={styles.glassOverlay} />
          </BlurView>
          <View style={styles.menuContent}>
            <View style={[styles.menuIconWrapper, { backgroundColor: 'rgba(74, 144, 226, 0.15)' }]}>
              <Ionicons name="shield-checkmark" size={22} color={colors.iconBlue} />
            </View>
            <Text style={styles.menuText}>보험사 관리</Text>
            <Ionicons name="chevron-forward" size={20} color="rgba(0,0,0,0.3)" />
          </View>
        </TouchableOpacity>

        {/* 정보 섹션 */}
        <GlassCard>
          <View style={styles.sectionHeader}>
            <View style={styles.sectionIconWrapper}>
              <Ionicons name="information-circle-outline" size={20} color={colors.iconBlue} />
            </View>
            <Text style={styles.sectionTitle}>정보</Text>
          </View>

          {/* 앱 버전 */}
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>앱 버전</Text>
            <Text style={styles.infoValue}>{APP_VERSION}</Text>
          </View>

          <View style={styles.infoDivider} />

          {/* 개발자 문의 */}
          <TouchableOpacity style={styles.infoRow} onPress={sendEmail} activeOpacity={0.7}>
            <Text style={styles.infoLabel}>개발자 문의</Text>
            <View style={styles.infoValueWithIcon}>
              <Text style={styles.infoValue}>{DEVELOPER_EMAIL}</Text>
              <Ionicons name="chevron-forward" size={16} color="rgba(0,0,0,0.3)" />
            </View>
          </TouchableOpacity>

          <View style={styles.infoDivider} />

          {/* 개인정보 처리방침 */}
          <TouchableOpacity
            style={styles.infoRow}
            onPress={() => openURL(PRIVACY_POLICY_URL)}
            activeOpacity={0.7}
          >
            <Text style={styles.infoLabel}>개인정보 처리방침</Text>
            <Ionicons name="chevron-forward" size={16} color="rgba(0,0,0,0.3)" />
          </TouchableOpacity>

          <View style={styles.infoDivider} />

          {/* 저작권 */}
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>저작권</Text>
            <Text style={styles.infoValue}>© {COPYRIGHT_YEAR} {COPYRIGHT_HOLDER}</Text>
          </View>
        </GlassCard>
      </ScrollView>
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
    paddingHorizontal: layout.screenPadding,
    paddingVertical: spacing[4],
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: layout.screenPadding,
    paddingBottom: 120,
    gap: spacing[4],
  },
  // 메뉴 카드 스타일
  menuCard: {
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  menuContent: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing[4],
    gap: spacing[3],
  },
  menuIconWrapper: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(212, 168, 75, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  menuText: {
    flex: 1,
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  // 글래스 카드 스타일
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
  // 섹션 헤더
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    marginBottom: spacing[3],
  },
  sectionIconWrapper: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(74, 144, 226, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: 'rgba(0,0,0,0.8)',
  },
  // 정보 행 스타일
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing[3],
  },
  infoLabel: {
    fontSize: 15,
    fontWeight: '500',
    color: 'rgba(0,0,0,0.7)',
  },
  infoValue: {
    fontSize: 15,
    color: 'rgba(0,0,0,0.5)',
    fontWeight: '500',
  },
  infoValueWithIcon: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1],
  },
  infoDivider: {
    height: 1,
    backgroundColor: 'rgba(0,0,0,0.06)',
  },
});
