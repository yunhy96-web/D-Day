import { useRef, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ScrollView,
  Animated,
  Dimensions,
  TouchableWithoutFeedback,
  Modal,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSites } from '@/contexts';
import { colors, spacing } from '@/styles';

const SIDEBAR_WIDTH = 280;

interface SidebarProps {
  visible: boolean;
  onClose: () => void;
  activeSiteId?: string;
}

export default function Sidebar({ visible, onClose, activeSiteId }: SidebarProps) {
  const { sites } = useSites();
  const router = useRouter();
  const slideAnim = useRef(new Animated.Value(-SIDEBAR_WIDTH)).current;
  const fadeAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 250,
          useNativeDriver: true,
        }),
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 250,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(slideAnim, {
          toValue: -SIDEBAR_WIDTH,
          duration: 200,
          useNativeDriver: true,
        }),
        Animated.timing(fadeAnim, {
          toValue: 0,
          duration: 200,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [visible, slideAnim, fadeAnim]);

  const navigateTo = (path: string) => {
    onClose();
    setTimeout(() => router.push(path as any), 100);
  };

  if (!visible) return null;

  return (
    <Modal transparent visible={visible} animationType="none" onRequestClose={onClose}>
      <View style={styles.overlay}>
        <TouchableWithoutFeedback onPress={onClose}>
          <Animated.View style={[styles.backdrop, { opacity: fadeAnim }]} />
        </TouchableWithoutFeedback>

        <Animated.View
          style={[styles.sidebar, { transform: [{ translateX: slideAnim }] }]}
        >
          <SafeAreaView style={styles.sidebarContent}>
            <View style={styles.header}>
              <Text style={styles.title}>CheckStock</Text>
              <Text style={styles.subtitle}>사이트 모니터링</Text>
            </View>

            <ScrollView style={styles.siteList}>
              <Text style={styles.sectionLabel}>모니터링 사이트</Text>
              {sites.map((site) => {
                const isActive = site.id === activeSiteId;
                return (
                  <TouchableOpacity
                    key={site.id}
                    style={[styles.siteItem, isActive && styles.siteItemActive]}
                    onPress={() => navigateTo(`/site/${site.id}`)}
                    activeOpacity={0.7}
                  >
                    <Ionicons
                      name="globe-outline"
                      size={20}
                      color={isActive ? colors.primary : colors.textSecondary}
                    />
                    <Text
                      style={[styles.siteName, isActive && styles.siteNameActive]}
                      numberOfLines={1}
                    >
                      {site.name}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>

            <View style={styles.footer}>
              <TouchableOpacity
                style={styles.footerButton}
                onPress={() => navigateTo('/manage')}
                activeOpacity={0.7}
              >
                <Ionicons name="list-outline" size={20} color={colors.textSecondary} />
                <Text style={styles.footerButtonText}>사이트 관리</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.footerButton}
                onPress={() => navigateTo('/settings')}
                activeOpacity={0.7}
              >
                <Ionicons name="options-outline" size={20} color={colors.textSecondary} />
                <Text style={styles.footerButtonText}>필터 설정</Text>
              </TouchableOpacity>
            </View>
          </SafeAreaView>
        </Animated.View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    flexDirection: 'row',
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  sidebar: {
    width: SIDEBAR_WIDTH,
    backgroundColor: colors.background,
    height: '100%',
  },
  sidebarContent: {
    flex: 1,
  },
  header: {
    paddingHorizontal: spacing[5],
    paddingTop: spacing[4],
    paddingBottom: spacing[6],
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    color: colors.primary,
  },
  subtitle: {
    fontSize: 12,
    color: colors.textTertiary,
    marginTop: spacing[1],
  },
  siteList: {
    flex: 1,
    paddingTop: spacing[4],
  },
  sectionLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: colors.textTertiary,
    paddingHorizontal: spacing[5],
    marginBottom: spacing[2],
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  siteItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[5],
    marginHorizontal: spacing[2],
    borderRadius: spacing[2],
  },
  siteItemActive: {
    backgroundColor: colors.primary + '20',
  },
  siteName: {
    fontSize: 15,
    color: colors.textPrimary,
    flex: 1,
  },
  siteNameActive: {
    color: colors.primary,
    fontWeight: '600',
  },
  footer: {
    borderTopWidth: 1,
    borderTopColor: colors.divider,
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[5],
  },
  footerButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    paddingVertical: spacing[3],
  },
  footerButtonText: {
    fontSize: 15,
    color: colors.textSecondary,
  },
});
